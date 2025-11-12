package com.nbatch.job.admin.core.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Cookie.Util
 *
 * @author Mr.ni 2015-12-12 18:01:06
 */
public class CookieUtil {

	// 默认缓存时间,单位/秒, 2H
	private static final int COOKIE_MAX_AGE = Integer.MAX_VALUE;
	// 保存路径,根路径
	private static final String COOKIE_PATH = "/";
	
	/**
	 * 保存
	 *
	 * @param response 响应
	 * @param key 键
	 * @param value 值
	 * @param ifRemember 是否记住
	 */
	public static void set(HttpServletResponse response, String key, String value, boolean ifRemember) {
		int age = ifRemember?COOKIE_MAX_AGE:-1;
		set(response, key, value, null, COOKIE_PATH, age, true);
	}

	/**
	 * 保存
	 *
	 * @param response 响应
	 * @param key 键
	 * @param value 值
	 * @param maxAge 最大年龄
	 */
	private static void set(HttpServletResponse response, String key, String value, String domain, String path, int maxAge, boolean isHttpOnly) {
		Cookie cookie = new Cookie(key, value);
		if (StrUtil.isNotEmpty(domain)) {
			cookie.setDomain(domain);
		}
		cookie.setPath(path);
		cookie.setMaxAge(maxAge);
		cookie.setHttpOnly(isHttpOnly);
		response.addCookie(cookie);
	}
	
	/**
	 * 查询value
	 *
	 * @param request 请求
	 * @param key Cookie键
	 */
	public static String getValue(HttpServletRequest request, String key) {
		Cookie cookie = get(request, key);
		if (cookie != null) {
			return cookie.getValue();
		}
		return null;
	}

	/**
	 * 查询Cookie
	 *
	 * @param request 请求
	 * @param key Cookie键
	 */
	private static Cookie get(HttpServletRequest request, String key) {
		if (request == null || key == null) {
			return null;
		}

		Cookie[] cookies = request.getCookies();
		if (ArrayUtil.isNotEmpty(cookies)) {
			for (Cookie cookie : cookies) {
				if (cookie != null && key.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		return null;
	}
	
	/**
	 * 删除Cookie
	 *
	 * @param request 请求
	 * @param response 响应
	 * @param key Cookie键
	 */
	public static void remove(HttpServletRequest request, HttpServletResponse response, String key) {
		Cookie cookie = get(request, key);
		if (cookie != null) {
			set(response, key, "", null, COOKIE_PATH, 0, true);
		}
	}

}