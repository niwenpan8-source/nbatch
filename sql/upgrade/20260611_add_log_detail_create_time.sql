ALTER TABLE `nbatch_job_work_run_node_log_detail`
    ADD COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间' AFTER `handle_msg`;

UPDATE `nbatch_job_work_run_node_log_detail`
SET `create_time` = COALESCE(`execute_time`, `call_back_time`)
WHERE `create_time` IS NULL;
