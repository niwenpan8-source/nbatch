package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobWorkRunPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.admin.mapper.IJobWorkRunMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.service.IJobWorkService;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.FlowStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import org.springframework.stereotype.Service;

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
                    jobWorkVo.setTurnDate(DateUtil.formatDate(jobRunWorkPo.getTurnDate()));
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


}
