package com.nbatch.job.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.controller.annotation.PermissionLimit;
import com.nbatch.job.admin.controller.interceptor.PermissionInterceptor;
import com.nbatch.job.admin.core.domain.param.JobUserParam;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobUserPo;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.admin.mapper.IJobGroupMapper;
import com.nbatch.job.admin.mapper.IJobUserMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账号管理
 * @author Mr.ni
 */
@Controller
@RequestMapping("/user")
public class JobUserController {

    @Resource
    private IJobUserMapper jobUserMapper;
    @Resource
    private IJobGroupMapper jobGroupMapper;

    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model) {

        // 执行器列表
        List<JobGroupPo> groupList = jobGroupMapper.selectList(Wrappers.lambdaQuery(JobGroupPo.class)
                .orderByDesc(JobGroupPo::getAppName).orderByDesc(JobGroupPo::getTitle)
                .orderByAsc(JobGroupPo::getId));

        model.addAttribute("groupList", groupList);

        return "user/user.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String username, int role) {

        Page<JobUserPo> jobUserPoPage = jobUserMapper.selectPage(new Page<>(start, length), Wrappers.lambdaQuery(JobUserPo.class)
                .eq(StrUtil.isNotEmpty(username), JobUserPo::getUsername, username)
                .eq(role != -1, JobUserPo::getRole, role)
        );

        // filter
        if (CollUtil.isNotEmpty(jobUserPoPage.getRecords())) {
            for (JobUserPo item : jobUserPoPage.getRecords()) {
                item.setPassword(null);
            }
        }

        // package result
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", jobUserPoPage.getTotal());
        // 过滤后的总记录数
        maps.put("recordsFiltered", jobUserPoPage.getTotal());
        // 分页列表
        maps.put("data", jobUserPoPage.getRecords());

        return maps;
    }

    @RequestMapping("/add")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> add(JobUserParam param) {

        // valid username
        if (!StringUtils.hasText(param.getUsername())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_username"));
        }
        param.setUsername(param.getUsername().trim());
        if (!(param.getUsername().length() >= 4 && param.getUsername().length() <= 20)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }
        // valid password
        if (!StringUtils.hasText(param.getPassword())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_password"));
        }
        param.setPassword(param.getPassword().trim());
        if (!(param.getPassword().length() >= 4 && param.getPassword().length() <= 20)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }
        // md5 password
        param.setPassword(DigestUtils.md5DigestAsHex(param.getPassword().getBytes()));
        JobUserPo existUser = jobUserMapper.selectOne(Wrappers.lambdaQuery(JobUserPo.class)
                .eq(JobUserPo::getUsername, param.getUsername()));
        if (existUser != null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("user_username_repeat"));
        }

        // write
        jobUserMapper.insert(BeanUtil.toBean(param, JobUserPo.class));
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(HttpServletRequest request, JobUserParam param) {

        // avoid opt login seft
        JobUserPo loginUser = PermissionInterceptor.getLoginUser(request);
        if (loginUser.getUsername().equals(param.getUsername())) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        // valid password
        if (StringUtils.hasText(param.getPassword())) {
            param.setPassword(param.getPassword().trim());
            if (!(param.getPassword().length() >= 4 && param.getPassword().length() <= 20)) {
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
            }
            // md5 password
            param.setPassword(DigestUtils.md5DigestAsHex(param.getPassword().getBytes()));
        } else {
            param.setPassword(null);
        }

        // update
        jobUserMapper.updateById(BeanUtil.toBean(param, JobUserPo.class));
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(HttpServletRequest request, String id) {

        // avoid opt login seft
        JobUserPo loginUser = PermissionInterceptor.getLoginUser(request);
        if (StrUtil.equals(loginUser.getId(), id)) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        jobUserMapper.deleteById(id);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/updatePwd")
    @ResponseBody
    public ReturnT<String> updatePwd(HttpServletRequest request, String password, String oldPassword) {

        // valid
        if (StrUtil.isBlank(oldPassword)) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
        }
        if (StrUtil.isBlank(password)) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
        }
        password = password.trim();
        if (!(password.length() >= 4 && password.length() <= 20)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }

        // md5 password
        String md5OldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // valid old pwd
        JobUserPo loginUser = PermissionInterceptor.getLoginUser(request);
        //WHERE t.username = #{username}
        JobUserPo existUser = jobUserMapper.selectOne(Wrappers.lambdaQuery(JobUserPo.class)
                .eq(JobUserPo::getUsername, loginUser.getUsername()));


        if (!md5OldPassword.equals(existUser.getPassword())) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("change_pwd_field_oldpwd") + I18nUtil.getString("system_unvalid"));
        }

        // write new
        existUser.setPassword(md5Password);
        jobUserMapper.updateById(existUser);

        return ReturnT.SUCCESS;
    }

}
