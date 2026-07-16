CREATE TABLE IF NOT EXISTS users
(
    `id`            BIGINT(19)   NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(64)  NOT NULL,
    `password_hash` VARCHAR(256) NOT NULL COMMENT 'BCrypt encoded password',
    `nickname`      VARCHAR(64)  DEFAULT NULL,
    `created_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
);

CREATE TABLE IF NOT EXISTS conversations
(
    `id`         VARCHAR(36)  NOT NULL,
    `user_id`    BIGINT(19)   NOT NULL,
    `title`      VARCHAR(255) NOT NULL DEFAULT '新对话',
    `app_type`   VARCHAR(32)  NOT NULL DEFAULT 'love_app',
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_conversations_user_app` (`user_id`, `app_type`, `updated_at` DESC)
);

CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY
(
    `id`              BIGINT(19)  NOT NULL AUTO_INCREMENT,
    `conversation_id` VARCHAR(36) NOT NULL,
    `content`         TEXT        NOT NULL,
    `type`            VARCHAR(10) NOT NULL,
    `timestamp`       TIMESTAMP   NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX` (`conversation_id`, `timestamp`),
    CONSTRAINT TYPE_CHECK CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL'))
);
