ALTER TABLE `nbatch_job_work`
    ADD COLUMN `init_turn_date` date DEFAULT NULL COMMENT '初始化翻牌日期' AFTER `version`;
