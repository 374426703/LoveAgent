package com.jiege.jieaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;


@Configuration
public class PgVectorVectorStoreConfig {

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    public VectorStore pgVectorVectorStore(@Qualifier("pgJdbcTemplate") JdbcTemplate jdbcTemplate,EmbeddingModel embeddingModel) {
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)                    // Optional: defaults to model dimensions or 1536
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
                //这里每个向量模型的最大批量转换次数不一样，text-embedding-v4最大为10因此需要批量插入
                .maxDocumentBatchSize(10)// Optional: defaults to 10000
                .build();
//        加载文档(不需要每次都执行)

/*      List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documents);
        int batchSize = 10;
        for (int i = 0; i < enrichedDocuments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, enrichedDocuments.size());
            List<Document> batch = enrichedDocuments.subList(i, end);
            vectorStore.add(batch);
        }
*/
        return vectorStore;
    }
}
