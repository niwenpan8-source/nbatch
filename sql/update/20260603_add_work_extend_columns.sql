-- Add columns used by JobWorkPo and JobWorkNodePo.
-- Run this only if the columns do not already exist.

ALTER TABLE nbatch_job_work
    ADD COLUMN version int DEFAULT 0 COMMENT '流程版本号';

ALTER TABLE nbatch_job_work_node
    ADD COLUMN error_strategy varchar(32) DEFAULT 'stop' COMMENT '失败策略';

ALTER TABLE nbatch_job_run_work
    ADD COLUMN context_json text COMMENT '流程上下文变量（JSON格式），用于节点间传参',
    ADD COLUMN work_type int DEFAULT 0 COMMENT '作业类型：0=普通作业、1=定时作业';

ALTER TABLE nbatch_job_work_run_node
    ADD COLUMN retry_times int DEFAULT 0 COMMENT '已重试次数';
