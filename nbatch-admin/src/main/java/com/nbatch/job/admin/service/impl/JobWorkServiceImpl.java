package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobRunWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.admin.mapper.IJobRunWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.service.IJobWorkService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.hutool.core.date.DatePattern.NORM_DATE_FORMATTER;

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
    private IJobRunWorkMapper jobRunWorkMapper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkPageParam param) {
        Page<JobWorkPo> page = jobWorkMapper.selectPage(new Page<>(param.getStart(), param.getLength()),
                Wrappers.lambdaQuery(JobWorkPo.class));
        List<JobWorkPo> records = page.getRecords();
        List<String> workIdList = records.stream().map(JobWorkPo::getWorkId).collect(Collectors.toList());
        List<JobRunWorkPo> jobRunWorkPoList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobRunWorkPo.class)
                .in(JobRunWorkPo::getWorkId, workIdList)
                .orderByDesc(JobRunWorkPo::getCreateTime));

        Map<String, JobRunWorkPo> jobRunWorkPoMap = jobRunWorkPoList.stream()
                .collect(Collectors.toMap(JobRunWorkPo::getWorkId, jobRunWorkPo -> jobRunWorkPo, (old, v) -> old));

        List<JobWorkVo> jobWorkVoList = records.stream().map(x -> {
            JobRunWorkPo jobRunWorkPo = jobRunWorkPoMap.get(x.getWorkId());
            JobWorkVo jobWorkVo = BeanUtil.toBean(x, JobWorkVo.class);
            if (jobRunWorkPo != null) {
                if (jobRunWorkPo.getTurnDate() != null) {
                    jobWorkVo.setTurnDate(DateUtil.format(jobRunWorkPo.getTurnDate(), NORM_DATE_FORMATTER));
                }
                jobWorkVo.setRunWorkStatus(jobRunWorkPo.getRunWorkStatus());
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
