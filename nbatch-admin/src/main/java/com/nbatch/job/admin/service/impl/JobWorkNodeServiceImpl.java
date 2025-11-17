package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.domain.param.JobWorkNodePageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeParam;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: 作业节点执行服务实现类
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Service
public class JobWorkNodeServiceImpl implements IJobWorkNodeService {

    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkNodePageParam param) {
        Page<JobWorkNodePo> page = jobWorkNodeMapper.selectPage(new Page<>(param.getStart(), param.getLength()),
                Wrappers.lambdaQuery(JobWorkNodePo.class));
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
    public int insert(JobWorkNodeParam param) {
        return jobWorkNodeMapper.insert(BeanUtil.toBean(param, JobWorkNodePo.class));
    }

    /**
     * 修改
     */
    @Override
    public int update(JobWorkNodeParam param) {
        return jobWorkNodeMapper.updateById(BeanUtil.toBean(param, JobWorkNodePo.class));
    }

    /**
     * 通过得到id得到对象
     */
    @Override
    public JobWorkNodeVo getModel(String id) {
        JobWorkNodePo jobWorkNodePo = jobWorkNodeMapper.selectById(id);
        if (jobWorkNodePo == null) {
            return null;
        }
        return BeanUtil.toBean(jobWorkNodePo, JobWorkNodeVo.class);
    }

    /**
     * 删除
     */
    @Override
    public int delete(String id) {
        JobWorkNodePo jobWorkPo = jobWorkNodeMapper.selectById(id);
        if (jobWorkPo == null) {
            return 1;
        }
        return jobWorkNodeMapper.deleteById(id);
    }


}
