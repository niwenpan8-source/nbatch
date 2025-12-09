package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobRunWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.enums.RunWorkStatusEnum;
import com.nbatch.job.admin.core.enums.WorkTypeEnum;
import com.nbatch.job.admin.mapper.IJobRunWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @description: 运行作业帮助类
 * @author: Mr.ni
 * @date: 2025/11/20
 */
@Component
@RequiredArgsConstructor
public class RunWorkHelper {

    private final IJobWorkMapper jobWorkMapper;

    private final IJobRunWorkMapper jobRunWorkMapper;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    public JobRunWorkPo initRunWork(String workId) {

        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        List<JobRunWorkPo> jobRunWorkList = jobRunWorkMapper
                .selectList(Wrappers.lambdaQuery(JobRunWorkPo.class)
                        .eq(JobRunWorkPo::getWorkId, jobWorkPo.getWorkId())
                        .orderByDesc(JobRunWorkPo::getCreateTime));
        JobRunWorkPo jobRunWorkPo;
        if (CollUtil.isEmpty(jobRunWorkList)) {
            jobRunWorkPo = initRunWork(jobWorkPo.getWorkId(), jobWorkPo.getWorkType(), DateUtil.parseDate(DateUtil.today()));
        } else {
            JobRunWorkPo lastJobRunWorkPo = jobRunWorkList.get(0);
            // 如果运行状态为待执行或者进行中，则不处理
            if (lastJobRunWorkPo.getRunWorkStatus() == RunWorkStatusEnum.COMPLETE.getCode()) {
                // 当jobRunWorkPo.getTurnDate()为空时currentTurnDate为空，
                // DateUtil.compare(currentTurnDate, DateUtil.parseDate(DateUtil.today())) 为-1，所有顺序类型作业不需要增加判断
                DateTime currentTurnDate = DateUtil.offset(lastJobRunWorkPo.getTurnDate(), DateField.DAY_OF_MONTH, 1);
                if (DateUtil.compare(currentTurnDate, DateUtil.parseDate(DateUtil.today())) > 0) {
                    return null;
                }
                jobRunWorkPo = initRunWork(lastJobRunWorkPo.getWorkId(), jobWorkPo.getWorkType(), currentTurnDate);
            } else {
                return lastJobRunWorkPo;
            }
        }
        List<JobWorkRunNodePo> insertRunNodeList = new ArrayList<>();
        List<JobWorkNodePo> jobWorkNodePos = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, jobRunWorkPo.getWorkId()));
        if (CollUtil.isEmpty(jobWorkNodePos)) {
            return null;
        }
        for (JobWorkNodePo elementNode : jobWorkNodePos) {
            JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
            jobWorkRunNodePo.setRunWorkId(jobRunWorkPo.getRunWorkId());
            jobWorkRunNodePo.setNodeId(elementNode.getNodeId());
            jobWorkRunNodePo.setNodeRunStatus(RunWorkStatusEnum.WAIT.getCode());
            if (jobRunWorkPo.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode()) {
                jobWorkRunNodePo.setTurnDate(jobRunWorkPo.getTurnDate());
            }
            jobWorkRunNodePo.setCreateTime(DateUtil.date());
            insertRunNodeList.add(jobWorkRunNodePo);
        }
        for (JobWorkRunNodePo jobWorkRunNodePo : insertRunNodeList) {
            jobWorkRunNodeMapper.insert(jobWorkRunNodePo);
        }
        jobRunWorkMapper.insert(jobRunWorkPo);
        return jobRunWorkPo;
    }

    /**
     * 初始化运行作业
     * @param workId 作业id
     */
    private JobRunWorkPo initRunWork(String workId, Integer workType, Date turnDate) {
        String runNodeId = IdUtil.getSnowflakeNextIdStr();
        JobRunWorkPo jobRunWorkPo = new JobRunWorkPo();
        jobRunWorkPo.setRunWorkId(runNodeId);
        jobRunWorkPo.setWorkId(workId);
        jobRunWorkPo.setRunWorkStatus(RunWorkStatusEnum.WAIT.getCode());
        if (workType == WorkTypeEnum.TYPE_TURN.getCode()) {
            jobRunWorkPo.setTurnDate(turnDate);
        }
        jobRunWorkPo.setWorkType(workType);
        jobRunWorkPo.setCreateTime(DateUtil.date());
        return jobRunWorkPo;
    }

    /**
     * 删除运行作业(只保留30天的记录)
     */
    public void deleteRunWork() {
        List<JobRunWorkPo> jobRunWorkPoList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobRunWorkPo.class)
                .eq(JobRunWorkPo::getRunWorkStatus, RunWorkStatusEnum.COMPLETE.getCode())
                .lt(JobRunWorkPo::getCreateTime, DateUtil.offsetDay(new Date(), -30)));
        for (JobRunWorkPo jobRunWorkPo : jobRunWorkPoList) {
            jobWorkRunNodeMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));
            jobRunWorkMapper.deleteById(jobRunWorkPo);
        }
    }

    /**
     * 得到所有需要运行的作业
     */
    public List<JobRunWorkPo> getALlNeedRunWorkList() {
        return jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobRunWorkPo.class)
                .in(JobRunWorkPo::getRunWorkStatus, RunWorkStatusEnum.WAIT.getCode(), RunWorkStatusEnum.RUNNING.getCode()));
    }

}
