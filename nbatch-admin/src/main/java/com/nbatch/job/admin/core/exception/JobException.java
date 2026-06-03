package com.nbatch.job.admin.core.exception;

import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.exception.BizException;

/**
 * @author Mr.ni 2019-05-04 23:19:29
 */
public class JobException extends BizException {

    public JobException() {
        super(HandleCodeConstant.HANDLE_CODE_FAIL, "system error");
    }
    public JobException(String message) {
        super(HandleCodeConstant.HANDLE_CODE_FAIL, message);
    }
    public JobException(int code, String message) {
        super(code, message);
    }
    public JobException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

}
