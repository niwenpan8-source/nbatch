package com.nbatch.job.core.biz.model;

import cn.hutool.core.util.IdUtil;
import com.nbatch.job.core.files.RunNodeLogDetailLog;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 执行节点参数
 */
@Data
@Accessors(chain = true)
public class ExecuteNodeParam {

    /**
     * 作业标识
     */
    private String workId;

    /**
     * 步骤标识
     */
    private String nodeId;

    /**
     * 节点所属流程组，仅由管理端调度使用。
     */
    private String groupId;

    /**
     * 节点执行日志标识
     */
    private String nodeLogId = IdUtil.getSnowflakeNextIdStr();

    /**
     * 节点类型：script（脚本）、store_procedure（存储过程）、execute_sql（执行 SQL）、file_to_db（文件导入数据库）、db_to_file（数据库导出文件）
     */
    private String nodeType;

    /**
     * 数据库类型
     */
    private String dbType;

    /**
     * 翻牌日期
     */
    private Date turnDate;

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
     * 执行文件导入数据库参数
     */
    private ExecuteFileToDbParam executeFileToDbParam;

    /**
     * 执行数据库导出文件参数
     */
    private ExecuteDbToFileParam executeDbToFileParam;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 节点超时时间，单位秒，0不限
     */
    private Integer timeout;

    /**
     * 失败重试次数
     */
    private Integer retryCount;

    /**
     * 重试间隔，单位秒
     */
    private Integer retryInterval;

    /**
     * 失败策略：stop-停止整个流程, skip-跳过继续, retry-重试
     */
    private String errorStrategy;

    /**
     * 运行节点状态
     */
    private Integer nodeRunStatus;

    /**
     * 推送运行节点日志详细回调
     */
    public void pushRunNodeLogDetailCallback(String msg) {
        RunNodeLogDetailParam runNodeLogDetailParam = new RunNodeLogDetailParam();
        runNodeLogDetailParam.setDetailLogId(IdUtil.getSnowflakeNextIdStr())
                .setWorkId(workId).setNodeLogId(nodeLogId)
                .setNodeId(nodeId)
                .setTurnDate(turnDate)
                .setExecuteTime(LocalDateTime.now()).setHandleMsg(msg);
        RunNodeLogDetailLog.getInstance().append(runNodeLogDetailParam);
    }

}
