package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * common return
 * @author Mr.ni 2015-12-4 16:32:31
 * @param <T>
 */
@Data
@ToString
@NoArgsConstructor
public class ReturnT<T> implements Serializable {
	private static final long serialVersionUID = 42L;

	public static final int SUCCESS_CODE = 200;
	public static final int FAIL_CODE = 500;

	public static final ReturnT<String> SUCCESS = new ReturnT<>(null);
	public static final ReturnT<String> FAIL = new ReturnT<>(FAIL_CODE, null);

	private int code;
	private String msg;
	private T content;

	public ReturnT(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	public ReturnT(T content) {
		this.code = SUCCESS_CODE;
		this.content = content;
	}

}
