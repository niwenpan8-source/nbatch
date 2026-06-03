package com.nbatch.job.admin.controller.resolver;

import com.nbatch.job.admin.core.exception.JobException;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JobException.class)
    public ReturnT<String> handleJobException(JobException e) {
        return ReturnT.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BizException.class)
    public ReturnT<String> handleBizException(BizException e) {
        return ReturnT.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ReturnT<String> handleIllegalArgument(IllegalArgumentException e) {
        return ReturnT.error(HandleCodeConstant.HANDLE_CODE_FAIL, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ReturnT<String> handleException(Exception e) {
        log.error("GlobalExceptionHandler:", e);
        return ReturnT.error(HandleCodeConstant.HANDLE_CODE_FAIL, e.getMessage());
    }

}
