package com.jiege.jieaiagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiege.jieaiagent.exception.BusinessException;
import com.jiege.jieaiagent.mapper.KnowledgeDocumentMapper;
import com.jiege.jieaiagent.model.KnowledgeDocument;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeBaseService {

    @Resource
    private KnowledgeDocumentMapper docMapper;

    @Resource
    @Qualifier("pgVectorVectorStore")
    private VectorStore vectorStore;

    @Resource
    private ResourcePatternResolver resourcePatternResolver;

    @Resource
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        Long count = docMapper.selectCount(new LambdaQueryWrapper<>());
        if (count == 0) {
            log.info("知识库为空，自动从 classpath 加载文档...");
            reloadFromClasspath();
        }
    }

    public KnowledgeDocument uploadMarkdown(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".md")) {
            throw new BusinessException("仅支持 Markdown (.md) 文件");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("读取上传文件失败", e);
        }

        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return originalName;
            }
        };

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", originalName)
                .withAdditionalMetadata("source", "admin_upload")
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        List<Document> documents = reader.get();

        if (documents.isEmpty()) {
            throw new BusinessException("文件内容为空，无法提取文档");
        }

        List<String> allIds = new ArrayList<>();
        int batchSize = 10;
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            vectorStore.add(batch);
            batch.forEach(d -> { if (d.getId() != null) allIds.add(d.getId()); });
        }

        String docIdsJson;
        try {
            docIdsJson = objectMapper.writeValueAsString(allIds);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化文档ID失败", e);
        }

        KnowledgeDocument kd = KnowledgeDocument.builder()
                .filename(originalName)
                .title(originalName.replace(".md", ""))
                .chunkCount(documents.size())
                .docIds(docIdsJson)
                .status("ACTIVE")
                .build();
        docMapper.insert(kd);

        log.info("知识库文档上传成功: {} ({} 个片段)", originalName, documents.size());
        return kd;
    }

    public List<KnowledgeDocument> listDocuments() {
        return docMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getStatus, "ACTIVE")
                        .orderByDesc(KnowledgeDocument::getCreatedAt));
    }

    public void deleteDocument(Long id) {
        KnowledgeDocument kd = docMapper.selectById(id);
        if (kd == null) {
            throw new BusinessException("文档不存在");
        }

        try {
            List<String> docIds = objectMapper.readValue(kd.getDocIds(), new TypeReference<List<String>>() {});
            if (docIds != null && !docIds.isEmpty()) {
                vectorStore.delete(docIds);
                log.info("已从向量库删除 {} 个文档片段", docIds.size());
            }
        } catch (Exception e) {
            log.error("删除向量库文档失败", e);
        }

        kd.setStatus("DELETED");
        docMapper.updateById(kd);
    }

    public Map<String, Object> search(String query, int page, int pageSize) {
        if (query != null && !query.isBlank()) {
            return semanticSearch(query, page, pageSize);
        } else {
            return listAll(page, pageSize);
        }
    }

    private Map<String, Object> semanticSearch(String query, int page, int pageSize) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(page * pageSize + pageSize)
                .similarityThreshold(0.3)
                .build();
        List<Document> results = vectorStore.similaritySearch(request);

        int total = results.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Document> pageResults = from < total ? results.subList(from, to) : List.of();

        return Map.of(
                "records", pageResults.stream().map(this::toMap).collect(Collectors.toList()),
                "total", total,
                "page", page,
                "pageSize", pageSize
        );
    }

    private Map<String, Object> listAll(int page, int pageSize) {
        String countSql = "SELECT COUNT(*) FROM vector_store";
        Integer total = pgJdbcTemplate.queryForObject(countSql, Integer.class);
        if (total == null) total = 0;

        int offset = (page - 1) * pageSize;
        String sql = "SELECT id, content, metadata::text as metadata FROM vector_store ORDER BY id LIMIT ? OFFSET ?";
        List<Map<String, Object>> rows = pgJdbcTemplate.queryForList(sql, pageSize, offset);

        List<Map<String, Object>> records = rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            String content = (String) row.get("content");
            item.put("content", content != null && content.length() > 300
                    ? content.substring(0, 300) + "..." : content);
            item.put("fullContent", content);
            item.put("metadata", parseMetadata((String) row.get("metadata")));
            return item;
        }).collect(Collectors.toList());

        return Map.of(
                "records", records,
                "total", total,
                "page", page,
                "pageSize", pageSize
        );
    }

    private Map<String, Object> toMap(Document doc) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", doc.getId());
        item.put("content", doc.getText() != null && doc.getText().length() > 300
                ? doc.getText().substring(0, 300) + "..." : doc.getText());
        item.put("fullContent", doc.getText());
        item.put("metadata", doc.getMetadata());
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(metadataJson, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    public void reloadFromClasspath() {
        new Thread(() -> {
            try {
                doReloadFromClasspath();
            } catch (Exception e) {
                log.error("重载知识库失败", e);
            }
        }, "kb-reload").start();
    }

    private void doReloadFromClasspath() {
        log.info("开始重建知识库，清空已有数据...");
        docMapper.delete(new LambdaQueryWrapper<>());
        pgJdbcTemplate.execute("TRUNCATE TABLE vector_store");
        log.info("已有数据已清空，重新从 classpath 加载文档");

        org.springframework.core.io.Resource[] resources;
        try {
            resources = resourcePatternResolver.getResources("classpath:document/*.md");
        } catch (IOException e) {
            log.error("读取 classpath 文档失败", e);
            return;
        }

        int totalLoaded = 0;
        for (org.springframework.core.io.Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) continue;

            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .withAdditionalMetadata("filename", filename)
                    .withAdditionalMetadata("source", "classpath")
                    .build();

            MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
            List<Document> documents = reader.get();
            if (documents.isEmpty()) continue;

            log.info("加载文档: {} ({} 个片段)", filename, documents.size());

            List<String> allIds = new ArrayList<>();
            int batchSize = 10;
            for (int i = 0; i < documents.size(); i += batchSize) {
                int end = Math.min(i + batchSize, documents.size());
                List<Document> batch = documents.subList(i, end);
                try {
                    vectorStore.add(batch);
                    batch.forEach(d -> { if (d.getId() != null) allIds.add(d.getId()); });
                    log.info("  批次 {}/{} 写入完成", (i / batchSize) + 1, (documents.size() + batchSize - 1) / batchSize);
                } catch (Exception e) {
                    log.error("写入向量库失败, 文件: {}, 批次: {}", filename, (i / batchSize) + 1, e);
                }
            }

            String docIdsJson;
            try {
                docIdsJson = objectMapper.writeValueAsString(allIds);
            } catch (JsonProcessingException e) {
                log.error("序列化文档ID失败", e);
                continue;
            }

            KnowledgeDocument kd = KnowledgeDocument.builder()
                    .filename(filename)
                    .title(filename.replace(".md", ""))
                    .chunkCount(documents.size())
                    .docIds(docIdsJson)
                    .status("ACTIVE")
                    .build();
            docMapper.insert(kd);
            totalLoaded += documents.size();
        }

        log.info("从 classpath 重新加载知识库完成: {} 个片段", totalLoaded);
    }
}
