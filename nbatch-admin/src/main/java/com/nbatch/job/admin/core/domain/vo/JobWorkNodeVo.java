package com.nbatch.job.admin.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: 作业节点表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkNodeVo {

    /**
     * 作业节点id
     */
    private String nodeId;

    /**
     * 作业id
     */
    private String workId;

    /** 作业名称。 */
    private String workName;

    /** 作业类型。 */
    private Integer workType;

    /**
     * 所属流程组。
     */
    private String groupId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点描述
     */
    private String nodeDesc;

    /** 正式运行状态。 */
    private Integer nodeRunStatus;

    private String nodeRunStatusName;

    /** 当前运行业务日期。 */
    private Date turnDate;

    private String turnDateText;

    /** 当前剩余重试次数。 */
    private Integer retryTimes;

    /**
     * 节点类型：script（脚本）、store_procedure（存储过程）、execute_sql（执行 SQL）、file_to_db（文件导入数据库）、db_to_file（数据库导出文件）
     */
    private String nodeType;

    private String nodeTypeName;

    /**
     * 数据库类型
     */
    private String dbType;

    /**
     * 执行内容
     */
    private String executeContent;

    /**
     * 执行内容参数
     */
    private String executeContentParam;

    /**
     * 执行器
     */
    private String executeHandler;

    /**
     * 脚本类型 => Java,Shell,Python,PHP,Nodejs,PowerShell
     */
    private String scriptType;

    /**
     * 修改时间
     */
    private Date updateTime;

    private Date createTime;

    private Integer timeout;

    private Integer retryCount;

    private Integer retryInterval;

    /**
     * 失败策略：stop-该运行节点停止, skip-跳过继续, retry-重试
     */
    private String errorStrategy;

    private String notifyEmail;

    private String importFileId;

    private String importFileName;

    private String importTableName;

    private String importTableFiled;

    private String importTableCondition;

    private String importFileCode;

    private String importSep;

    private Integer importAllUpdate;

    private Integer importIsGzip;

    private String importFileNameParam;

    private String exportFileId;

    private String exportFileName;

    private String exportTableName;

    private String exportTableFiled;

    private String exportTableCondition;

    private String exportFileCode;

    private String exportSep;

    private Integer exportAllUpdate;

    private Integer exportIsGzip;

    private String exportFileNameParam;

}
