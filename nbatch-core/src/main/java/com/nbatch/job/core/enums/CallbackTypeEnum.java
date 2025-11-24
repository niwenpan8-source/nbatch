package com.nbatch.job.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 回调类型枚举
 * @author Mr.ni
 */

@Getter
@RequiredArgsConstructor
public enum CallbackTypeEnum {

    NODE_STATUS_CALLBACK("node_status_callback"),

    LOG_CALLBACK("log_callback");

    private final String value;
}
