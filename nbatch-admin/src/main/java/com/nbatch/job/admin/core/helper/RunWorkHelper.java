package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunPo;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobLockMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
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

    private static final String RUN_WORK_INIT_LOCK_PREFIX = "run_work_init_lock:";

    private final IJobWorkMapper jobWorkMapper;

    private final IJobLockMapper jobLockMapper;

    private final IJobWorkRunMapper jobRunWorkMapper;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    /**
     * 初始化运行作业
     *
     * @param workId 作业id
     */
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> initRunWork(String workId) {
        if (workId == null || workId.trim().isEmpty()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空.");
        }
        // 锁的生命周期是initRunWork，因为有Transactional注解所以发方法执行完毕后会
        lockRunWorkInit(workId);

        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        if (jobWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业不存在.");
        }
        // 看是否有没有完成运行中作业
        List<JobWorkRunPo> notCompleteJobRunWorkList = jobRunWorkMapper
                .selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                        .eq(JobWorkRunPo::getWorkId, jobWorkPo.getWorkId())
                        .orderByDesc(JobWorkRunPo::getCreateTime));
        JobWorkRunPo jobRunWorkPo = null;
        if (CollUtil.isEmpty(notCompleteJobRunWorkList)) {
            Date initTurnDate = jobWorkPo.getInitTurnDate() == null
                    ? DateUtil.parseDate(DateUtil.today())
                    : DateUtil.parseDate(DateUtil.formatDate(jobWorkPo.getInitTurnDate()));
            jobRunWorkPo = initRunWork(jobWorkPo.getWorkId(), jobWorkPo.getWorkType(), initTurnDate);
        } else {
            JobWorkRunPo lastJobRunWorkPo = notCompleteJobRunWorkList.get(0);
            // 如果运行状态为待执行或者进行中，则不处理
            if (lastJobRunWorkPo.getRunWorkStatus() == FlowRunStatusEnum.COMPLETE.getCode()) {
                // 当jobRunWorkPo.getTurnDate()为空时currentTurnDate为空，
                // DateUtil.compare(currentTurnDate, DateUtil.parseDate(DateUtil.today())) 为-1，所有顺序类型作业不需要增加判断
                DateTime currentTurnDate = DateUtil.offset(lastJobRunWorkPo.getTurnDate(), DateField.DAY_OF_MONTH, 1);
                if (DateUtil.compare(currentTurnDate, DateUtil.parseDate(DateUtil.today())) > 0) {
                    return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "今天的翻牌任务已经执行过了.");
                }
                jobRunWorkPo = initRunWork(lastJobRunWorkPo.getWorkId(), jobWorkPo.getWorkType(), currentTurnDate);
            }
        }
        if (jobRunWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "可执行的运行作业为空.");
        }
        List<JobWorkRunNodePo> insertRunNodeList = new ArrayList<>();
        List<JobWorkNodePo> jobWorkNodePos = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, jobRunWorkPo.getWorkId()));
        if (CollUtil.isEmpty(jobWorkNodePos)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "该运行作用没有运行节点.");
        }
        for (JobWorkNodePo elementNode : jobWorkNodePos) {
            JobWorkRunNodePo jobWorkRunNodePo = initRunNode(elementNode, jobRunWorkPo);
            insertRunNodeList.add(jobWorkRunNodePo);
        }
        jobRunWorkMapper.insert(jobRunWorkPo);
        for (JobWorkRunNodePo jobWorkRunNodePo : insertRunNodeList) {
            jobWorkRunNodeMapper.insert(jobWorkRunNodePo);
        }
        return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "job work started successfully.");
    }

    private void lockRunWorkInit(String workId) {
        String lockName = RUN_WORK_INIT_LOCK_PREFIX.concat(workId);
        jobLockMapper.insertIgnore(lockName);
        jobLockMapper.lockByName(lockName);
    }

    /**
     * 初始化运行作业
     *
     * @param workId 作业id
     */
    private JobWorkRunPo initRunWork(String workId, Integer workType, Date turnDate) {
        String runNodeId = IdUtil.getSnowflakeNextIdStr();
        JobWorkRunPo jobRunWorkPo = new JobWorkRunPo();
        jobRunWorkPo.setRunWorkId(runNodeId);
        jobRunWorkPo.setWorkId(workId);
        jobRunWorkPo.setRunWorkStatus(FlowRunStatusEnum.WAIT.getCode());
        if (workType == WorkTypeEnum.TYPE_TURN.getCode()) {
            jobRunWorkPo.setTurnDate(turnDate);
        }
        jobRunWorkPo.setWorkType(workType);
        jobRunWorkPo.setCreateTime(DateUtil.date());
        return jobRunWorkPo;
    }

    private JobWorkRunNodePo initRunNode(JobWorkNodePo elementNode,
                                         JobWorkRunPo jobRunWorkPo) {
        JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
        jobWorkRunNodePo.setRunWorkId(jobRunWorkPo.getRunWorkId());
        jobWorkRunNodePo.setWorkId(jobRunWorkPo.getWorkId());
        jobWorkRunNodePo.setNodeId(elementNode.getNodeId());
        jobWorkRunNodePo.setNodeRunStatus(FlowRunStatusEnum.WAIT.getCode());
        if (jobRunWorkPo.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode()) {
            jobWorkRunNodePo.setTurnDate(jobRunWorkPo.getTurnDate());
        }
        jobWorkRunNodePo.setCreateTime(DateUtil.date());
        // 错误策略
        jobWorkRunNodePo.setErrorStrategy(elementNode.getErrorStrategy());
        jobWorkRunNodePo.setRetryTimes(elementNode.getRetryTimes());
        return jobWorkRunNodePo;
    }

    /**
     * 删除运行作业(只保留30天的记录)
     */
    public void deleteRunWork() {
        List<JobWorkRunPo> jobRunWorkPoList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getRunWorkStatus, FlowRunStatusEnum.COMPLETE.getCode(), FlowRunStatusEnum.EXCEPTION.getCode())
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
                .in(JobWorkRunPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode(), FlowRunStatusEnum.RUNNING.getCode())
                .orderByAsc(JobWorkRunPo::getCreateTime));
    }

    /**
     * 修改作业翻牌时间
     */
    public void updateWorkTurnDate() {
        List<JobWorkRunPo> jobRunWorkList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getRunWorkStatus, FlowRunStatusEnum.RUNNING.getCode(), FlowRunStatusEnum.WAIT.getCode()));
        for (JobWorkRunPo jobRunWorkPo : jobRunWorkList) {
            List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));

            if (CollUtil.isNotEmpty(jobWorkRunNodePos)) {
                long count = jobWorkRunNodePos.stream()
                        .filter(x -> {
                            boolean flag = x.getNodeRunStatus() == FlowRunStatusEnum.COMPLETE.getCode();
                            // 这里由于当作业类型为顺序类型时翻牌时间为空，不判断翻牌时间
                            if (flag && x.getTurnDate() != null) {
                                flag = DateUtil.compare(x.getTurnDate(), jobRunWorkPo.getTurnDate()) == 0;
                            }
                            return flag;
                        })
                        .count();
                if (count == jobWorkRunNodePos.size()) {
                    JobWorkRunPo updateJobWork = new JobWorkRunPo()
                            .setRunWorkStatus(FlowRunStatusEnum.COMPLETE.getCode())
                            .setWorkId(jobRunWorkPo.getWorkId())
                            .setRunWorkId(jobRunWorkPo.getRunWorkId());
//                    if (offsetTurnDate != null) {
//                        updateJobWork.setTurnDate(offsetTurnDate);
//                    }
                    jobRunWorkMapper.updateById(updateJobWork);
                }
            }
        }
    }

}
