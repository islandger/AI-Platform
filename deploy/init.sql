-- 初始化数据库和表（来自 database_design.md 的建表脚本）
-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_platform 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE ai_platform;

-- 1. 智能体表
CREATE TABLE IF NOT EXISTS `agent` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '智能体名称',
    `description` VARCHAR(500) COMMENT '智能体描述',
    `system_prompt` TEXT COMMENT '系统提示词',
    `user_prompt_template` TEXT COMMENT '用户提示词模板',
    `model_config` JSON COMMENT '模型配置',
    `workflow_id` BIGINT COMMENT '关联的工作流ID',
    `knowledge_base_ids` JSON COMMENT '关联的知识库ID列表',
    `plugin_ids` JSON COMMENT '关联的插件ID列表',
    `status` VARCHAR(20) DEFAULT 'draft' COMMENT '状态: draft/published',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体表';

-- 2. 工作流表
CREATE TABLE IF NOT EXISTS `workflow` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '工作流名称',
    `description` VARCHAR(500) COMMENT '描述',
    `nodes` JSON COMMENT '节点列表',
    `edges` JSON COMMENT '边列表',
    `config` JSON COMMENT '全局配置',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流表';

-- 3. 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(500) COMMENT '描述',
    `vector_db_type` VARCHAR(50) DEFAULT 'milvus' COMMENT '向量库类型: milvus/chroma',
    `chunk_size` INT DEFAULT 512 COMMENT '分块大小',
    `chunk_overlap` INT DEFAULT 50 COMMENT '分块重叠',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 4. 文档表
CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `filename` VARCHAR(255) NOT NULL COMMENT '文件名',
    `content` LONGTEXT COMMENT '文件原始内容',
    `chunks` JSON COMMENT '分块后的文本列表',
    `vector_ids` JSON COMMENT '向量ID列表',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/processing/success/failed',
    `uploaded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_kb_id` (`knowledge_base_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 5. 插件表
CREATE TABLE IF NOT EXISTS `plugin` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '插件名称',
    `description` VARCHAR(500) COMMENT '描述',
    `type` VARCHAR(20) DEFAULT 'custom' COMMENT '类型: builtin/custom',
    `openapi_spec` JSON COMMENT 'OpenAPI规范',
    `config` JSON COMMENT '配置信息',
    `status` VARCHAR(20) DEFAULT 'disabled' COMMENT '状态: enabled/disabled',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件表';

-- 可选：插入一个示例智能体记录（便于验证）
INSERT INTO `agent` (`name`, `description`, `system_prompt`, `status`) 
VALUES ('示例助手', '用于验证初始化的示例智能体', '你是一个友好的示例助手。', 'draft')
ON DUPLICATE KEY UPDATE name=name;

-- 用户表（简单实现：用于登录/注册）
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `display_name` VARCHAR(100),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 插入示例用户 (username: demo, password: demo)
INSERT INTO `user` (`username`, `password_hash`, `display_name`) 
VALUES ('demo', '$2a$10$sv2oBUXpMyqDkyaZfwf.2.w5Yi4M4pjlVuoKdq6xQFGPmCahgrYiW', '示例用户')
ON DUPLICATE KEY UPDATE username=username;

-- End of init script
