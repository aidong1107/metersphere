-- 平台同步数据
CREATE TABLE IF NOT EXISTS `platform_data` (
                                 `id` varchar(50) DEFAULT NULL,
                                 `record_id` varchar(50) DEFAULT NULL,
                                 `platform` varchar(100) DEFAULT NULL COMMENT 'platform key: Jira,...',
                                 `platform_id` varchar(50) DEFAULT NULL,
                                 `platform_data` longtext COMMENT 'platform raw fields data'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_general_ci;
