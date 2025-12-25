package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeLogPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodePageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeRelationParam;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodeRelationPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogDetailPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeRelationVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeLogVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeVo;
import com.nbatch.job.admin.core.enums.NodeTypeEnum;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeRelationMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogDetailMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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

    @Resource
    private IJobWorkNodeRelationMapper jobWorkNodeRelationMapper;

    @Resource
    private IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;

    @Resource
    private IJobWorkRunNodeLogDetailMapper jobWorkRunNodeLogDetailMapper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkNodePageParam param) {
        Page<JobWorkNodePo> page = jobWorkNodeMapper.selectPage(new Page<>((param.getStart() / param.getLength()) + 1, param.getLength()),
                Wrappers.lambdaQuery(JobWorkNodePo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkNodePo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeType()), JobWorkNodePo::getNodeType, param.getNodeType()));

        List<JobWorkPo> jobWorkPos = jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class));
        Map<String, String> workMap = new HashMap<>();
        if (CollUtil.isNotEmpty(jobWorkPos)) {
            workMap = jobWorkPos.stream()
                    .collect(Collectors.toMap(JobWorkPo::getWorkId, JobWorkPo::getWorkName));
        }
        // package result
        Map<String, String> finalWorkMap = workMap;
        page.convert(jobWorkNodePo -> {
            JobWorkRunNodeVo jobWorkNodeVo = BeanUtil.toBean(jobWorkNodePo, JobWorkRunNodeVo.class);
            jobWorkNodeVo.setNodeTypeName(NodeTypeEnum.getValue(jobWorkNodePo.getNodeType()));
            jobWorkNodeVo.setWorkName(finalWorkMap.get(jobWorkNodePo.getWorkId()));
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
        param.setUpdateTime(DateUtil.date());
        return jobWorkNodeMapper.insert(BeanUtil.toBean(param, JobWorkNodePo.class));
    }

    /**
     * 修改
     */
    @Override
    public int update(JobWorkNodeParam param) {
        param.setUpdateTime(DateUtil.date());
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
    public List<JobWorkNodePo> getWorkNodeList(String workId) {
        return jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getNodeStatus, 1).eq(JobWorkNodePo::getWorkId, workId));
    }

    /**
     * 获取所有启用的作业
     */
    @Override
    public List<JobWorkPo> getAllWorkList() {
        return jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class));

    }

    /**
     * 获得所有作业节点关系
     */
    @Override
    public List<JobWorkNodeRelationVo> getWorkNodeRelationByWorkId(String workId) {
        List<JobWorkNodeRelationPo> relationList = jobWorkNodeRelationMapper.selectList(Wrappers.lambdaQuery(JobWorkNodeRelationPo.class)
                .eq(JobWorkNodeRelationPo::getWorkId, workId)
                .orderByDesc(JobWorkNodeRelationPo::getNodeId1));
        List<JobWorkNodePo> jobWorkNodePos = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId));
        Map<String, String> nodeMap = new HashMap<>();
        if (CollUtil.isNotEmpty(jobWorkNodePos)) {
            nodeMap = jobWorkNodePos.stream()
                    .collect(Collectors.toMap(JobWorkNodePo::getNodeId, JobWorkNodePo::getNodeName));
        }
        Map<String, String> finalNodeMap = nodeMap;
        return relationList.stream().map(x -> {
            JobWorkNodeRelationVo bean = BeanUtil.toBean(x, JobWorkNodeRelationVo.class);
            bean.setNodeName1(finalNodeMap.get(x.getNodeId1()));
            bean.setNodeName2(finalNodeMap.get(x.getNodeId2()));
            return bean;
        }).collect(Collectors.toList());
    }

    /**
     * 批量插入作业节点关系
     */
    @Override
    public int updateWorkNodeRelation(JobWorkNodeRelationParam param) {
        int insertCount = 0;
        jobWorkNodeRelationMapper.delete(Wrappers.lambdaQuery(JobWorkNodeRelationPo.class)
                .eq(JobWorkNodeRelationPo::getWorkId, param.getWorkId()));
        for (JobWorkNodeRelationParam.NodeRelation relation : param.getNodeRelationList()) {
            JobWorkNodeRelationPo bean = BeanUtil.toBean(relation, JobWorkNodeRelationPo.class);
            bean.setWorkId(param.getWorkId());
            insertCount += jobWorkNodeRelationMapper.insert(bean);
        }
        return insertCount;
    }


    /**
     * 获取作业节点关系
     */
    @Override
    public IPage<JobWorkRunNodeLogVo> logPageList(JobWorkNodeLogPageParam param) {
        IPage<JobWorkRunNodeLogPo> page = jobWorkRunNodeLogMapper
                .selectPage(new Page<>(param.getStart(), param.getLength()),
                Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkRunNodeLogPo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeId()), JobWorkRunNodeLogPo::getNodeId, param.getNodeId())
                        .ge(param.getStartTime() != null, JobWorkRunNodeLogPo::getCreateTime, param.getStartTime())
                        .le(param.getEndTime() != null, JobWorkRunNodeLogPo::getCreateTime, param.getEndTime())
                );
        if (CollUtil.isEmpty(page.getRecords())) {
            return null;
        }
        List<String> runNodeIdList = page.getRecords().stream().map(JobWorkRunNodeLogPo::getRunNodeId)
                .collect(Collectors.toList());
        List<JobWorkRunNodeLogDetailPo> jobWorkRunNodeLogDetailList =
                jobWorkRunNodeLogDetailMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                .in(JobWorkRunNodeLogDetailPo::getRunNodeId, runNodeIdList));
        Map<String, List<JobWorkRunNodeLogDetailPo>> detailMap = new HashMap<>();
        if (CollUtil.isNotEmpty(jobWorkRunNodeLogDetailList)) {
            detailMap = jobWorkRunNodeLogDetailList.stream()
                    .collect(Collectors.groupingBy(JobWorkRunNodeLogDetailPo::getRunNodeId));
        }
        Map<String, List<JobWorkRunNodeLogDetailPo>> finalDetailMap = detailMap;
        return page.convert(x -> {
            JobWorkRunNodeLogVo vo = BeanUtil.toBean(x, JobWorkRunNodeLogVo.class);
            List<JobWorkRunNodeLogDetailPo> jobWorkRunNodeLogDetailPos = finalDetailMap.get(x.getRunNodeId());
            if (CollUtil.isNotEmpty(jobWorkRunNodeLogDetailPos)) {
                String jobDetail = jobWorkRunNodeLogDetailPos.stream().map(JobWorkRunNodeLogDetailPo::getHandleMsg)
                        .collect(Collectors.joining("<br>"));
                vo.setLogDetail(jobDetail);
            }
            return vo;
        });
    }

    /**
     * 获取作业节点
     */
    @Override
    public JobWorkNodePo getWorkNode(String workNodeId) {
        return jobWorkNodeMapper.selectById(workNodeId);
    }


}
