package com.jiege.jieaiagent.controller.admin;

import com.jiege.jieaiagent.dto.ApiResponse;
import com.jiege.jieaiagent.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge")
public class AdminKnowledgeController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/upload")
    public ApiResponse upload(@RequestParam("file") MultipartFile file) {
        knowledgeBaseService.uploadMarkdown(file);
        return ApiResponse.success("上传成功", Map.of());
    }

    @GetMapping("/documents")
    public ApiResponse listDocuments() {
        return ApiResponse.success(knowledgeBaseService.listDocuments());
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse deleteDocument(@PathVariable Long id) {
        knowledgeBaseService.deleteDocument(id);
        return ApiResponse.success("删除成功", Map.of());
    }

    @PostMapping("/search")
    public ApiResponse search(@RequestBody Map<String, Object> body) {
        String query = (String) body.getOrDefault("query", "");
        int page = body.containsKey("page") ? ((Number) body.get("page")).intValue() : 1;
        int pageSize = body.containsKey("pageSize") ? ((Number) body.get("pageSize")).intValue() : 10;
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;

        return ApiResponse.success(knowledgeBaseService.search(query, page, pageSize));
    }

    @PostMapping("/reload")
    public ApiResponse reload() {
        knowledgeBaseService.reloadFromClasspath();
        return ApiResponse.success("知识库重新加载完成", Map.of());
    }
}
