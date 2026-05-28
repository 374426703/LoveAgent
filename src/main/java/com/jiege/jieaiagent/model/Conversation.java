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
@TableName("conversations")
public class Conversation {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long userId;
    private String title;
    private String appType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
