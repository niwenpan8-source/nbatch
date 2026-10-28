package com.nbatch.job.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 回调类型枚举
 */

@Getter
@RequiredArgsConstructor
public enum CallbackTypeEnum {

    LOG_CALLBACK("log_callback");

    private final String value;
}
