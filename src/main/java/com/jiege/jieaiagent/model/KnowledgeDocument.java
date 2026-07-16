package com.jiege.jieaiagent.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_documents")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String filename;
    private String title;
    private Integer chunkCount;
    private String docIds;
    private String status;
    private LocalDateTime createdAt;
}
