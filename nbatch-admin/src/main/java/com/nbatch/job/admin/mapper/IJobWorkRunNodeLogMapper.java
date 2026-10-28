package com.nbatch.job.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 作业运行节点日志
 */
public interface IJobWorkRunNodeLogMapper extends BaseMapper<JobWorkRunNodeLogPo> {

    /**
     * 查询当前最新执行记录为失败的节点。
     * 同一节点后续产生成功、运行中或新的失败日志时，旧失败日志不再作为当前异常展示。
     */
    @Select("SELECT current_log.* FROM nbatch_work_run_node_log current_log "
            + "WHERE current_log.handle_code = #{handleCode} "
            + "AND NOT EXISTS (SELECT 1 FROM nbatch_work_run_node_log newer_log "
            + "WHERE newer_log.work_id = current_log.work_id "
            + "AND newer_log.node_id = current_log.node_id "
            + "AND (newer_log.create_time > current_log.create_time "
            + "OR (newer_log.create_time = current_log.create_time "
            + "AND newer_log.node_log_id > current_log.node_log_id))) "
            + "ORDER BY current_log.create_time DESC, current_log.node_log_id DESC LIMIT #{limit}")
    List<JobWorkRunNodeLogPo> selectLatestFailedNodeLogs(@Param("handleCode") Integer handleCode,
                                                         @Param("limit") int limit);
}
