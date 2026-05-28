package com.jiege.jieaiagent.chatmemory;


import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.MysqlChatMemoryRepositoryDialect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class MysqlMemory {

    @Bean
    public ChatMemoryRepository chatMemoryRepository(@Qualifier("mysqlJdbcTemplate") JdbcTemplate mysqlJdbcTemplate){
        ChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(mysqlJdbcTemplate)
                .dialect(new MysqlChatMemoryRepositoryDialect())
                .build();
        return chatMemoryRepository;
    }
}
