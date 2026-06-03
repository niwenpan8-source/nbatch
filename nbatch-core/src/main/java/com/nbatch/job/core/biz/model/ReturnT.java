package com.nbatch.job.core.biz.model;

import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * common return
 * @author Mr.ni
 */
@Data
@ToString
@NoArgsConstructor
public class ReturnT<T> implements Serializable {
	private static final long serialVersionUID = 42L;

	public static final ReturnT<String> SUCCESS = new ReturnT<>(null);
	public static final ReturnT<String> FAIL = new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, null);

	private int code;
	private String msg;
	private T content;

	public ReturnT(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	public ReturnT(T content) {
		this.code = HandleCodeConstant.HANDLE_CODE_SUCCESS;
		this.content = content;
	}

	public boolean isSuccess() {
		return this.code == HandleCodeConstant.HANDLE_CODE_SUCCESS;
	}

	public boolean isFail() {
		return this.code != HandleCodeConstant.HANDLE_CODE_SUCCESS;
	}

	public static <T> ReturnT<T> error(String msg) {
		return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, msg);
	}

	public static <T> ReturnT<T> error(int code, String msg) {
		return new ReturnT<>(code, msg);
	}

	public static <T> ReturnT<T> success(T data) {
		return new ReturnT<>(data);
	}

}
