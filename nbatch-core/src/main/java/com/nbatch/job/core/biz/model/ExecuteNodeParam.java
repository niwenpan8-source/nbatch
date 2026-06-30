package com.nbatch.job.core.biz.model;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.nbatch.job.core.thread.RunNodeLogDetailCallbackThread;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @description: 执行节点参数
 * @author: Mr.ni
 * @date: 2025/11/20
 */
@Data
public class ExecuteNodeParam {

    /**
     * 运行作业id
     */
    private String workId;
    private String runWorkId;

    /**
     * 作业节点id
     */
    private String nodeId;
    private String runNodeId;

    /**
     * 运行节点日志id
     */
    private String nodeLogId = IdUtil.getSnowflakeNextIdStr();

    /**
     * script:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件
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
     * 节点关系id列表
     */
    private List<String> nodeRelationIdList;

    /**
     * 推送运行节点日志详细回调
     */
    public void pushRunNodeLogDetailCallback(String msg) {
        RunNodeLogDetailParam runNodeLogDetailParam = new RunNodeLogDetailParam();
        runNodeLogDetailParam.setWorkId(workId).setRunWorkId(runWorkId)
                .setNodeId(nodeId).setRunNodeId(runNodeId)
                .setTurnDate(turnDate)
                .setExecuteTime(DateUtil.date()).setHandleMsg(msg);
        RunNodeLogDetailCallbackThread.pushRunNodeLogDetailCallback(runNodeLogDetailParam);
    }



}
