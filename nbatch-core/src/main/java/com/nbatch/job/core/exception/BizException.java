package com.nbatch.job.core.exception;

import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BizException(String message) {
        this(HandleCodeConstant.HANDLE_CODE_FAIL, message);
    }

    public BizException(String message, Throwable cause) {
        this(HandleCodeConstant.HANDLE_CODE_FAIL, message, cause);
    }

}
