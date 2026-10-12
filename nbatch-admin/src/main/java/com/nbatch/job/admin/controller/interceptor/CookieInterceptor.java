package com.nbatch.job.admin.controller.interceptor;

import com.nbatch.job.admin.core.util.FtlUtil;
import com.nbatch.job.admin.core.util.I18nUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * 将 Cookie 以 cookieMap 形式写入页面模型
 *
 * @author Mr.ni
 */
@Component
public class CookieInterceptor implements AsyncHandlerInterceptor {

	@Override
	public void postHandle(HttpServletRequest request,
						   HttpServletResponse response,
						   Object handler,
						   ModelAndView modelAndView) {

		// 读取 Cookie
		if (modelAndView!=null && request.getCookies()!=null && request.getCookies().length>0) {
			HashMap<String, Cookie> cookieMap = new HashMap<>();
			for (Cookie ck : request.getCookies()) {
				cookieMap.put(ck.getName(), ck);
			}
			modelAndView.addObject("cookieMap", cookieMap);
		}

		// 静态方法
		if (modelAndView != null) {
			modelAndView.addObject("I18nUtil", FtlUtil.generateStaticModel(I18nUtil.class.getName()));
		}

	}
	
}
