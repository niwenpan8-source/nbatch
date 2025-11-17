package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.service.IJobWorkService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: 作业执行服务实现类
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Service
public class JobWorkServiceImpl implements IJobWorkService {

    @Resource
    private IJobWorkMapper jobWorkMapper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkPageParam param) {
        Page<JobWorkPo> page = jobWorkMapper.selectPage(new Page<>(param.getStart(), param.getLength()),
                Wrappers.lambdaQuery(JobWorkPo.class));
        // package result
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", page.getTotal());
        // 过滤后的总记录数
        maps.put("recordsFiltered", page.getTotal());
        // 分页列表
        maps.put("data", page.getRecords());
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
