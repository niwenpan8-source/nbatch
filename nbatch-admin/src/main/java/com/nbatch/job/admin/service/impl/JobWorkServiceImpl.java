package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobWorkRunPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.service.IJobWorkService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.FlowStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description: 作业执行服务实现类
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Service
public class JobWorkServiceImpl implements IJobWorkService {

    @Resource
    private IJobWorkMapper jobWorkMapper;

    @Resource
    private IJobWorkRunMapper jobRunWorkMapper;

    @Resource
    private IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkPageParam param) {
        Page<JobWorkPo> page = jobWorkMapper.selectPage(
                new Page<>((param.getStart() / param.getLength()) + 1, param.getLength()),
                Wrappers.lambdaQuery(JobWorkPo.class)
                        .eq(param.getWorkStatus() != null, JobWorkPo::getWorkStatus, param.getWorkStatus()));
        List<JobWorkPo> records = page.getRecords();
        List<String> workIdList = records.stream().map(JobWorkPo::getWorkId).collect(Collectors.toList());
        List<JobWorkRunPo> jobRunWorkPoList = workIdList.isEmpty() ? Collections.emptyList()
                : jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getWorkId, workIdList)
                .orderByDesc(JobWorkRunPo::getCreateTime));

        Map<String, JobWorkRunPo> jobRunWorkPoMap = jobRunWorkPoList.stream()
                .collect(Collectors.toMap(JobWorkRunPo::getWorkId, jobRunWorkPo -> jobRunWorkPo, (old, v) -> old));

        List<JobWorkVo> jobWorkVoList = records.stream().map(x -> {
            JobWorkRunPo jobRunWorkPo = jobRunWorkPoMap.get(x.getWorkId());
            JobWorkVo jobWorkVo = BeanUtil.toBean(x, JobWorkVo.class);
            jobWorkVo.setWorkStatusName(getFlowStatusName(x.getWorkStatus()));
            jobWorkVo.setWorkTypeName(getWorkTypeName(x.getWorkType()));
            if (jobRunWorkPo != null) {
                jobWorkVo.setRunWorkId(jobRunWorkPo.getRunWorkId());
                if (jobRunWorkPo.getTurnDate() != null) {
                    jobWorkVo.setTurnDate(DateUtil.formatDateTime(jobRunWorkPo.getTurnDate()));
                }
                if (jobRunWorkPo.getCreateTime() != null) {
                    jobWorkVo.setRunWorkCreateTime(DateUtil.formatDateTime(jobRunWorkPo.getCreateTime()));
                }
                jobWorkVo.setRunWorkStatus(jobRunWorkPo.getRunWorkStatus());
                jobWorkVo.setRunWorkStatusName(FlowRunStatusEnum.getValueByCode(jobRunWorkPo.getRunWorkStatus()));
            }
            return jobWorkVo;
        }).collect(Collectors.toList());
        // package result
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", page.getTotal());
        // 过滤后的总记录数
        maps.put("recordsFiltered", page.getTotal());
        // 分页列表
        maps.put("data", jobWorkVoList);
        return maps;
    }

    private String getFlowStatusName(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlowStatusEnum value : FlowStatusEnum.values()) {
            if (value.getCode().equals(code)) {
                return value.getValue();
            }
        }
        return null;
    }

    private String getWorkTypeName(Integer code) {
        if (code == null) {
            return null;
        }
        for (WorkTypeEnum value : WorkTypeEnum.values()) {
            if (value.getCode() == code) {
                return value.getValue();
            }
        }
        return null;
    }

    /**
     * 获取作业列表
     */
    @Override
    public List<JobWorkPo> getWorkList() {
        return jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class));
    }

    /**
     * 插入
     */
    @Override
    public int insert(JobWorkParam param) {
        return jobWorkMapper.insert(BeanUtil.toBean(param, JobWorkPo.class));
    }

    /**
     * 修改
     */
    @Override
    public int update(JobWorkParam param) {
        return jobWorkMapper.updateById(BeanUtil.toBean(param, JobWorkPo.class));
    }

    /**
     * 通过得到id得到对象
     */
    @Override
    public JobWorkVo getModel(String id) {
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(id);
        if (jobWorkPo == null) {
            return null;
        }
        return BeanUtil.toBean(jobWorkPo, JobWorkVo.class);
    }

    /**
     * 删除
     */
    @Override
    public int delete(String id) {
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(id);
        if (jobWorkPo == null) {
            return 1;
        }
        return jobWorkMapper.deleteById(id);
    }

    @Override
    public ReturnT<String> recoverRunWork(String runWorkId) {
        if (runWorkId == null || runWorkId.trim().isEmpty()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行作业ID不能为空");
        }
        JobWorkRunPo oldRunWorkPo = jobRunWorkMapper.selectById(runWorkId);
        if (oldRunWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行作业不存在");
        }
        if (oldRunWorkPo.getRunWorkStatus() != null
                && oldRunWorkPo.getRunWorkStatus() != FlowRunStatusEnum.RUNNING.getCode()
                && oldRunWorkPo.getRunWorkStatus() != FlowRunStatusEnum.EXCEPTION.getCode()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "只有进行中或异常的运行作业可以恢复重跑");
        }

        // 恢复重跑只处理异常/卡住的节点，已完成节点保持完成状态，避免退化成整批重跑。
        int resetCount = jobWorkRunNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodePo.class)
                .set(JobWorkRunNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                .set(JobWorkRunNodePo::getStartTime, null)
                .set(JobWorkRunNodePo::getEndTime, null)
                .eq(JobWorkRunNodePo::getRunWorkId, runWorkId)
                .in(JobWorkRunNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.EXCEPTION.getCode(),
                        FlowRunStatusEnum.RUNNING.getCode()));
        if (resetCount <= 0) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "当前运行作业没有异常或失败节点可恢复");
        }

        jobRunWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunPo.class)
                .set(JobWorkRunPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode())
                .eq(JobWorkRunPo::getRunWorkId, runWorkId));
        return ReturnT.SUCCESS;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> rerunLatestRunWork(String workId) {
        if (workId == null || workId.trim().isEmpty()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空");
        }
        // 一键重跑以页面展示的最新运行作业为准，防止重置历史运行批次。
        JobWorkRunPo latestRunWorkPo = jobRunWorkMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .eq(JobWorkRunPo::getWorkId, workId)
                .orderByDesc(JobWorkRunPo::getCreateTime)
                .orderByDesc(JobWorkRunPo::getRunWorkId)
                .last("LIMIT 1"));
        if (latestRunWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "暂无运行作业记录");
        }

        // 一键重跑重置最新运行批次的所有节点，让该作业完整重新执行。
        jobRunWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunPo.class)
                .set(JobWorkRunPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode())
                .eq(JobWorkRunPo::getRunWorkId, latestRunWorkPo.getRunWorkId()));

        jobWorkRunNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodePo.class)
                .set(JobWorkRunNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                .set(JobWorkRunNodePo::getTurnDate, latestRunWorkPo.getTurnDate())
                .set(JobWorkRunNodePo::getStartTime, null)
                .set(JobWorkRunNodePo::getEndTime, null)
                .eq(JobWorkRunNodePo::getRunWorkId, latestRunWorkPo.getRunWorkId())
                .ne(JobWorkRunNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode()));

        return ReturnT.SUCCESS;
    }


}
