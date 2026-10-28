package com.nbatch.job.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobUserPo;
import com.nbatch.job.admin.core.util.CookieUtil;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.admin.core.util.JacksonUtil;
import com.nbatch.job.admin.mapper.IJobUserMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * 登录service
 */
@Service
public class LoginService {

    public static final String LOGIN_IDENTITY_KEY = "NBATCH_JOB_LOGIN_IDENTITY";

    @Resource
    private IJobUserMapper jobUserMapper;


    // ---------------------- token tool ----------------------

    private String makeToken(JobUserPo jobUser){
        String tokenJson = JacksonUtil.writeValueAsString(jobUser);
        assert tokenJson != null;
        return new BigInteger(tokenJson.getBytes()).toString(16);
    }
    private JobUserPo parseToken(String tokenHex){
        JobUserPo jobUserPo = null;
        if (tokenHex != null) {
            // 用户名和密码（MD5）
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());
            jobUserPo = JacksonUtil.readValue(tokenJson, JobUserPo.class);
        }
        return jobUserPo;
    }


    // ---------------------- login tool, with cookie and db ----------------------

    public ReturnT<String> login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean ifRemember){

        // 校验参数
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)){
            return new ReturnT<>(500, I18nUtil.getString("login_param_empty"));
        }

        // 校验密码
        JobUserPo jobUser = jobUserMapper.selectOne(Wrappers.lambdaQuery(JobUserPo.class)
                .eq(JobUserPo::getUsername, username));
        if (jobUser == null) {
            return new ReturnT<>(500, I18nUtil.getString("login_param_unvalid"));
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordMd5.equals(jobUser.getPassword())) {
            return new ReturnT<>(500, I18nUtil.getString("login_param_unvalid"));
        }

        String loginToken = makeToken(jobUser);

        // 执行登录
        CookieUtil.set(response, LOGIN_IDENTITY_KEY, loginToken, ifRemember);
        return ReturnT.SUCCESS;
    }

    /**
     * 退出登录
     *
     * @param request request
     * @param response response
     */
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response){
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return ReturnT.SUCCESS;
    }

    /**
     * 退出登录
     *
     * @param request request
     */
    public JobUserPo ifLogin(HttpServletRequest request, HttpServletResponse response){
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
        if (cookieToken != null) {
            JobUserPo cookieUser = null;
            try {
                cookieUser = parseToken(cookieToken);
            } catch (Exception e) {
                logout(request, response);
            }
            if (cookieUser != null) {
                JobUserPo dbUser = jobUserMapper.selectOne(Wrappers.lambdaQuery(JobUserPo.class)
                        .eq(JobUserPo::getUsername, cookieUser.getUsername()));
                if (dbUser != null) {
                    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
                        return dbUser;
                    }
                }
            }
        }
        return null;
    }


}
