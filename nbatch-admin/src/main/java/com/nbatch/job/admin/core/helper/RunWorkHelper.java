package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkRunPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.mapper.*;
import com.nbatch.job.core.enums.RunWorkStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    private final IJobWorkRunMapper jobRunWorkMapper;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    /**
     * 初始化运行作业
     * @param workId 作业id
     */
    @Transactional(rollbackFor = Exception.class)
    public void initRunWork(String workId) {

        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        List<JobWorkRunPo> jobRunWorkList = jobRunWorkMapper
                .selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                        .eq(JobWorkRunPo::getWorkId, jobWorkPo.getWorkId())
                        .orderByDesc(JobWorkRunPo::getCreateTime));
        JobWorkRunPo jobRunWorkPo = null;
        if (CollUtil.isEmpty(jobRunWorkList)) {
            jobRunWorkPo = initRunWork(jobWorkPo.getWorkId(), jobWorkPo.getWorkType(), DateUtil.parseDate(DateUtil.today()));
        } else {
            JobWorkRunPo lastJobRunWorkPo = jobRunWorkList.get(0);
            // 如果运行状态为待执行或者进行中，则不处理
            if (lastJobRunWorkPo.getRunWorkStatus() == RunWorkStatusEnum.COMPLETE.getCode()) {
                // 当jobRunWorkPo.getTurnDate()为空时currentTurnDate为空，
                // DateUtil.compare(currentTurnDate, DateUtil.parseDate(DateUtil.today())) 为-1，所有顺序类型作业不需要增加判断
                DateTime currentTurnDate = DateUtil.offset(lastJobRunWorkPo.getTurnDate(), DateField.DAY_OF_MONTH, 1);
                if (DateUtil.compare(currentTurnDate, DateUtil.parseDate(DateUtil.today())) > 0) {
                    return;
                }
                jobRunWorkPo = initRunWork(lastJobRunWorkPo.getWorkId(), jobWorkPo.getWorkType(), currentTurnDate);
            }
        }
        if (jobRunWorkPo == null) {
            return;
        }
        List<JobWorkRunNodePo> insertRunNodeList = new ArrayList<>();
        List<JobWorkNodePo> jobWorkNodePos = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, jobRunWorkPo.getWorkId()));
        if (CollUtil.isEmpty(jobWorkNodePos)) {
            return;
        }
        for (JobWorkNodePo elementNode : jobWorkNodePos) {
            JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
            jobWorkRunNodePo.setRunWorkId(jobRunWorkPo.getRunWorkId());
            jobWorkRunNodePo.setWorkId(jobRunWorkPo.getWorkId());
            jobWorkRunNodePo.setNodeId(elementNode.getNodeId());
            jobWorkRunNodePo.setNodeRunStatus(RunWorkStatusEnum.WAIT.getCode());
            if (jobRunWorkPo.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode()) {
                jobWorkRunNodePo.setTurnDate(jobRunWorkPo.getTurnDate());
            }
            jobWorkRunNodePo.setCreateTime(DateUtil.date());
            // 错误策略
            jobWorkRunNodePo.setErrorStrategy(elementNode.getErrorStrategy());
            jobWorkRunNodePo.setRetryTimes(elementNode.getRetryTimes());
            insertRunNodeList.add(jobWorkRunNodePo);
        }
        jobRunWorkMapper.insert(jobRunWorkPo);
        for (JobWorkRunNodePo jobWorkRunNodePo : insertRunNodeList) {
            jobWorkRunNodeMapper.insert(jobWorkRunNodePo);
        }
    }

    /**
     * 初始化运行作业
     * @param workId 作业id
     */
    private JobWorkRunPo initRunWork(String workId, Integer workType, Date turnDate) {
        String runNodeId = IdUtil.getSnowflakeNextIdStr();
        JobWorkRunPo jobRunWorkPo = new JobWorkRunPo();
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
        List<JobWorkRunPo> jobRunWorkPoList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getRunWorkStatus, RunWorkStatusEnum.COMPLETE.getCode(), RunWorkStatusEnum.FAIL.getCode())
                .lt(JobWorkRunPo::getCreateTime, DateUtil.offsetDay(new Date(), -30)));
        for (JobWorkRunPo jobRunWorkPo : jobRunWorkPoList) {
            jobWorkRunNodeMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));
            jobRunWorkMapper.deleteById(jobRunWorkPo);
        }
    }

    /**
     * 获取需要继续调度执行的运行作业
     */
    public List<JobWorkRunPo> getAllNeedRunWorkList() {
        return jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getRunWorkStatus, RunWorkStatusEnum.WAIT.getCode(), RunWorkStatusEnum.RUNNING.getCode())
                .orderByAsc(JobWorkRunPo::getCreateTime));
    }

    /**
     * 修改作业翻牌时间
     */
    public void updateWorkTurnDate() {
        List<JobWorkRunPo> jobRunWorkList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getRunWorkStatus, RunWorkStatusEnum.RUNNING.getCode(), RunWorkStatusEnum.WAIT.getCode()));
        for (JobWorkRunPo jobRunWorkPo : jobRunWorkList) {
            List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));

            if (CollUtil.isNotEmpty(jobWorkRunNodePos)) {
                DateTime offsetTurnDate = jobRunWorkPo.getTurnDate() == null ? null : DateUtil.offset(jobRunWorkPo.getTurnDate(), DateField.DAY_OF_MONTH, 1);
                long count = jobWorkRunNodePos.stream()
                        .filter(x -> {
                            boolean flag = x.getNodeRunStatus() == RunWorkStatusEnum.COMPLETE.getCode();
                            // 这里由于当作业类型为顺序类型时翻牌时间为空，不判断翻牌时间
                            if (flag && x.getTurnDate() != null) {
                                flag = DateUtil.compare(x.getTurnDate(), offsetTurnDate) == 0;
                            }
                            return flag;
                        })
                        .count();
                if (count == jobWorkRunNodePos.size()) {
                    JobWorkRunPo updateJobWork = new JobWorkRunPo()
                            .setRunWorkStatus(RunWorkStatusEnum.COMPLETE.getCode())
                            .setWorkId(jobRunWorkPo.getWorkId())
                            .setRunWorkId(jobRunWorkPo.getRunWorkId());
                    if (offsetTurnDate != null) {
                        updateJobWork.setTurnDate(offsetTurnDate);
                    }
                    jobRunWorkMapper.updateById(updateJobWork);
                }
            }
        }
    }

}
