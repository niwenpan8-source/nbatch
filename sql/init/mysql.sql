-- nbatch MySQL initialization script
--
-- 用于新环境初始化：保留表结构，移除业务配置、运行快照、日志、注册记录和历史锁。
-- 脚本会删除同名表，请勿在已有环境直接执行；升级环境请使用 sql/update 中的脚本。
-- 初始化管理员账号：admin，初始密码：CHANGE_ME_BEFORE_USE。首次登录后必须立即修改密码。
--
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `nbatch_group`
--

DROP TABLE IF EXISTS `nbatch_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_group` (
                                    `id` varchar(32) NOT NULL,
                                    `app_name` varchar(64) NOT NULL COMMENT '执行器AppName',
                                    `title` varchar(12) NOT NULL COMMENT '执行器名称',
                                    `address_type` tinyint NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
                                    `address_list` text COMMENT '执行器地址列表，多地址逗号分隔',
                                    `update_time` datetime DEFAULT NULL,
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_lock`
--

DROP TABLE IF EXISTS `nbatch_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_lock` (
                                   `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
                                   PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_registry`
--

DROP TABLE IF EXISTS `nbatch_registry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_registry` (
                                       `id` varchar(32) NOT NULL,
                                       `registry_group` varchar(50) NOT NULL,
                                       `registry_key` varchar(255) NOT NULL,
                                       `registry_value` varchar(255) NOT NULL,
                                       `update_time` datetime DEFAULT NULL,
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `i_g_k_v` (`registry_group`,`registry_key`,`registry_value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_user`
--

DROP TABLE IF EXISTS `nbatch_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_user` (
                                   `id` varchar(32) NOT NULL,
                                   `username` varchar(50) NOT NULL COMMENT '账号',
                                   `password` varchar(100) NOT NULL COMMENT '密码加密信息',
                                   `token` varchar(100) DEFAULT NULL COMMENT '登录token',
                                   `role` tinyint NOT NULL COMMENT '角色：0-普通用户、1-管理员',
                                   `permission` varchar(255) DEFAULT NULL COMMENT '权限：执行器ID列表，多个逗号分割',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `i_username` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_work`
--

DROP TABLE IF EXISTS `nbatch_work`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_work` (
                                   `work_id` varchar(32) NOT NULL COMMENT '作业id',
                                   `work_name` varchar(300) NOT NULL COMMENT '作业名',
                                   `work_desc` varchar(300) DEFAULT NULL COMMENT '作业描述',
                                   `work_type` tinyint NOT NULL DEFAULT '0' COMMENT '作业类型 => 0:翻牌类型，1：顺序类型',
                                   `job_group` varchar(32) DEFAULT NULL COMMENT '执行器分组ID',
                                   `schedule_type` varchar(50) NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
                                   `schedule_conf` varchar(128) DEFAULT NULL COMMENT '调度配置',
                                   `schedule_status` tinyint NOT NULL DEFAULT '0' COMMENT '定时状态：0=停止，1=运行',
                                   `misfire_strategy` varchar(50) NOT NULL DEFAULT 'DO_NOTHING' COMMENT '过期策略',
                                   `trigger_last_time` bigint DEFAULT NULL COMMENT '上次触发时间',
                                   `trigger_next_time` bigint DEFAULT NULL COMMENT '下次触发时间',
                                   `timeout` int NOT NULL DEFAULT '0' COMMENT '执行超时秒数',
                                   `notify_email` varchar(255) DEFAULT NULL COMMENT '通知邮箱',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `version` int DEFAULT '0' COMMENT '流程版本号',
                                   `run_work_status` tinyint DEFAULT NULL COMMENT '当前作业运行状态',
                                   `turn_date` date DEFAULT NULL COMMENT '当前业务日期',
                                   `run_create_time` datetime DEFAULT NULL COMMENT '当前作业运行创建时间',
                                   PRIMARY KEY (`work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业表';
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_work_export_file`
--

DROP TABLE IF EXISTS `nbatch_work_export_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_work_export_file` (
                                               `export_file_id` varchar(32) NOT NULL COMMENT '导出文件id',
                                               `node_id` varchar(32) NOT NULL COMMENT '作业节点id',
                                               `file_name` varchar(200) NOT NULL COMMENT '导出的文件名',
                                               `export_table_name` varchar(200) NOT NULL COMMENT '导出的表名',
                                               `export_table_filed` text NOT NULL COMMENT '导出的列',
                                               `export_table_condition` varchar(200) DEFAULT NULL COMMENT '删除条件',
                                               `file_code` varchar(8) DEFAULT NULL COMMENT '文件编码',
                                               `sep` varchar(32) DEFAULT NULL COMMENT '分隔符',
                                               `all_update` int DEFAULT '0' COMMENT '是否全量文件：1全量 0增量',
                                               `is_gzip` int DEFAULT '0' COMMENT '是否压缩：1压缩 0不压缩',
                                               `file_name_param` varchar(500) DEFAULT NULL COMMENT '生成文件名时，特殊参数参数',
                                               PRIMARY KEY (`export_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业节点导出文件表';
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_work_flow_dependency`
--

DROP TABLE IF EXISTS `nbatch_work_flow_dependency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_work_flow_dependency` (
                                                   `dependency_id` varchar(32) NOT NULL COMMENT '依赖ID',
                                                   `work_id` varchar(32) NOT NULL COMMENT '所属作业ID',
                                                   `predecessor_type` varchar(16) NOT NULL COMMENT '前置端点类型：NODE、GROUP',
                                                   `predecessor_id` varchar(32) NOT NULL COMMENT '前置端点ID',
                                                   `successor_type` varchar(16) NOT NULL COMMENT '后置端点类型：NODE、GROUP',
                                                   `successor_id` varchar(32) NOT NULL COMMENT '后置端点ID',
                                                   `create_time` datetime NOT NULL COMMENT '创建时间',
                                                   PRIMARY KEY (`dependency_id`),
                                                   UNIQUE KEY `uk_flow_dependency` (`work_id`,`predecessor_type`,`predecessor_id`,`successor_type`,`successor_id`),
                                                   KEY `idx_successor` (`work_id`,`successor_type`,`successor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业流程统一依赖';
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_work_flow_group`
--

DROP TABLE IF EXISTS `nbatch_work_flow_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_work_flow_group` (
                                              `group_id` varchar(32) NOT NULL COMMENT '流程组ID',
                                              `work_id` varchar(32) NOT NULL COMMENT '所属作业ID',
                                              `group_name` varchar(128) NOT NULL COMMENT '流程组名称',
                                              `group_desc` varchar(500) DEFAULT NULL COMMENT '流程组说明',
                                              `group_type` varchar(16) NOT NULL COMMENT 'ROOT=默认组，NORMAL=普通组',
                                              `sort_order` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
                                              `version` int NOT NULL DEFAULT '1' COMMENT '乐观锁版本',
                                              `create_time` datetime NOT NULL COMMENT '创建时间',
                                              `update_time` datetime NOT NULL COMMENT '更新时间',
                                              PRIMARY KEY (`group_id`),
                                              KEY `idx_work_sort` (`work_id`,`sort_order`),
                                              KEY `idx_work_group_type` (`work_id`,`group_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业流程组';
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_work_import_file`
--

DROP TABLE IF EXISTS `nbatch_work_import_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_work_import_file` (
                                               `import_file_id` varchar(32) NOT NULL COMMENT '导入文件id',
                                               `node_id` varchar(32) NOT NULL COMMENT '作业节点id',
                                               `file_name` varchar(200) NOT NULL COMMENT '导入的文件名',
                                               `import_table_name` varchar(200) NOT NULL COMMENT '导入的表名',
                                               `import_table_filed` text NOT NULL COMMENT '导入的列',
                                               `import_table_condition` text COMMENT '导入条件',
                                               `file_code` varchar(8) DEFAULT NULL COMMENT '文件编码',
                                               `sep` varchar(32) DEFAULT NULL COMMENT '分隔符',
                                               `all_update` int DEFAULT '0' COMMENT '是否全量文件：1全量 0增量',
                                               `is_gzip` int DEFAULT '0' COMMENT '是否压缩：1压缩 0不压缩',
                                               `file_name_param` varchar(500) DEFAULT NULL COMMENT '生成文件名时，特殊参数参数',
                                               PRIMARY KEY (`import_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业节点导入文件表';
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_work_node`
--

DROP TABLE IF EXISTS `nbatch_work_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_work_node` (
                                        `node_id` varchar(32) NOT NULL COMMENT '作业节点id',
                                        `work_id` varchar(32) NOT NULL COMMENT '作业id',
                                        `group_id` varchar(32) NOT NULL COMMENT '所属流程组',
                                        `node_name` varchar(300) NOT NULL COMMENT '节点名称',
                                        `node_desc` varchar(300) DEFAULT NULL COMMENT '节点描述',
                                        `node_type` varchar(20) NOT NULL COMMENT 'script:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件',
                                        `db_type` varchar(32) DEFAULT NULL COMMENT '翻牌日期',
                                        `execute_content` text COMMENT '执行内容',
                                        `execute_content_param` text COMMENT '执行内容参数',
                                        `execute_handler` text COMMENT 'bean执行器',
                                        `script_type` varchar(32) DEFAULT NULL COMMENT '脚本类型',
                                        `update_time` datetime NOT NULL COMMENT '修改时间',
                                        `error_strategy` varchar(32) DEFAULT 'retry' COMMENT '失败策略：stop-该运行节点停止, skip-跳过继续, retry-重试',
                                        `retry_times` int DEFAULT '5' COMMENT '重试次数',
                                        `node_run_status` tinyint NOT NULL DEFAULT '8' COMMENT '当前正式节点运行状态：8=未初始化',
                                        `turn_date` date DEFAULT NULL COMMENT '当前业务日期',
                                        `current_retry_times` int DEFAULT NULL COMMENT '当前剩余重试次数',
                                        PRIMARY KEY (`node_id`),
                                        KEY `idx_work_group` (`work_id`,`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业节点表';
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_work_run_node_log`
--

DROP TABLE IF EXISTS `nbatch_work_run_node_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_work_run_node_log` (
                                                `node_log_id` varchar(32) NOT NULL COMMENT '节点日志id',
                                                `work_id` varchar(32) NOT NULL COMMENT '作业id',
                                                `node_id` varchar(32) NOT NULL COMMENT '作业节点id',
                                                `turn_date` date DEFAULT NULL COMMENT '翻牌日期',
                                                `handle_code` int NOT NULL COMMENT '执行状态',
                                                `handle_msg` text COMMENT '执行信息',
                                                `executor_address` varchar(32) DEFAULT NULL COMMENT '执行器地址',
                                                `create_time` datetime DEFAULT NULL COMMENT '执行-时间',
                                                `execute_start_time` datetime DEFAULT NULL COMMENT '实际开始执行时间',
                                                `execute_end_time` datetime DEFAULT NULL COMMENT '实际结束执行时间',
                                                `last_active_time` datetime DEFAULT NULL COMMENT '最后活跃时间',
                                                `call_back_time` datetime DEFAULT NULL COMMENT '执行-时间',
                                                PRIMARY KEY (`node_log_id`),
                                                KEY `idx_work_node_turn_callback_create` (`work_id`,`node_id`,`turn_date`,`call_back_time`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业运行节点日志表';
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `nbatch_work_run_node_log_detail`
--

DROP TABLE IF EXISTS `nbatch_work_run_node_log_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_work_run_node_log_detail` (
                                                       `detail_log_id` varchar(32) NOT NULL COMMENT '节点日志id',
                                                       `node_log_id` varchar(32) NOT NULL COMMENT '单次执行日志ID',
                                                       `work_id` varchar(32) NOT NULL COMMENT '作业id',
                                                       `node_id` varchar(32) NOT NULL COMMENT '作业节点id',
                                                       `turn_date` date DEFAULT NULL COMMENT '翻牌日期',
                                                       `handle_msg` text COMMENT '执行信息',
                                                       `create_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
                                                       `execute_time` datetime DEFAULT NULL COMMENT '执行-时间',
                                                       `call_back_time` datetime DEFAULT NULL COMMENT '执行-时间',
                                                       PRIMARY KEY (`detail_log_id`),
                                                       KEY `idx_node_log_id` (`node_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业运行节点日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Minimal bootstrap data. Do not add production credentials or runtime records here.
--
INSERT INTO `nbatch_lock` (`lock_name`) VALUES
                                                ('schedule_lock'),
                                                ('run_work_node_lock')
    ON DUPLICATE KEY UPDATE `lock_name` = VALUES(`lock_name`);

INSERT INTO `nbatch_user` (`id`, `username`, `password`, `token`, `role`, `permission`) VALUES
    ('1', 'admin', 'e10adc3949ba59abbe56e057f20f883e', NULL, 1, NULL)
    ON DUPLICATE KEY UPDATE
                         `username` = VALUES(`username`),
                         `password` = VALUES(`password`),
                         `token` = NULL,
                         `role` = VALUES(`role`),
                         `permission` = NULL;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- End of nbatch MySQL initialization script.
