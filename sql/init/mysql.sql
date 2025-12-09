-- —————————————————————— job group and registry ——————————————————

drop table if exists nbatch_job_group;
CREATE TABLE `nbatch_job_group`
(
    `id`           varchar(32)     NOT NULL,
    `app_name`     varchar(64) NOT NULL COMMENT '执行器AppName',
    `title`        varchar(12) NOT NULL COMMENT '执行器名称',
    `address_type` tinyint(4)  NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
    `address_list` text COMMENT '执行器地址列表，多地址逗号分隔',
    `update_time`  datetime             DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

drop table if exists nbatch_job_registry;
CREATE TABLE `nbatch_job_registry`
(
    `id`             varchar(32)      NOT NULL,
    `registry_group` varchar(50)  NOT NULL,
    `registry_key`   varchar(255) NOT NULL,
    `registry_value` varchar(255) NOT NULL,
    `update_time`    datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_g_k_v` (`registry_group`, `registry_key`, `registry_value`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- —————————————————————— job info ——————————————————

drop table if exists nbatch_job_info;
CREATE TABLE `nbatch_job_info`
(
    `id`                        varchar(32)      NOT NULL,
    `job_group`                 varchar(32)      NOT NULL COMMENT '执行器主键ID',
    `job_desc`                  varchar(255) NOT NULL,
    `add_time`                  datetime              DEFAULT NULL,
    `update_time`               datetime              DEFAULT NULL,
    `author`                    varchar(64)           DEFAULT NULL COMMENT '作者',
    `alarm_email`               varchar(255)          DEFAULT NULL COMMENT '报警邮件',
    `schedule_type`             varchar(50)  NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
    `schedule_conf`             varchar(128)          DEFAULT NULL COMMENT '调度配置，值含义取决于调度类型',
    `misfire_strategy`          varchar(50)  NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
    `executor_route_strategy`   varchar(50)           DEFAULT NULL COMMENT '执行器路由策略',
    `executor_handler`          varchar(255)          DEFAULT NULL COMMENT '执行器任务handler',
    `executor_param`            varchar(512)          DEFAULT NULL COMMENT '执行器任务参数',
    `executor_block_strategy`   varchar(50)           DEFAULT NULL COMMENT '阻塞处理策略',
    `executor_timeout`          int(11)      NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
    `executor_fail_retry_count` int(11)      NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `glue_type`                 varchar(50)  NOT NULL COMMENT 'GLUE类型，BEAN类型（任务执行类型），WORK类型',
    `glue_source`               mediumtext COMMENT 'GLUE源代码',
    `glue_remark`               varchar(128)          DEFAULT NULL COMMENT 'GLUE备注',
    `glue_updatetime`           datetime              DEFAULT NULL COMMENT 'GLUE更新时间',
    `child_jobid`               varchar(255)          DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
    `trigger_status`            tinyint(4)   NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
    `trigger_last_time`         bigint(13)   NOT NULL DEFAULT '0' COMMENT '上次调度时间',
    `trigger_next_time`         bigint(13)   NOT NULL DEFAULT '0' COMMENT '下次调度时间',
    `work_id`         varchar(50)   NOT NULL DEFAULT '0' COMMENT '作业id',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

drop table if exists nbatch_job_logglue;
CREATE TABLE `nbatch_job_logglue`
(
    `id`          varchar(32)      NOT NULL,
    `job_id`      varchar(32)      NOT NULL COMMENT '任务，主键ID',
    `glue_type`   varchar(50) DEFAULT NULL COMMENT 'GLUE类型',
    `glue_source` mediumtext COMMENT 'GLUE源代码',
    `glue_remark` varchar(128) NOT NULL COMMENT 'GLUE备注',
    `add_time`    datetime    DEFAULT NULL,
    `update_time` datetime    DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- —————————————————————— job log and report ——————————————————

drop table if exists nbatch_job_log;
CREATE TABLE `nbatch_job_log`
(
    `id`                        varchar(32) NOT NULL,
    `job_group`                 varchar(32)    NOT NULL COMMENT '执行器主键ID',
    `job_id`                    varchar(32)    NOT NULL COMMENT '任务，主键ID',
    `executor_address`          varchar(255)        DEFAULT NULL COMMENT '执行器地址，本次执行的地址',
    `executor_handler`          varchar(255)        DEFAULT NULL COMMENT '执行器任务handler',
    `executor_param`            varchar(512)        DEFAULT NULL COMMENT '执行器任务参数',
    `executor_sharding_param`   varchar(20)         DEFAULT NULL COMMENT '执行器任务分片参数，格式如 1/2',
    `executor_fail_retry_count` int(11)    NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `trigger_time`              datetime            DEFAULT NULL COMMENT '调度-时间',
    `trigger_code`              int(11)    NOT NULL COMMENT '调度-结果',
    `trigger_msg`               text COMMENT '调度-日志',
    `handle_time`               datetime            DEFAULT NULL COMMENT '执行-时间',
    `handle_code`               int(11)    NOT NULL COMMENT '执行-状态',
    `handle_msg`                text COMMENT '执行-日志',
    `alarm_status`              tinyint(4) NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
    PRIMARY KEY (`id`),
    KEY `I_trigger_time` (`trigger_time`),
    KEY `I_handle_code` (`handle_code`),
    KEY `I_jobid_jobgroup` (`job_id`,`job_group`),
    KEY `I_job_id` (`job_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

drop table if exists nbatch_job_log_report;
CREATE TABLE `nbatch_job_log_report`
(
    `id`            varchar(32) NOT NULL,
    `trigger_day`   datetime         DEFAULT NULL COMMENT '调度-时间',
    `running_count` int(11) NOT NULL DEFAULT '0' COMMENT '运行中-日志数量',
    `suc_count`     int(11) NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
    `fail_count`    int(11) NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
    `update_time`   datetime         DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- —————————————————————— lock ——————————————————

drop table if exists nbatch_job_lock;
CREATE TABLE `nbatch_job_lock`
(
    `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
    PRIMARY KEY (`lock_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- —————————————————————— user ——————————————————

drop table if exists nbatch_job_user;
CREATE TABLE `nbatch_job_user`
(
    `id`         varchar(32)     NOT NULL,
    `username`   varchar(50) NOT NULL COMMENT '账号',
    `password`   varchar(100) NOT NULL COMMENT '密码加密信息',
    `token`      varchar(100) DEFAULT NULL COMMENT '登录token',
    `role`       tinyint(4)  NOT NULL COMMENT '角色：0-普通用户、1-管理员',
    `permission` varchar(255) DEFAULT NULL COMMENT '权限：执行器ID列表，多个逗号分割',
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_username` (`username`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


-- —————————————————————— for default data ——————————————————

INSERT INTO `nbatch_job_group`(`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`)
VALUES (1, 'xxl-job-executor-sample', '通用执行器Sample', 0, NULL, now()),
       (2, 'xxl-job-executor-sample-ai', 'AI执行器Sample', 0, NULL, now());


INSERT INTO `nbatch_job_user`(`id`, `username`, `password`, `role`, `permission`)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL);

INSERT INTO `nbatch_job_lock` (`lock_name`)
VALUES ('schedule_lock');

commit;

# 作业表
drop table if exists nbatch_job_work;
create table nbatch_job_work
(
    work_id      varchar(32) not null comment '作业id',
    work_name    varchar(300) not null comment '作业名',
    work_desc    varchar(300) comment '作业描述',
    work_type    tinyint(4) not null default 0 comment '作业类型 => 0:翻牌类型，1：顺序类型',
    work_status  tinyint(4)  not null default 0 comment '作业状态：0=停用、1=启用',
    primary key (work_id)
) engine = innodb comment = '作业表'
  default charset = utf8mb4;

# 作业执行表
drop table if exists nbatch_job_run_work;
create table nbatch_job_run_work
(
    run_work_id      varchar(32) not null comment '运行作业id',
    work_id      varchar(32) not null comment '作业id',
    run_work_status  tinyint(4)  not null default 0 comment '运行状态：0=待执行、1=进行中、2=执行完毕',
    turn_date  date comment '翻牌日期',
    create_time  datetime comment '创建时间',
    primary key (run_work_id)
) engine = innodb comment = '运行作业表'
  default charset = utf8mb4;

# 作业节点表
drop table if exists nbatch_job_work_node;
create table nbatch_job_work_node
(
    node_id      varchar(32) not null comment '作业节点id',
    work_id      varchar(32) not null comment '作业id',
    node_name    varchar(300) not null comment '节点名称',
    node_desc    varchar(300) comment '节点描述',
    node_status  tinyint(4)  not null default 0 comment '节点状态：0=停用、1=启用',
    node_type  varchar(20)  not null comment 'script:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件',
    db_type  varchar(32) comment '翻牌日期',
    execute_content  text comment '执行内容',
    execute_content_param  text comment '执行内容参数',
    execute_handler  text comment 'bean执行器',
    script_type  varchar(32) comment '脚本类型 => Java,Shell,Python,PHP,Nodejs,PowerShell',
    update_time  datetime not null comment '更新时间',
    primary key (node_id)
) engine = innodb comment = '作业节点表'
  default charset = utf8mb4;

# 运行节点表
drop table if exists nbatch_job_work_run_node;
create table nbatch_job_work_run_node
(
    run_node_id      varchar(32) not null comment '运行节点id',
    run_work_id      varchar(32) not null comment '执行作业id',
    node_id      varchar(32) not null comment '作业节点id',
    node_run_status  tinyint(4)  not null default 0 comment '运行状态：0=待执行、1=进行中、2=执行完毕',
    turn_date  date comment '翻牌日期',
    create_time  datetime comment '创建时间',
    primary key (run_node_id)
) engine = innodb comment = '作业运行节点表'
  default charset = utf8mb4;

# 作业节点导出文件表
drop table if exists nbatch_job_work_export_file;
create table nbatch_job_work_export_file (
    export_file_id varchar(32) not null comment '导出文件id',
    node_id varchar(32) not null comment '作业节点id',
    file_name varchar(200) not null comment '导出的文件名',
    export_table_name varchar(200) not null comment '导出的表名',
    export_table_filed text comment '导出的列',
    export_table_condition varchar(200) comment '删除条件',
    file_code varchar(8)  comment '文件编码',
    sep varchar(32) comment '分隔符',
    all_update int default 0 comment '是否全量文件：1全量 0增量',
    is_gzip int default 0  comment '是否压缩：1压缩 0不压缩',
    db_type varchar(32) default 0  comment '数据库类型',
    file_name_param varchar(500) comment '生成文件名时，特殊参数参数',
    primary key (export_file_id)
) engine = innodb comment = '作业节点导出文件表'
  default charset = utf8mb4;

# 作业节点导入文件表
drop table if exists nbatch_job_work_import_file;
create table nbatch_job_work_import_file (
    import_file_id varchar(32) not null comment '导入文件id',
    node_id varchar(32) not null comment '作业节点id',
    file_name              varchar(200) not null comment '导入的文件名',
    import_table_name      varchar(200) not null comment '导入的表名',
    import_table_filed     text not null comment '导入的列',
    import_table_condition text comment '导入条件',
    file_code              varchar(8) comment '文件编码',
    sep                    varchar(32) comment '分隔符',
    all_update             int default 0 comment '是否全量文件：1全量 0增量',
    is_gzip                int default 0 comment '是否压缩：1压缩 0不压缩',
    db_type varchar(32) default 0  comment '数据库类型',
    file_name_param varchar(500) comment '生成文件名时，特殊参数参数',
    primary key (import_file_id)
) engine = innodb comment = '作业节点导入文件表'
  default charset = utf8mb4;

# 作业节点关系表
drop table if exists nbatch_job_work_node_relation;
create table nbatch_job_work_node_relation (
    node_relation_id varchar(32) not null comment '作业节点关系id',
    work_id varchar(32) not null comment '作业id',
    node_id1 varchar(32) not null comment '节点1',
    node_id2 varchar(32) not null comment '节点2',
    node_order tinyint(4)  not null comment '节点顺序',
    primary key (node_relation_id)
) engine = innodb comment = '作业节点关系表'
  default charset = utf8mb4;

# 运行节点表
drop table if exists nbatch_job_work_run_node_log;
create table nbatch_job_work_run_node_log
(
    node_log_id      varchar(32) not null comment '节点日志id',
    work_id      varchar(32) not null comment '作业id',
    run_work_id      varchar(32) not null comment '运行作业id',
    node_id      varchar(32) not null comment '作业节点id',
    run_node_id      varchar(32) not null comment '运行作业节点id',
    handle_code  int not null comment '执行状态',
    handle_msg  text comment '执行信息',
    create_time datetime COMMENT '执行-时间',
    call_back_time datetime COMMENT '执行-时间',
    primary key (node_log_id)
) engine = innodb comment = '作业运行节点日志表'
  default charset = utf8mb4;

# 运行节点表
drop table if exists nbatch_job_work_run_node_log_detail;
create table nbatch_job_work_run_node_log_detail
(
    detail_log_id      varchar(32) not null comment '节点日志id',
    work_id      varchar(32) not null comment '作业id',
    run_work_id      varchar(32) not null comment '运行作业id',
    node_id      varchar(32) not null comment '作业节点id',
    run_node_id      varchar(32) not null comment '运行作业节点id',
    handle_msg  text comment '执行信息',
    execute_time datetime COMMENT '执行-时间',
    call_back_time datetime COMMENT '执行-时间',
    primary key (detail_log_id)
) engine = innodb comment = '作业运行节点日志表'
  default charset = utf8mb4;



truncate table nbatch_job_work_node_relation;
INSERT INTO nbatch_job_work_node_relation (node_relation_id, work_id, node_id1, node_id2, node_order) VALUES ('1', '1991745845269688321', '2', '1', 1);
INSERT INTO nbatch_job_work_node_relation (node_relation_id, work_id, node_id1, node_id2, node_order) VALUES ('2', '1991745845269688321', '2', '5', 2);
INSERT INTO nbatch_job_work_node_relation (node_relation_id, work_id, node_id1, node_id2, node_order) VALUES ('3', '1991745845269688321', '3', '2', 3);
INSERT INTO nbatch_job_work_node_relation (node_relation_id, work_id, node_id1, node_id2, node_order) VALUES ('4', '1991745845269688321', '4', '3', 4);
INSERT INTO nbatch_job_work_node_relation (node_relation_id, work_id, node_id1, node_id2, node_order) VALUES ('5', '1991745845269688322', '7', '6', 4);


truncate table nbatch_job_work_node;
INSERT INTO nbatch_job_work_node (node_id, work_id, node_name, node_desc, node_status, node_type, db_type, execute_content, execute_content_param, execute_handler, script_type, update_time) VALUES ('1', '1991745845269688321', 'gauss导出1', 'gauss导出', 1, 'db_to_file', 'zenith', null, null, null, null, '2025-12-02 15:36:33');
INSERT INTO nbatch_job_work_node (node_id, work_id, node_name, node_desc, node_status, node_type, db_type, execute_content, execute_content_param, execute_handler, script_type, update_time) VALUES ('2', '1991745845269688321', 'gbase导入', 'gbase导入', 1, 'file_to_db', 'gbase', null, null, null, null, '2025-12-02 15:36:33');
INSERT INTO nbatch_job_work_node (node_id, work_id, node_name, node_desc, node_status, node_type, db_type, execute_content, execute_content_param, execute_handler, script_type, update_time) VALUES ('3', '1991745845269688321', 'gauss导出', 'gauss导出', 1, 'db_to_file', 'gbase', null, null, null, null, '2025-12-02 15:36:33');
INSERT INTO nbatch_job_work_node (node_id, work_id, node_name, node_desc, node_status, node_type, db_type, execute_content, execute_content_param, execute_handler, script_type, update_time) VALUES ('4', '1991745845269688321', 'gbase导入', 'gbase导入', 1, 'file_to_db', 'zenith', null, null, null, null, '2025-12-02 15:36:33');
INSERT INTO nbatch_job_work_node (node_id, work_id, node_name, node_desc, node_status, node_type, db_type, execute_content, execute_content_param, execute_handler, script_type, update_time) VALUES ('5', '1991745845269688321', 'gauss导出2', 'gauss导出', 1, 'db_to_file', 'zenith', null, null, null, null, '2025-12-02 15:36:33');
INSERT INTO nbatch_job_work_node (node_id, work_id, node_name, node_desc, node_status, node_type, db_type, execute_content, execute_content_param, execute_handler, script_type, update_time) VALUES ('6', '1991745845269688322', 'shell脚本1', 'gauss导出', 1, 'script', '', '/usr/local/php-fcgi/bin/php /home/www/h.php
', '', null, 'shell', '2025-12-02 15:36:55');
INSERT INTO nbatch_job_work_node (node_id, work_id, node_name, node_desc, node_status, node_type, db_type, execute_content, execute_content_param, execute_handler, script_type, update_time) VALUES ('7', '1991745845269688322', 'shell脚本2', 'gauss导出', 1, 'script', '', '/usr/local/php-fcgi/bin/php /home/www/h.php
', '', null, 'shell', '2025-12-02 15:36:55');



truncate table nbatch_job_work_import_file;
INSERT INTO nbatch_job_work_import_file (import_file_id, node_id, file_name, import_table_name, import_table_filed, import_table_condition, file_code, sep, all_update, is_gzip, file_name_param) VALUES ('1', '2', 'gauss_export_dmap_assess_plan_#{special}.gz.#{date}0', 'dmap_assess_plan', 'plan_id, plan_name, plan_cycle_start_time, plan_cycle_end_time, score_type, plan_status, plan_desc, create_time, create_user, create_user_dept, create_user_role_id, is_delete, update_time, review_user, review_time', 'plan_id', 'utf8', ' | ', 0, 1, '{
    "special": "123"
}');
INSERT INTO nbatch_job_work_import_file (import_file_id, node_id, file_name, import_table_name, import_table_filed, import_table_condition, file_code, sep, all_update, is_gzip, file_name_param) VALUES ('2', '4', 'gbase_export_dmap_assess_plan_#{special}.gz.#{date}0', 'dmap_assess_plan', 'plan_id, plan_name, plan_cycle_start_time, plan_cycle_end_time, score_type, plan_status, plan_desc, create_time, create_user, create_user_dept, create_user_role_id, is_delete, update_time, review_user, review_time', 'plan_id', 'utf8', ' | ', 0, 1, '{
    "special": "123"
}');



truncate table nbatch_job_work_export_file;
INSERT INTO nbatch_job_work_export_file (export_file_id, node_id, file_name, export_table_name, export_table_filed, export_table_condition, file_code, sep, all_update, is_gzip, file_name_param) VALUES ('1', '1', 'gauss_export_dmap_assess_plan_#{special}.gz.#{date}0', 'dmap_assess_plan', 'plan_id, plan_name, plan_cycle_start_time, plan_cycle_end_time, score_type, plan_status, plan_desc, create_time, create_user, create_user_dept, create_user_role_id, is_delete, update_time, review_user, review_time', '', 'utf8', ' | ', 1, 1, '{
    "special": "123"
}');
INSERT INTO nbatch_job_work_export_file (export_file_id, node_id, file_name, export_table_name, export_table_filed, export_table_condition, file_code, sep, all_update, is_gzip, file_name_param) VALUES ('2', '5', 'gauss_export_dmap_assess_plan_assessor_#{special}.gz.#{date}0', 'dmap_assess_plan_assessor', 'plan_assessor_id, plan_id, uid, create_time, is_delete, update_time', '', 'utf8', ' | ', 1, 1, '{
    "special": "456"
}');
INSERT INTO nbatch_job_work_export_file (export_file_id, node_id, file_name, export_table_name, export_table_filed, export_table_condition, file_code, sep, all_update, is_gzip, file_name_param) VALUES ('3', '3', 'gbase_export_dmap_assess_plan_#{special}.gz.#{date}0', 'dmap_assess_plan', 'plan_id, plan_name, plan_cycle_start_time, plan_cycle_end_time, score_type, plan_status, plan_desc, create_time, create_user, create_user_dept, create_user_role_id, is_delete, update_time, review_user, review_time', '', 'utf8', ' | ', 1, 1, '{
    "special": "123"
}');


truncate table nbatch_job_work;
INSERT INTO nbatch_job_work (work_id, work_name, work_desc, work_type, work_status) VALUES ('1991745845269688321', '测试作业', '用于测试', 0, 1);
INSERT INTO nbatch_job_work (work_id, work_name, work_desc, work_type, work_status) VALUES ('1991745845269688322', '测试作业2', '用于测试2', 1, 1);


truncate table nbatch_job_user;
INSERT INTO nbatch_job_user (id, username, password, token, role, permission) VALUES ('1', 'admin', 'e10adc3949ba59abbe56e057f20f883e', null, 1, null);


truncate table nbatch_job_lock;
INSERT INTO nbatch_job_lock (lock_name) VALUES ('schedule_lock');


truncate table nbatch_job_info;
INSERT INTO nbatch_job_info (id, job_group, job_desc, add_time, update_time, author, alarm_email, schedule_type, schedule_conf, misfire_strategy, executor_route_strategy, executor_handler, executor_param, executor_block_strategy, executor_timeout, executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime, child_jobid, trigger_status, trigger_last_time, trigger_next_time, work_id) VALUES ('1991745682576830464', '1', 'work测试任务', '2025-11-21 13:49:10', '2025-11-21 13:49:10', 'admin', '', 'CRON', '* 0/5 * * * ?', 'DO_NOTHING', 'FIRST', '', '', 'SERIAL_EXECUTION', 0, 0, 'WORK', '', 'GLUE代码初始化', '2025-11-21 13:49:10', null, 0, 0, 0, '1991745845269688321');
INSERT INTO nbatch_job_info (id, job_group, job_desc, add_time, update_time, author, alarm_email, schedule_type, schedule_conf, misfire_strategy, executor_route_strategy, executor_handler, executor_param, executor_block_strategy, executor_timeout, executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime, child_jobid, trigger_status, trigger_last_time, trigger_next_time, work_id) VALUES ('1991745682576830467', '1', 'work测试任务2', '2025-11-21 13:49:10', '2025-12-04 15:30:42', 'admin', '', 'CRON', '* 0/2 * * * ?', 'DO_NOTHING', 'FIRST', '', '', 'SERIAL_EXECUTION', 0, 0, 'WORK', '', 'GLUE代码初始化', '2025-11-21 13:49:10', null, 1, 0, 1764833447000, '1991745845269688322');
INSERT INTO nbatch_job_info (id, job_group, job_desc, add_time, update_time, author, alarm_email, schedule_type, schedule_conf, misfire_strategy, executor_route_strategy, executor_handler, executor_param, executor_block_strategy, executor_timeout, executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime, child_jobid, trigger_status, trigger_last_time, trigger_next_time, work_id) VALUES ('1991752974135259136', '1', 'test任务', '2025-11-21 14:18:09', '2025-11-21 14:18:09', 'admin', '', 'CRON', '* 0/5 * * * ?', 'DO_NOTHING', 'FIRST', 'demoJobHandler', '123', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化', '2025-11-21 14:18:09', null, 0, 0, 0, '');
INSERT INTO nbatch_job_info (id, job_group, job_desc, add_time, update_time, author, alarm_email, schedule_type, schedule_conf, misfire_strategy, executor_route_strategy, executor_handler, executor_param, executor_block_strategy, executor_timeout, executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime, child_jobid, trigger_status, trigger_last_time, trigger_next_time, work_id) VALUES ('1995740147851304960', '1', 'shell脚本', '2025-12-02 14:21:45', '2025-12-02 15:41:24', 'admin', '123', 'CRON', '* 0/5 * * * ?', 'DO_NOTHING', 'FIRST', '', '', 'SERIAL_EXECUTION', 0, 0, 'GLUE_SHELL', '#!/bin/bash

# 输出鼓励语句
echo "你好，今天一定要变得更好"', '测试sql', '2025-12-02 15:41:24', null, 0, 0, 0, '0');


truncate table nbatch_job_group;
INSERT INTO nbatch_job_group (id, app_name, title, address_type, address_list, update_time) VALUES ('1', 'xxl-job-executor-sample', '通用执行器Sample', 0, null, '2025-12-04 18:34:44');
