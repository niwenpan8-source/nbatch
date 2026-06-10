-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: batch_dev
-- ------------------------------------------------------
-- Server version	8.0.42

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
-- Table structure for table `nbatch_job_group`
--

DROP TABLE IF EXISTS `nbatch_job_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_group` (
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
-- Dumping data for table `nbatch_job_group`
--

LOCK TABLES `nbatch_job_group` WRITE;
/*!40000 ALTER TABLE `nbatch_job_group` DISABLE KEYS */;
INSERT INTO `nbatch_job_group` VALUES ('1','nbatch-consumer','nbatch消费者',0,'http://192.168.10.225:9999/','2026-06-04 18:43:42');
/*!40000 ALTER TABLE `nbatch_job_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_info`
--

DROP TABLE IF EXISTS `nbatch_job_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_info` (
  `id` varchar(32) NOT NULL,
  `job_group` varchar(32) NOT NULL COMMENT '执行器主键ID',
  `job_desc` varchar(255) NOT NULL,
  `add_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `author` varchar(64) DEFAULT NULL COMMENT '作者',
  `alarm_email` varchar(255) DEFAULT NULL COMMENT '报警邮件',
  `schedule_type` varchar(50) NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
  `schedule_conf` varchar(128) DEFAULT NULL COMMENT '调度配置，值含义取决于调度类型',
  `misfire_strategy` varchar(50) NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
  `executor_route_strategy` varchar(50) DEFAULT NULL COMMENT '执行器路由策略',
  `executor_handler` varchar(255) DEFAULT NULL COMMENT '执行器任务handler',
  `executor_param` varchar(512) DEFAULT NULL COMMENT '执行器任务参数',
  `executor_block_strategy` varchar(50) DEFAULT NULL COMMENT '阻塞处理策略',
  `executor_timeout` int NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
  `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `glue_type` varchar(50) NOT NULL COMMENT 'GLUE类型，BEAN类型（任务执行类型），WORK类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) DEFAULT NULL COMMENT 'GLUE备注',
  `glue_updatetime` datetime DEFAULT NULL COMMENT 'GLUE更新时间',
  `child_jobid` varchar(255) DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
  `trigger_status` tinyint NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
  `trigger_last_time` bigint NOT NULL DEFAULT '0' COMMENT '上次调度时间',
  `trigger_next_time` bigint NOT NULL DEFAULT '0' COMMENT '下次调度时间',
  `work_id` varchar(50) NOT NULL DEFAULT '0' COMMENT '作业id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_info`
--

LOCK TABLES `nbatch_job_info` WRITE;
/*!40000 ALTER TABLE `nbatch_job_info` DISABLE KEYS */;
INSERT INTO `nbatch_job_info` VALUES ('2001499399308009472','1','demo任务1','2025-12-18 11:46:58','2025-12-18 11:46:58','admin','','CRON','* 0/5 * * * ?','DO_NOTHING','FIRST','','','SERIAL_EXECUTION',0,0,'WORK','','GLUE代码初始化','2025-12-18 11:46:58',NULL,0,0,0,'1991745845269688321'),('2001905927944933376','1','测试lua脚本任务','2025-12-19 14:42:21','2025-12-26 16:47:17','admin','','CRON','0/1 0/1 * * * ?','FIRE_ONCE_NOW','FIRST','','','SERIAL_EXECUTION',0,0,'WORK','','GLUE代码初始化','2025-12-19 14:42:21',NULL,0,0,0,'2001905454051495938'),('2011632156235325440','1','gauss -> clickhouse','2026-01-15 10:50:55','2026-01-15 11:06:22','admin','','CRON','0 0/5 * * * ?','DO_NOTHING','FIRST','','','SERIAL_EXECUTION',0,0,'WORK','','GLUE代码初始化','2026-01-15 10:50:55',NULL,0,0,0,'2011630712279064578');
/*!40000 ALTER TABLE `nbatch_job_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_lock`
--

DROP TABLE IF EXISTS `nbatch_job_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_lock` (
  `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
  PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_lock`
--

LOCK TABLES `nbatch_job_lock` WRITE;
/*!40000 ALTER TABLE `nbatch_job_lock` DISABLE KEYS */;
INSERT INTO `nbatch_job_lock` VALUES ('run_work_node_lock'),('schedule_lock');
/*!40000 ALTER TABLE `nbatch_job_lock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_log`
--

DROP TABLE IF EXISTS `nbatch_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_log` (
  `id` varchar(32) NOT NULL,
  `job_group` varchar(32) NOT NULL COMMENT '执行器主键ID',
  `job_id` varchar(32) NOT NULL COMMENT '任务，主键ID',
  `executor_address` varchar(255) DEFAULT NULL COMMENT '执行器地址，本次执行的地址',
  `executor_handler` varchar(255) DEFAULT NULL COMMENT '执行器任务handler',
  `executor_param` varchar(512) DEFAULT NULL COMMENT '执行器任务参数',
  `executor_sharding_param` varchar(20) DEFAULT NULL COMMENT '执行器任务分片参数，格式如 1/2',
  `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `trigger_time` datetime DEFAULT NULL COMMENT '调度-时间',
  `trigger_code` int NOT NULL COMMENT '调度-结果',
  `trigger_msg` text COMMENT '调度-日志',
  `handle_time` datetime DEFAULT NULL COMMENT '执行-时间',
  `handle_code` int NOT NULL COMMENT '执行-状态',
  `handle_msg` text COMMENT '执行-日志',
  `alarm_status` tinyint NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
  PRIMARY KEY (`id`),
  KEY `I_trigger_time` (`trigger_time`),
  KEY `I_handle_code` (`handle_code`),
  KEY `I_jobid_jobgroup` (`job_id`,`job_group`),
  KEY `I_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `nbatch_job_logglue`
--

DROP TABLE IF EXISTS `nbatch_job_logglue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_logglue` (
  `id` varchar(32) NOT NULL,
  `job_id` varchar(32) NOT NULL COMMENT '任务，主键ID',
  `glue_type` varchar(50) DEFAULT NULL COMMENT 'GLUE类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) NOT NULL COMMENT 'GLUE备注',
  `add_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_logglue`
--

LOCK TABLES `nbatch_job_logglue` WRITE;
/*!40000 ALTER TABLE `nbatch_job_logglue` DISABLE KEYS */;
/*!40000 ALTER TABLE `nbatch_job_logglue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_registry`
--

DROP TABLE IF EXISTS `nbatch_job_registry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_registry` (
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
-- Dumping data for table `nbatch_job_registry`
--

LOCK TABLES `nbatch_job_registry` WRITE;
/*!40000 ALTER TABLE `nbatch_job_registry` DISABLE KEYS */;
INSERT INTO `nbatch_job_registry` VALUES ('2062469355079835649','EXECUTOR','nbatch-consumer','http://192.168.10.225:9999/','2026-06-04 18:43:19');
/*!40000 ALTER TABLE `nbatch_job_registry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_user`
--

DROP TABLE IF EXISTS `nbatch_job_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_user` (
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
-- Dumping data for table `nbatch_job_user`
--

LOCK TABLES `nbatch_job_user` WRITE;
/*!40000 ALTER TABLE `nbatch_job_user` DISABLE KEYS */;
INSERT INTO `nbatch_job_user` VALUES ('1','admin','e10adc3949ba59abbe56e057f20f883e',NULL,1,NULL),('2004458866106494977','test1','5910f84862a6f94eb90dd1b57264770f',NULL,0,'1');
/*!40000 ALTER TABLE `nbatch_job_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_work`
--

DROP TABLE IF EXISTS `nbatch_job_work`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_work` (
  `work_id` varchar(32) NOT NULL COMMENT '作业id',
  `work_name` varchar(300) NOT NULL COMMENT '作业名',
  `work_desc` varchar(300) DEFAULT NULL COMMENT '作业描述',
  `work_type` tinyint NOT NULL DEFAULT '0' COMMENT '作业类型 => 0:翻牌类型，1：顺序类型',
  `work_status` tinyint NOT NULL DEFAULT '0' COMMENT '作业状态：0=停用、1=启用',
  `version` int DEFAULT '0' COMMENT '流程版本号',
  PRIMARY KEY (`work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_work`
--

LOCK TABLES `nbatch_job_work` WRITE;
/*!40000 ALTER TABLE `nbatch_job_work` DISABLE KEYS */;
INSERT INTO `nbatch_job_work` VALUES ('1991745845269688321','测试作业','用于测试',0,2,0),('2001905454051495938','测试-lua脚本作业','测试-lua脚本作业',0,1,0),('2011630712279064578','将gauss数据库中的数据迁移到clickhouse数据库','用于测试',0,1,0),('2042786427320209409','测试执行存储过程','用于测试',0,0,0);
/*!40000 ALTER TABLE `nbatch_job_work` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_work_export_file`
--

DROP TABLE IF EXISTS `nbatch_job_work_export_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_work_export_file` (
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
-- Dumping data for table `nbatch_job_work_export_file`
--

LOCK TABLES `nbatch_job_work_export_file` WRITE;
/*!40000 ALTER TABLE `nbatch_job_work_export_file` DISABLE KEYS */;
INSERT INTO `nbatch_job_work_export_file` VALUES ('1','1','gauss_export_dmap_assess_plan_#{special}.gz.#{date}0','dmap_assess_plan','plan_id, plan_name, plan_cycle_start_time, plan_cycle_end_time, score_type, plan_status, plan_desc, create_time, create_user, create_user_dept, create_user_role_id, is_delete, update_time, review_user, review_time','','utf8','|',NULL,1,'{ \n    \"special\": \"123\"\n}'),('2','5','gauss_export_dmap_assess_plan_assessor_#{special}.gz.#{date}0','dmap_assess_plan_assessor','plan_assessor_id, plan_id, uid, create_time, is_delete, update_time','','utf8','|',NULL,1,'{ \n    \"special\": \"456\"\n}'),('3','3','gbase_export_dmap_assess_plan_#{special}.gz.#{date}0','dmap_assess_plan','plan_id, plan_name, plan_cycle_start_time, plan_cycle_end_time, score_type, plan_status, plan_desc, create_time, create_user, create_user_dept, create_user_role_id, is_delete, update_time, review_user, review_time','','utf8','|',NULL,1,'{ \n    \"special\": \"123\"\n}'),('4','2001537294454599681','gauss_export_bt_fundinfo.gz.#{date}0','bt_fundinfo','fundcode, updatetime, fundname, pyjc, fundname_long, backfundcode, qprq, clrq, clfe, clgm, clzspe, clzsprice, tzmb, tzfw, tzfg, tzlx, bigfundtype, is_cde, is_etf, yjjz, tzcl, stockname, jjgsid, orgname, orgname_short, tgbankid, tgbank, fundmgr, fundmgr_json, jjfe, jjfe_rq, cyrzs, jjsz, jjsz_rq, fundzt_gn, newrq, newjz, newljjz, newfqjz, oldrq, oldjz, oldljjz, oldfqjz, dayzd, dayzf, dayzlxorder, dayzlxcount, clzf, clnhzf, cltlxzf, cltlxnhzf, clstockzf, clstocknhzf, pmrq, weekzf, weektlxzf, weekstockzf, weekzlxorder, weekzlxcount, monthzf, monthtlxzf, monthstockzf, monthnhzf, monthzlxorder, monthzlxcount, quarterzf, quartertlxzf, quarterstockzf, quarternhzf, quarterzlxorder, quarterzlxcount, halfyearzf, halfyeartlxzf, halfyearstockzf, halfyearnhzf, halfyearzlxorder, halfyearzlxcount, yearzf, yeartlxzf, yearstockzf, yearnhzf, yearzlxorder, yearzlxcount, thisyearzf, thisyeartlxzf, thisyearstockzf, thisyearzlxorder, thisyearzlxcount, twoyearzf, twoyeartlxzf, twoyearstockzf, twoyearnhzf, twoyearzlxorder, twoyearzlxcount, threeyearzf, threeyeartlxzf, threeyearstockzf, threeyearnhzf, threeyearzlxorder, threeyearzlxcount, fiveyearzf, fiveyeartlxzf, fiveyearstockzf, fiveyearnhzf, fiveyearzlxorder, fiveyearzlxcount, clyearzf, clyearnhzf, cltwoyearzf, cltwoyearnhzf, clthreeyearzf, clthreeyearnhzf, clfiveyearzf, clfiveyearnhzf, qnzf, qntlxzf, qnstockzf, qnnhzf, qnylzf, qnyltlxzf, qnylstockzf, qnylnhzf, hishigh2015zf, hishigh2015nhzf, hishigh2015zlxorder, hishigh2015zlxcount, hishigh2007zf, hishigh2007nhzf, hishigh2007zlxorder, hishigh2007zlxcount, week4q, month4q, quarter4q, halfyear4q, year4q, thisyear4q, twoyear4q, threeyear4q, fiveyear4q, yearhc, twoyearhc, clfundhc, cltlxhc, clstockhc, jjfx_num, jjfx_zhpj, jjfx_star, jjfx_pj, jjfx_dp_jjzd, jjfx_dp_jjzd_json, jjyj_num, jjjlfx_num, jjgsfx_num, tzfg_dp, tzfg_zp, tzfg_cy, tzfg_py, tzfg_py_s, zzkd_num, zzkd_zhpj, zzkd_zhpj_all, zzkd_zhpj_s, bdfx_num, bdfx_zhpj, buystatus, buynotice, sellstatus, sellnotice, fx_sdate, fx_edate, tgfl, glfl, fl_json, fhcs, ljfh, halfyearprofit_gl, yearprofit_gl, twoyearprofit_gl, threeyearprofit_gl, fiveyearprofit_gl, profitgl_py, halfyearprofit_level, halfyearprofit_nhlevel, yearprofit_level, yearprofit_nhlevel, twoyearprofit_level, twoyearprofit_nhlevel, threeyearprofit_level, threeyearprofit_nhlevel, fiveyearprofit_level, fiveyearprofit_nhlevel, profitlevel_py, profit_date, yjfx_py, jzd_py, wdd_py, sharp_py, fhcf_py, jjgm_py, cyr_py, jbrq, holdstock_json, holdhy_json, holdhy_py, holdzq_json, yeardtsy_m, twoyeardtsy_m, threeyeardtsy_m, fiveyeardtsy_m, yeardtsy_w, twoyeardtsy_w, threeyeardtsy_w, fiveyeardtsy_w, yeardtsy_dw, twoyeardtsy_dw, threeyeardtsy_dw, fiveyeardtsy_dw, yeardtpm_m, twoyeardtpm_m, threeyeardtpm_m, fiveyeardtpm_m, yeardtpm_w, twoyeardtpm_w, threeyeardtpm_w, fiveyeardtpm_w, yeardtpm_dw, twoyeardtpm_dw, threeyeardtpm_dw, fiveyeardtpm_dw, risklevel, is_gs, count_rank5, rank_cx3y, rank_cx3y_trend, rank_zszq3y, rank_zszq3y_trend, rank_shzq3y, rank_shzq3y_trend, rank_jajx3y, rank_jajx3y_trend, bdfx_bdp_bn, bdfx_bdp_yn, bdfx_bdp_ln, bdfx_syp_bn, bdfx_syp_yn, bdfx_syp_ln, bdfx_fxp_bn, bdfx_fxp_yn, bdfx_fxp_ln, hsl_py','','utf8','|',0,1,''),('5','2011631244959866882','gauss_export_bt_fundinfo.gz.#{date}0','bt_fundinfo','fundcode, updatetime, fundname, pyjc, fundname_long, backfundcode, qprq, clrq, clfe, clgm, clzspe, clzsprice, tzmb, tzfw, tzfg, tzlx, bigfundtype, is_cde, is_etf, yjjz, tzcl, stockname, jjgsid, orgname, orgname_short, tgbankid, tgbank, fundmgr, fundmgr_json, jjfe, jjfe_rq, cyrzs, jjsz, jjsz_rq, fundzt_gn, newrq, newjz, newljjz, newfqjz, oldrq, oldjz, oldljjz, oldfqjz, dayzd, dayzf, dayzlxorder, dayzlxcount, clzf, clnhzf, cltlxzf, cltlxnhzf, clstockzf, clstocknhzf, pmrq, weekzf, weektlxzf, weekstockzf, weekzlxorder, weekzlxcount, monthzf, monthtlxzf, monthstockzf, monthnhzf, monthzlxorder, monthzlxcount, quarterzf, quartertlxzf, quarterstockzf, quarternhzf, quarterzlxorder, quarterzlxcount, halfyearzf, halfyeartlxzf, halfyearstockzf, halfyearnhzf, halfyearzlxorder, halfyearzlxcount, yearzf, yeartlxzf, yearstockzf, yearnhzf, yearzlxorder, yearzlxcount, thisyearzf, thisyeartlxzf, thisyearstockzf, thisyearzlxorder, thisyearzlxcount, twoyearzf, twoyeartlxzf, twoyearstockzf, twoyearnhzf, twoyearzlxorder, twoyearzlxcount, threeyearzf, threeyeartlxzf, threeyearstockzf, threeyearnhzf, threeyearzlxorder, threeyearzlxcount, fiveyearzf, fiveyeartlxzf, fiveyearstockzf, fiveyearnhzf, fiveyearzlxorder, fiveyearzlxcount, clyearzf, clyearnhzf, cltwoyearzf, cltwoyearnhzf, clthreeyearzf, clthreeyearnhzf, clfiveyearzf, clfiveyearnhzf, qnzf, qntlxzf, qnstockzf, qnnhzf, qnylzf, qnyltlxzf, qnylstockzf, qnylnhzf, hishigh2015zf, hishigh2015nhzf, hishigh2015zlxorder, hishigh2015zlxcount, hishigh2007zf, hishigh2007nhzf, hishigh2007zlxorder, hishigh2007zlxcount, week4q, month4q, quarter4q, halfyear4q, year4q, thisyear4q, twoyear4q, threeyear4q, fiveyear4q, yearhc, twoyearhc, clfundhc, cltlxhc, clstockhc, jjfx_num, jjfx_zhpj, jjfx_star, jjfx_pj, jjfx_dp_jjzd, jjfx_dp_jjzd_json, jjyj_num, jjjlfx_num, jjgsfx_num, tzfg_dp, tzfg_zp, tzfg_cy, tzfg_py, tzfg_py_s, zzkd_num, zzkd_zhpj, zzkd_zhpj_all, zzkd_zhpj_s, bdfx_num, bdfx_zhpj, buystatus, buynotice, sellstatus, sellnotice, fx_sdate, fx_edate, tgfl, glfl, fl_json, fhcs, ljfh, halfyearprofit_gl, yearprofit_gl, twoyearprofit_gl, threeyearprofit_gl, fiveyearprofit_gl, profitgl_py, halfyearprofit_level, halfyearprofit_nhlevel, yearprofit_level, yearprofit_nhlevel, twoyearprofit_level, twoyearprofit_nhlevel, threeyearprofit_level, threeyearprofit_nhlevel, fiveyearprofit_level, fiveyearprofit_nhlevel, profitlevel_py, profit_date, yjfx_py, jzd_py, wdd_py, sharp_py, fhcf_py, jjgm_py, cyr_py, jbrq, holdstock_json, holdhy_json, holdhy_py, holdzq_json, yeardtsy_m, twoyeardtsy_m, threeyeardtsy_m, fiveyeardtsy_m, yeardtsy_w, twoyeardtsy_w, threeyeardtsy_w, fiveyeardtsy_w, yeardtsy_dw, twoyeardtsy_dw, threeyeardtsy_dw, fiveyeardtsy_dw, yeardtpm_m, twoyeardtpm_m, threeyeardtpm_m, fiveyeardtpm_m, yeardtpm_w, twoyeardtpm_w, threeyeardtpm_w, fiveyeardtpm_w, yeardtpm_dw, twoyeardtpm_dw, threeyeardtpm_dw, fiveyeardtpm_dw, risklevel, is_gs, count_rank5, rank_cx3y, rank_cx3y_trend, rank_zszq3y, rank_zszq3y_trend, rank_shzq3y, rank_shzq3y_trend, rank_jajx3y, rank_jajx3y_trend, bdfx_bdp_bn, bdfx_bdp_yn, bdfx_bdp_ln, bdfx_syp_bn, bdfx_syp_yn, bdfx_syp_ln, bdfx_fxp_bn, bdfx_fxp_yn, bdfx_fxp_ln, hsl_py','','utf8','|',NULL,1,''),('6','2011631244959866883','clickhouse_export_bt_fundinfo.gz.#{date}0','synctable_customer_portrait_bt_fundinfo','fundcode, updatetime, fundname, pyjc, fundname_long, backfundcode, qprq, clrq, clfe, clgm, clzspe, clzsprice, tzmb, tzfw, tzfg, tzlx, bigfundtype, is_cde, is_etf, yjjz, tzcl, stockname, jjgsid, orgname, orgname_short, tgbankid, tgbank, fundmgr, fundmgr_json, jjfe, jjfe_rq, cyrzs, jjsz, jjsz_rq, fundzt_gn, newrq, newjz, newljjz, newfqjz, oldrq, oldjz, oldljjz, oldfqjz, dayzd, dayzf, dayzlxorder, dayzlxcnt, clzf, clnhzf, cltlxzf, cltlxnhzf, clstockzf, clstocknhzf, pmrq, weekzf, weektlxzf, weekstockzf, weekzlxorder, weekzlxcnt, monthzf, monthtlxzf, monthstockzf, monthnhzf, monthzlxorder, monthzlxcnt, quarterzf, quartertlxzf, quarterstockzf, quarternhzf, quarterzlxorder, quarterzlxcnt, halfyearzf, halfyeartlxzf, halfyearstockzf, halfyearnhzf, halfyearzlxorder, halfyearzlxcnt, yearzf, yeartlxzf, yearstockzf, yearnhzf, yearzlxorder, yearzlxcnt, thisyearzf, thisyeartlxzf, thisyearstockzf, thisyearzlxorder, thisyearzlxcnt, twoyearzf, twoyeartlxzf, twoyearstockzf, twoyearnhzf, twoyearzlxorder, twoyearzlxcnt, threeyearzf, threeyeartlxzf, threeyearstockzf, threeyearnhzf, threeyearzlxorder, threeyearzlxcnt, fiveyearzf, fiveyeartlxzf, fiveyearstockzf, fiveyearnhzf, fiveyearzlxorder, fiveyearzlxcnt, clyearzf, clyearnhzf, cltwoyearzf, cltwoyearnhzf, clthreeyearzf, clthreeyearnhzf, clfiveyearzf, clfiveyearnhzf, qnzf, qntltxzf, qnstockzf, qnnhzf, qnylzf, qnyltlxzf, qnylstockzf, qnylnhzf, hishigh2015zf, hishigh2015nhzf, hishigh2015zlxorder, hishigh2015zlxcnt, hishigh2007zf, hishigh2007nhzf, hishigh2007zlxorder, hishigh2007zlxcnt, week4q, month4q, quarter4q, halfyear4q, year4q, thisyear4q, twoyear4q, threeyear4q, fiveyear4q, yearhc, twoyearhc, clfundhc, cltlxhc, clstockhc, jjfx_num, jjfx_zhpj, jjfx_star, jjfx_pj, jjfx_dp_jjzd, jjfx_dp_jjzd_json, jjyj_num, jjjlfx_num, jjgsfx_num, tzfg_dp, tzfg_zp, tzfg_cy, tzfg_py, tzfg_py_s, zzkd_num, zzkd_zhpj, zzkd_zhpj_all, zzkd_zhpj_s, bdfx_num, bdfx_zhpj, buystatus, buynotice, sellstatus, sellnotice, fx_sdate, fx_edate, tgfl, glfl, fl_json, fhcs, ljfh, halfyearprofit_gl, yearprofit_gl, twoyearprofit_gl, threeyearprofit_gl, fiveyearprofit_gl, profitgl_py, halfyearprofit_level, halfyearprofit_nhlevel, yearprofit_level, yearprofit_nhlevel, twoyearprofit_level, twoyearprofit_nhlevel, threeyearprofit_level, threeyearprofit_nhlevel, fiveyearprofit_level, fiveyearprofit_nhlevel, profitlevel_py, profit_date, yjfx_py, jzd_py, wdd_py, sharp_py, fhcf_py, jjgm_py, cyr_py, jbrq, holdstock_json, holdhy_json, holdhy_py, holdzq_json, yeardtsy_m, twoyeardtsy_m, threeyeardtsy_m, fiveyeardtsy_m, yeardtsy_w, twoyeardtsy_w, threeyeardtsy_w, fiveyeardtsy_w, yeardtsy_dw, twoyeardtsy_dw, threeyeardtsy_dw, fiveyeardtsy_dw, yeardtpm_m, twoyeardtpm_m, threeyeardtpm_m, fiveyeardtpm_m, yeardtpm_w, twoyeardtpm_w, threeyeardtpm_w, fiveyeardtpm_w, yeardtpm_dw, twoyeardtpm_dw, threeyeardtpm_dw, fiveyeardtpm_dw, risklevel, is_gs, count_rank5, rank_cx3y, rank_cx3y_trend, rank_zszq3y, rank_zszq3y_trend, rank_shzq3y, rank_shzq3y_trend, rank_jajx3y, rank_jajx3y_trend, bdfx_bdp_bn, bdfx_bdp_yn, bdfx_bdp_ln, bdfx_syp_bn, bdfx_syp_yn, bdfx_syp_ln, bdfx_fxp_bn, bdfx_fxp_yn, bdfx_fxp_ln, hsl_py','','utf8','\\u0000',NULL,1,'');
/*!40000 ALTER TABLE `nbatch_job_work_export_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_work_import_file`
--

DROP TABLE IF EXISTS `nbatch_job_work_import_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_work_import_file` (
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
-- Dumping data for table `nbatch_job_work_import_file`
--

LOCK TABLES `nbatch_job_work_import_file` WRITE;
/*!40000 ALTER TABLE `nbatch_job_work_import_file` DISABLE KEYS */;
INSERT INTO `nbatch_job_work_import_file` VALUES ('1','2','gauss_export_dmap_assess_plan_#{special}.gz.#{date}0','dmap_assess_plan','plan_id, plan_name, plan_cycle_start_time, plan_cycle_end_time, score_type, plan_status, plan_desc, create_time, create_user, create_user_dept, create_user_role_id, is_delete, update_time, review_user, review_time','plan_id','utf8','|',0,1,'{ \n    \"special\": \"123\"\n}'),('2','2001530610352967681','gauss_export_bt_fundinfo.gz.#{date}0','bt_fundinfo','fundcode, updatetime, fundname, pyjc, fundname_long, backfundcode, qprq, clrq, clfe, clgm, clzspe, clzsprice, tzmb, tzfw, tzfg, tzlx, bigfundtype, is_cde, is_etf, yjjz, tzcl, stockname, jjgsid, orgname, orgname_short, tgbankid, tgbank, fundmgr, fundmgr_json, jjfe, jjfe_rq, cyrzs, jjsz, jjsz_rq, fundzt_gn, newrq, newjz, newljjz, newfqjz, oldrq, oldjz, oldljjz, oldfqjz, dayzd, dayzf, dayzlxorder, dayzlxcount, clzf, clnhzf, cltlxzf, cltlxnhzf, clstockzf, clstocknhzf, pmrq, weekzf, weektlxzf, weekstockzf, weekzlxorder, weekzlxcount, monthzf, monthtlxzf, monthstockzf, monthnhzf, monthzlxorder, monthzlxcount, quarterzf, quartertlxzf, quarterstockzf, quarternhzf, quarterzlxorder, quarterzlxcount, halfyearzf, halfyeartlxzf, halfyearstockzf, halfyearnhzf, halfyearzlxorder, halfyearzlxcount, yearzf, yeartlxzf, yearstockzf, yearnhzf, yearzlxorder, yearzlxcount, thisyearzf, thisyeartlxzf, thisyearstockzf, thisyearzlxorder, thisyearzlxcount, twoyearzf, twoyeartlxzf, twoyearstockzf, twoyearnhzf, twoyearzlxorder, twoyearzlxcount, threeyearzf, threeyeartlxzf, threeyearstockzf, threeyearnhzf, threeyearzlxorder, threeyearzlxcount, fiveyearzf, fiveyeartlxzf, fiveyearstockzf, fiveyearnhzf, fiveyearzlxorder, fiveyearzlxcount, clyearzf, clyearnhzf, cltwoyearzf, cltwoyearnhzf, clthreeyearzf, clthreeyearnhzf, clfiveyearzf, clfiveyearnhzf, qnzf, qntlxzf, qnstockzf, qnnhzf, qnylzf, qnyltlxzf, qnylstockzf, qnylnhzf, hishigh2015zf, hishigh2015nhzf, hishigh2015zlxorder, hishigh2015zlxcount, hishigh2007zf, hishigh2007nhzf, hishigh2007zlxorder, hishigh2007zlxcount, week4q, month4q, quarter4q, halfyear4q, year4q, thisyear4q, twoyear4q, threeyear4q, fiveyear4q, yearhc, twoyearhc, clfundhc, cltlxhc, clstockhc, jjfx_num, jjfx_zhpj, jjfx_star, jjfx_pj, jjfx_dp_jjzd, jjfx_dp_jjzd_json, jjyj_num, jjjlfx_num, jjgsfx_num, tzfg_dp, tzfg_zp, tzfg_cy, tzfg_py, tzfg_py_s, zzkd_num, zzkd_zhpj, zzkd_zhpj_all, zzkd_zhpj_s, bdfx_num, bdfx_zhpj, buystatus, buynotice, sellstatus, sellnotice, fx_sdate, fx_edate, tgfl, glfl, fl_json, fhcs, ljfh, halfyearprofit_gl, yearprofit_gl, twoyearprofit_gl, threeyearprofit_gl, fiveyearprofit_gl, profitgl_py, halfyearprofit_level, halfyearprofit_nhlevel, yearprofit_level, yearprofit_nhlevel, twoyearprofit_level, twoyearprofit_nhlevel, threeyearprofit_level, threeyearprofit_nhlevel, fiveyearprofit_level, fiveyearprofit_nhlevel, profitlevel_py, profit_date, yjfx_py, jzd_py, wdd_py, sharp_py, fhcf_py, jjgm_py, cyr_py, jbrq, holdstock_json, holdhy_json, holdhy_py, holdzq_json, yeardtsy_m, twoyeardtsy_m, threeyeardtsy_m, fiveyeardtsy_m, yeardtsy_w, twoyeardtsy_w, threeyeardtsy_w, fiveyeardtsy_w, yeardtsy_dw, twoyeardtsy_dw, threeyeardtsy_dw, fiveyeardtsy_dw, yeardtpm_m, twoyeardtpm_m, threeyeardtpm_m, fiveyeardtpm_m, yeardtpm_w, twoyeardtpm_w, threeyeardtpm_w, fiveyeardtpm_w, yeardtpm_dw, twoyeardtpm_dw, threeyeardtpm_dw, fiveyeardtpm_dw, risklevel, is_gs, count_rank5, rank_cx3y, rank_cx3y_trend, rank_zszq3y, rank_zszq3y_trend, rank_shzq3y, rank_shzq3y_trend, rank_jajx3y, rank_jajx3y_trend, bdfx_bdp_bn, bdfx_bdp_yn, bdfx_bdp_ln, bdfx_syp_bn, bdfx_syp_yn, bdfx_syp_ln, bdfx_fxp_bn, bdfx_fxp_yn, bdfx_fxp_ln, hsl_py','fundcode','utf8','|',0,1,'{     '),('3','2011706781334089730','gauss_export_bt_fundinfo.gz.#{date}0','synctable_customer_portrait_bt_fundinfo','fundcode, updatetime, fundname, pyjc, fundname_long, backfundcode, qprq, clrq, clfe, clgm, clzspe, clzsprice, tzmb, tzfw, tzfg, tzlx, bigfundtype, is_cde, is_etf, yjjz, tzcl, stockname, jjgsid, orgname, orgname_short, tgbankid, tgbank, fundmgr, fundmgr_json, jjfe, jjfe_rq, cyrzs, jjsz, jjsz_rq, fundzt_gn, newrq, newjz, newljjz, newfqjz, oldrq, oldjz, oldljjz, oldfqjz, dayzd, dayzf, dayzlxorder, dayzlxcnt, clzf, clnhzf, cltlxzf, cltlxnhzf, clstockzf, clstocknhzf, pmrq, weekzf, weektlxzf, weekstockzf, weekzlxorder, weekzlxcnt, monthzf, monthtlxzf, monthstockzf, monthnhzf, monthzlxorder, monthzlxcnt, quarterzf, quartertlxzf, quarterstockzf, quarternhzf, quarterzlxorder, quarterzlxcnt, halfyearzf, halfyeartlxzf, halfyearstockzf, halfyearnhzf, halfyearzlxorder, halfyearzlxcnt, yearzf, yeartlxzf, yearstockzf, yearnhzf, yearzlxorder, yearzlxcnt, thisyearzf, thisyeartlxzf, thisyearstockzf, thisyearzlxorder, thisyearzlxcnt, twoyearzf, twoyeartlxzf, twoyearstockzf, twoyearnhzf, twoyearzlxorder, twoyearzlxcnt, threeyearzf, threeyeartlxzf, threeyearstockzf, threeyearnhzf, threeyearzlxorder, threeyearzlxcnt, fiveyearzf, fiveyeartlxzf, fiveyearstockzf, fiveyearnhzf, fiveyearzlxorder, fiveyearzlxcnt, clyearzf, clyearnhzf, cltwoyearzf, cltwoyearnhzf, clthreeyearzf, clthreeyearnhzf, clfiveyearzf, clfiveyearnhzf, qnzf, qntltxzf, qnstockzf, qnnhzf, qnylzf, qnyltlxzf, qnylstockzf, qnylnhzf, hishigh2015zf, hishigh2015nhzf, hishigh2015zlxorder, hishigh2015zlxcnt, hishigh2007zf, hishigh2007nhzf, hishigh2007zlxorder, hishigh2007zlxcnt, week4q, month4q, quarter4q, halfyear4q, year4q, thisyear4q, twoyear4q, threeyear4q, fiveyear4q, yearhc, twoyearhc, clfundhc, cltlxhc, clstockhc, jjfx_num, jjfx_zhpj, jjfx_star, jjfx_pj, jjfx_dp_jjzd, jjfx_dp_jjzd_json, jjyj_num, jjjlfx_num, jjgsfx_num, tzfg_dp, tzfg_zp, tzfg_cy, tzfg_py, tzfg_py_s, zzkd_num, zzkd_zhpj, zzkd_zhpj_all, zzkd_zhpj_s, bdfx_num, bdfx_zhpj, buystatus, buynotice, sellstatus, sellnotice, fx_sdate, fx_edate, tgfl, glfl, fl_json, fhcs, ljfh, halfyearprofit_gl, yearprofit_gl, twoyearprofit_gl, threeyearprofit_gl, fiveyearprofit_gl, profitgl_py, halfyearprofit_level, halfyearprofit_nhlevel, yearprofit_level, yearprofit_nhlevel, twoyearprofit_level, twoyearprofit_nhlevel, threeyearprofit_level, threeyearprofit_nhlevel, fiveyearprofit_level, fiveyearprofit_nhlevel, profitlevel_py, profit_date, yjfx_py, jzd_py, wdd_py, sharp_py, fhcf_py, jjgm_py, cyr_py, jbrq, holdstock_json, holdhy_json, holdhy_py, holdzq_json, yeardtsy_m, twoyeardtsy_m, threeyeardtsy_m, fiveyeardtsy_m, yeardtsy_w, twoyeardtsy_w, threeyeardtsy_w, fiveyeardtsy_w, yeardtsy_dw, twoyeardtsy_dw, threeyeardtsy_dw, fiveyeardtsy_dw, yeardtpm_m, twoyeardtpm_m, threeyeardtpm_m, fiveyeardtpm_m, yeardtpm_w, twoyeardtpm_w, threeyeardtpm_w, fiveyeardtpm_w, yeardtpm_dw, twoyeardtpm_dw, threeyeardtpm_dw, fiveyeardtpm_dw, risklevel, is_gs, count_rank5, rank_cx3y, rank_cx3y_trend, rank_zszq3y, rank_zszq3y_trend, rank_shzq3y, rank_shzq3y_trend, rank_jajx3y, rank_jajx3y_trend, bdfx_bdp_bn, bdfx_bdp_yn, bdfx_bdp_ln, bdfx_syp_bn, bdfx_syp_yn, bdfx_syp_ln, bdfx_fxp_bn, bdfx_fxp_yn, bdfx_fxp_ln, hsl_py','fundcode','utf8','|',0,1,'{ \n    \"special\": \"123\"\n}');
/*!40000 ALTER TABLE `nbatch_job_work_import_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_work_node`
--

DROP TABLE IF EXISTS `nbatch_job_work_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_work_node` (
  `node_id` varchar(32) NOT NULL COMMENT '作业节点id',
  `work_id` varchar(32) NOT NULL COMMENT '作业id',
  `node_name` varchar(300) NOT NULL COMMENT '节点名称',
  `node_desc` varchar(300) DEFAULT NULL COMMENT '节点描述',
  `node_status` tinyint NOT NULL DEFAULT '0' COMMENT '节点状态：0=停用、1=启用',
  `node_type` varchar(20) NOT NULL COMMENT 'script:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件',
  `db_type` varchar(32) DEFAULT NULL COMMENT '翻牌日期',
  `execute_content` text COMMENT '执行内容',
  `execute_content_param` text COMMENT '执行内容参数',
  `execute_handler` text COMMENT 'bean执行器',
  `script_type` varchar(32) DEFAULT NULL COMMENT '脚本类型',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `error_strategy` varchar(32) DEFAULT 'retry' COMMENT '失败策略：stop-该运行节点停止, skip-跳过继续, retry-重试',
  `retry_times` int DEFAULT '5' COMMENT '重试次数',
  PRIMARY KEY (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业节点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_work_node`
--

LOCK TABLES `nbatch_job_work_node` WRITE;
/*!40000 ALTER TABLE `nbatch_job_work_node` DISABLE KEYS */;
INSERT INTO `nbatch_job_work_node` VALUES ('2001530610352967681','1991745845269688321','高斯数据库导入节点','高斯数据库导入节点',2,'file_to_db','openGauss','','','','','2026-06-04 15:15:54','retry',5),('2001537087562166273','1991745845269688321','高斯数据库处理数据','高斯数据库处理数据',1,'bean','','','','handleComputation','','2025-12-18 14:16:43','retry',5),('2001537294454599681','1991745845269688321','高斯数据库导出数据','高斯数据库导出数据',2,'db_to_file','openGauss','','','','','2026-06-04 15:15:46','retry',5),('2001905723405504513','2001905454051495938','测试lua脚本节点','测试lua脚本节点',1,'bean','','','C:\\disk\\project\\work\\2025\\nbatch\\lua\\test3.lua','executeLuaScript','','2025-12-19 14:41:33','retry',5),('2011631244959866882','2011630712279064578','gauss->clickhouse gauss导出','gauss->clickhouse gauss导出',1,'db_to_file','openGauss','','','','','2026-01-15 17:27:44','retry',5),('2011631244959866883','2011630712279064578','gauss->clickhouse clickhouse 导出','gauss->clickhouse clickhouse 导出',1,'db_to_file','clickhouse','','','','','2026-01-15 17:27:44','retry',5),('2011631731947921410','2011630712279064578','gauss->clickhouse clickhouse创建表','gauss->clickhouse clickhouse创建表',1,'bean','','','bt_fundinfo','gaussTableToMysql','','2026-01-15 10:49:14','retry',5),('2011706781334089730','2011630712279064578','gauss->clickhouse 导入clickhouse','gauss->clickhouse 导入clickhouse',1,'file_to_db','clickhouse','','','','','2026-01-15 17:27:58','retry',5);
/*!40000 ALTER TABLE `nbatch_job_work_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_work_node_relation`
--

DROP TABLE IF EXISTS `nbatch_job_work_node_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_work_node_relation` (
  `node_relation_id` varchar(32) NOT NULL COMMENT '作业节点关系id',
  `work_id` varchar(32) NOT NULL COMMENT '作业id',
  `node_id1` varchar(32) NOT NULL COMMENT '节点1',
  `node_id2` varchar(32) NOT NULL COMMENT '节点2',
  `node_order` tinyint DEFAULT NULL COMMENT '节点顺序',
  PRIMARY KEY (`node_relation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业节点关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_work_node_relation`
--

LOCK TABLES `nbatch_job_work_node_relation` WRITE;
/*!40000 ALTER TABLE `nbatch_job_work_node_relation` DISABLE KEYS */;
INSERT INTO `nbatch_job_work_node_relation` VALUES ('2012452058215051266','2011630712279064578','2011706781334089730','2011631731947921410',NULL),('2012452058227634177','2011630712279064578','2011631731947921410','2011631244959866882',NULL),('2012452058240217090','2011630712279064578','2011631244959866883','2011706781334089730',NULL),('2062433406274863105','1991745845269688321','2001537087562166273','2001537294454599681',NULL),('2062433406283251713','1991745845269688321','2001530610352967681','2001537087562166273',NULL);
/*!40000 ALTER TABLE `nbatch_job_work_node_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_work_run`
--

DROP TABLE IF EXISTS `nbatch_job_work_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_work_run` (
  `run_work_id` varchar(32) NOT NULL COMMENT '运行作业id',
  `work_id` varchar(32) NOT NULL COMMENT '作业id',
  `run_work_status` tinyint NOT NULL DEFAULT '0' COMMENT '运行状态：0=待执行、1=进行中、2=执行完毕',
  `turn_date` date DEFAULT NULL COMMENT '翻牌日期',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `context_json` text COMMENT '流程上下文变量（JSON格式），用于节点间传参',
  `work_type` int DEFAULT '0' COMMENT '作业类型：0=普通作业、1=定时作业',
  PRIMARY KEY (`run_work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运行作业表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_work_run`
--

LOCK TABLES `nbatch_job_work_run` WRITE;
/*!40000 ALTER TABLE `nbatch_job_work_run` DISABLE KEYS */;
INSERT INTO `nbatch_job_work_run` VALUES ('2062465995090018304','1991745845269688321',3,'2026-06-04','2026-06-04 17:26:27',NULL,0);
/*!40000 ALTER TABLE `nbatch_job_work_run` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_work_run_node`
--

DROP TABLE IF EXISTS `nbatch_job_work_run_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_work_run_node` (
  `run_node_id` varchar(32) NOT NULL COMMENT '运行节点id',
  `run_work_id` varchar(32) NOT NULL COMMENT '执行作业id',
  `work_id` varchar(32) DEFAULT NULL COMMENT '作业id',
  `node_id` varchar(32) NOT NULL COMMENT '作业节点id',
  `node_run_status` tinyint NOT NULL DEFAULT '0' COMMENT '运行状态：0=待执行、1=进行中、2=执行完毕、3=失败',
  `turn_date` date DEFAULT NULL COMMENT '翻牌日期',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `error_strategy` varchar(32) DEFAULT NULL COMMENT '失败策略：stop-该运行节点停止, skip-跳过继续, retry-重试',
  `retry_times` int DEFAULT NULL COMMENT '重试次数',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`run_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业运行节点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_work_run_node`
--

LOCK TABLES `nbatch_job_work_run_node` WRITE;
/*!40000 ALTER TABLE `nbatch_job_work_run_node` DISABLE KEYS */;
INSERT INTO `nbatch_job_work_run_node` VALUES ('2062465995094212610','2062465995090018304','1991745845269688321','2001530610352967681',0,'2026-06-04','2026-06-04 17:26:27','retry',5,NULL,NULL),('2062465995094212611','2062465995090018304','1991745845269688321','2001537087562166273',0,'2026-06-04','2026-06-04 17:26:27','retry',5,NULL,NULL),('2062465995094212612','2062465995090018304','1991745845269688321','2001537294454599681',3,'2026-06-04','2026-06-04 17:26:27','retry',2,'2026-06-04 09:30:32',NULL);
/*!40000 ALTER TABLE `nbatch_job_work_run_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nbatch_job_work_run_node_log`
--

DROP TABLE IF EXISTS `nbatch_job_work_run_node_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_work_run_node_log` (
  `node_log_id` varchar(32) NOT NULL COMMENT '节点日志id',
  `work_id` varchar(32) NOT NULL COMMENT '作业id',
  `run_work_id` varchar(32) NOT NULL COMMENT '运行作业id',
  `node_id` varchar(32) NOT NULL COMMENT '作业节点id',
  `run_node_id` varchar(32) NOT NULL COMMENT '运行作业节点id',
  `handle_code` int NOT NULL COMMENT '执行状态',
  `handle_msg` text COMMENT '执行信息',
  `executor_address` varchar(255) DEFAULT NULL COMMENT '执行器地址',
  `create_time` datetime DEFAULT NULL COMMENT '执行-时间',
  `call_back_time` datetime DEFAULT NULL COMMENT '执行-时间',
  PRIMARY KEY (`node_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业运行节点日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_work_run_node_log`
--


--
-- Table structure for table `nbatch_job_work_run_node_log_detail`
--

DROP TABLE IF EXISTS `nbatch_job_work_run_node_log_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nbatch_job_work_run_node_log_detail` (
  `detail_log_id` varchar(32) NOT NULL COMMENT '节点日志id',
  `work_id` varchar(32) NOT NULL COMMENT '作业id',
  `run_work_id` varchar(32) NOT NULL COMMENT '运行作业id',
  `node_id` varchar(32) NOT NULL COMMENT '作业节点id',
  `run_node_id` varchar(32) NOT NULL COMMENT '运行作业节点id',
  `handle_msg` text COMMENT '执行信息',
  `execute_time` datetime DEFAULT NULL COMMENT '执行-时间',
  `call_back_time` datetime DEFAULT NULL COMMENT '执行-时间',
  PRIMARY KEY (`detail_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业运行节点日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nbatch_job_work_run_node_log_detail`
--

LOCK TABLES `nbatch_job_work_run_node_log_detail` WRITE;
/*!40000 ALTER TABLE `nbatch_job_work_run_node_log_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `nbatch_job_work_run_node_log_detail` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-05  9:14:17
