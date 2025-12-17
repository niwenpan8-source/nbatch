package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.domain.param.JobWorkNodePageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeParam;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeTypeVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeVo;
import com.nbatch.job.admin.core.enums.NodeTypeEnum;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description: 作业节点执行服务实现类
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Service
public class JobWorkNodeServiceImpl implements IJobWorkNodeService {

    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;

    @Resource
    private IJobWorkMapper jobWorkMapper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkNodePageParam param) {
        Page<JobWorkNodePo> page = jobWorkNodeMapper.selectPage(new Page<>((param.getStart() / param.getLength()) + 1, param.getLength()),
                Wrappers.lambdaQuery(JobWorkNodePo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkNodePo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeType()), JobWorkNodePo::getNodeType, param.getNodeType()));
        // package result
        page.convert(jobWorkNodePo -> {
            JobWorkRunNodeVo jobWorkNodeVo = BeanUtil.toBean(jobWorkNodePo, JobWorkRunNodeVo.class);
            jobWorkNodeVo.setNodeTypeName(NodeTypeEnum.getValue(jobWorkNodePo.getNodeType()));
            return jobWorkNodeVo;
        });
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

    /**
     * 得到所有的发布的
     */
    @Override
    public List<JobWorkNodePo> getWorkNode(String workId) {
        return jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getNodeStatus, 1).eq(JobWorkNodePo::getWorkId, workId));
    }

    /**
     * 获取所有启用的作业
     */
    @Override
    public List<JobWorkPo> getAllEnableWorkList() {
        return jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class)
                .eq(JobWorkPo::getWorkStatus, 1));

    }


}
