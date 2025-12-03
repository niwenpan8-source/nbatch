package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobRunWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.enums.RunWorkStatusEnum;
import com.nbatch.job.admin.core.enums.WorkStatusEnum;
import com.nbatch.job.admin.core.enums.WorkTypeEnum;
import com.nbatch.job.admin.mapper.IJobRunWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void initRunWork() {

        List<JobWorkPo> jobWorkPos = jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class)
                .eq(JobWorkPo::getWorkStatus, WorkStatusEnum.START.getCode()));
        List<JobRunWorkPo> insertRunWorkList = new ArrayList<>();
        for (JobWorkPo jobWorkPo : jobWorkPos) {
            List<JobRunWorkPo> jobRunWorkList = jobRunWorkMapper
                    .selectList(Wrappers.lambdaQuery(JobRunWorkPo.class)
                    .eq(JobRunWorkPo::getWorkId, jobWorkPo.getWorkId())
                    .orderByDesc(JobRunWorkPo::getCreateTime));
            if (CollUtil.isEmpty(jobRunWorkList)) {
                JobRunWorkPo jobRunWorkPo = initRunWork(jobWorkPo.getWorkId(), jobWorkPo.getWorkType(), DateUtil.parseDate(DateUtil.today()));
                insertRunWorkList.add(jobRunWorkPo);
                continue;
            }
            JobRunWorkPo jobRunWorkPo = jobRunWorkList.get(0);
            // 如果运行状态为待执行或者进行中，则不处理
            if (jobRunWorkPo.getRunWorkStatus() == RunWorkStatusEnum.COMPLETE.getCode()) {
                JobRunWorkPo copyJobRunWorkPo = initRunWork(jobRunWorkPo.getWorkId(), jobWorkPo.getWorkType(), jobRunWorkPo.getTurnDate());
                insertRunWorkList.add(copyJobRunWorkPo);
            }
        }
        if (CollUtil.isEmpty(insertRunWorkList)) {
            return;
        }
        List<JobWorkRunNodePo> insertRunNodeList = new ArrayList<>();
        List<String> runWorkIdList = insertRunWorkList.stream().map(JobRunWorkPo::getWorkId).collect(Collectors.toList());
        List<JobWorkNodePo> jobWorkNodePos = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .in(JobWorkNodePo::getWorkId, runWorkIdList));
        Map<String, List<JobWorkNodePo>> jobWorkNodePoMap = jobWorkNodePos.stream()
                .collect(Collectors.groupingBy(JobWorkNodePo::getWorkId));
        Iterator<JobRunWorkPo> iterator = insertRunWorkList.iterator();
        while (iterator.hasNext()) {
            JobRunWorkPo element = iterator.next();
            List<JobWorkNodePo> elementNodeList = jobWorkNodePoMap.get(element.getWorkId());
            if (CollUtil.isEmpty(elementNodeList)) {
                // 安全地移除当前元素
                iterator.remove();
                continue;
            }
            for (JobWorkNodePo elementNode : elementNodeList) {
                JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
                jobWorkRunNodePo.setRunWorkId(element.getRunWorkId());
                jobWorkRunNodePo.setNodeId(elementNode.getNodeId());
                jobWorkRunNodePo.setNodeRunStatus(RunWorkStatusEnum.WAIT.getCode());
                if (element.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode()) {
                    jobWorkRunNodePo.setTurnDate(element.getTurnDate());
                }
                jobWorkRunNodePo.setCreateTime(DateUtil.date());
                insertRunNodeList.add(jobWorkRunNodePo);
            }
        }
        for (JobWorkRunNodePo jobWorkRunNodePo : insertRunNodeList) {
            jobWorkRunNodeMapper.insert(jobWorkRunNodePo);
        }
        for (JobRunWorkPo jobRunWorkPo : insertRunWorkList) {
            jobRunWorkMapper.insert(jobRunWorkPo);
        }
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
            jobRunWorkPo.setTurnDate(DateUtil.offset(turnDate, DateField.DAY_OF_MONTH, 1));
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

}
