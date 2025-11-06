package com.nbatch.job.admin.controller.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.controller.annotation.PermissionLimit;
import com.nbatch.job.admin.core.model.XxlJobGroup;
import com.nbatch.job.admin.core.model.XxlJobUser;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.admin.service.impl.LoginService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 权限拦截
 *
 * @author Mr.ni 2015-12-12 18:09:04
 */
@Component
public class PermissionInterceptor implements AsyncHandlerInterceptor {

	@Resource
	private LoginService loginService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		
		if (!(handler instanceof HandlerMethod)) {
			// proceed with the next interceptor
			return true;
		}

		// if need login
		boolean needLogin = true;
		boolean needAdminuser = false;
		HandlerMethod method = (HandlerMethod)handler;
		PermissionLimit permission = method.getMethodAnnotation(PermissionLimit.class);
		if (permission!=null) {
			needLogin = permission.limit();
			needAdminuser = permission.adminuser();
		}

		if (needLogin) {
			XxlJobUser loginUser = loginService.ifLogin(request, response);
			if (loginUser == null) {
				response.setStatus(302);
				response.setHeader("location", request.getContextPath()+"/toLogin");
				return false;
			}
			if (needAdminuser && loginUser.getRole()!=1) {
				throw new RuntimeException(I18nUtil.getString("system_permission_limit"));
			}
			// set loginUser, with request
			request.setAttribute(LoginService.LOGIN_IDENTITY_KEY, loginUser);
		}
		// proceed with the next interceptor
		return true;
	}


	// -------------------- permission tool --------------------

	/**
	 * get loginUser
	 *
	 * @param request 请求
	 */
	public static XxlJobUser getLoginUser(HttpServletRequest request){
        // get loginUser, with request
        return (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
	}

	/**
	 * valid permission by JobGroup
	 *
	 * @param request 请求
	 * @param jobGroup 群组ID
	 */
	public static void validJobGroupPermission(HttpServletRequest request, int jobGroup) {
		XxlJobUser loginUser = getLoginUser(request);
		if (!loginUser.validPermission(jobGroup)) {
			throw new RuntimeException(I18nUtil.getString("system_permission_limit") + "[username="+ loginUser.getUsername() +"]");
		}
	}

	/**
	 * filter XxlJobGroup by role
	 *
	 * @param request 请求
	 * @param jobGroupListAll 所有群组
	 */
	public static List<XxlJobGroup> filterJobGroupByRole(HttpServletRequest request, List<XxlJobGroup> jobGroupListAll){
		List<XxlJobGroup> jobGroupList = new ArrayList<>();
		if (CollUtil.isNotEmpty(jobGroupListAll)) {
			XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
			if (loginUser.getRole() == 1) {
				jobGroupList = jobGroupListAll;
			} else {
				List<String> groupIdStrs = new ArrayList<>();
				if (StrUtil.isNotBlank(loginUser.getPermission())) {
					groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(StrPool.COMMA));
				}
				for (XxlJobGroup groupItem : jobGroupListAll) {
					if (groupIdStrs.contains(String.valueOf(groupItem.getId()))) {
						jobGroupList.add(groupItem);
					}
				}
			}
		}
		return jobGroupList;
	}

	
}
