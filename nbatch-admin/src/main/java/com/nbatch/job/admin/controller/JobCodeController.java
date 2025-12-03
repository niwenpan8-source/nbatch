package com.nbatch.job.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.controller.interceptor.PermissionInterceptor;
import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.domain.po.JobLoggluePo;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.admin.mapper.IJobInfoMapper;
import com.nbatch.job.admin.mapper.IJobLogglueMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.glue.GlueTypeEnum;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * job code controller
 * @author Mr.ni
 */
@Controller
@RequestMapping("/jobcode")
public class JobCodeController {

	@Resource
	private IJobInfoMapper jobInfoMapper;
	@Resource
	private IJobLogglueMapper jobLogglueMapper;

	@RequestMapping
	public String index(HttpServletRequest request, Model model, String jobId) {
		JobInfoPo jobInfo = jobInfoMapper.selectById(jobId);
		List<JobLoggluePo> jobLogGlues = jobLogglueMapper.selectList(Wrappers.lambdaQuery(JobLoggluePo.class)
				.eq(JobLoggluePo::getJobId, jobId).orderByDesc(JobLoggluePo::getId));

		if (jobInfo == null) {
			throw new RuntimeException(I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
		}
		if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType())) {
			throw new RuntimeException(I18nUtil.getString("jobinfo_glue_gluetype_unvalid"));
		}

		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

		// Glue类型-字典
		model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());

		model.addAttribute("jobInfo", jobInfo);
		model.addAttribute("jobLogGlues", jobLogGlues);
		return "jobcode/jobcode.index";
	}
	
	@RequestMapping("/save")
	@ResponseBody
	public ReturnT<String> save(HttpServletRequest request, String id, String glueSource, String glueRemark) {
		// valid
		if (glueRemark==null) {
			return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_remark")));
		}
		if (glueRemark.length()<4 || glueRemark.length()>100) {
			return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_remark_limit"));
		}
		JobInfoPo existsJobInfo = jobInfoMapper.selectById(id);
		if (existsJobInfo == null) {
			return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
		}

		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, existsJobInfo.getJobGroup());
		
		// update new code
		existsJobInfo.setGlueSource(glueSource);
		existsJobInfo.setGlueRemark(glueRemark);
		existsJobInfo.setGlueUpdatetime(new Date());

		existsJobInfo.setUpdateTime(new Date());
		jobInfoMapper.updateById(existsJobInfo);

		// log old code
		JobLoggluePo jobLoggluePo = new JobLoggluePo();
		jobLoggluePo.setJobId(existsJobInfo.getId());
		jobLoggluePo.setGlueType(existsJobInfo.getGlueType());
		jobLoggluePo.setGlueSource(glueSource);
		jobLoggluePo.setGlueRemark(glueRemark);

		jobLoggluePo.setAddTime(new Date());
		jobLoggluePo.setUpdateTime(new Date());
		jobLogglueMapper.insert(jobLoggluePo);

		Page<JobLoggluePo> jobLoggluePoPage = jobLogglueMapper.selectPage(new Page<>(1, 30),
				Wrappers.lambdaQuery(JobLoggluePo.class).eq(JobLoggluePo::getJobId, existsJobInfo.getId()));
		if (jobLoggluePoPage != null && jobLoggluePoPage.getTotal() > 0) {
			List<String> jobLogglueIds = jobLoggluePoPage.getRecords().stream()
					.map(JobLoggluePo::getId).collect(Collectors.toList());
			jobLogglueMapper.delete(Wrappers.lambdaQuery(JobLoggluePo.class)
					.in(JobLoggluePo::getId, jobLogglueIds));
		}

		return ReturnT.SUCCESS;
	}
	
}
