package com.nbatch.job.handler.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 处理异常
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HandlerException extends RuntimeException {

    private int code;

    private String message;

    private Throwable throwable;

    public HandlerException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public HandlerException(int code, Throwable cause) {
        super(cause);
        this.code = code;
        this.throwable = cause;
    }
}
