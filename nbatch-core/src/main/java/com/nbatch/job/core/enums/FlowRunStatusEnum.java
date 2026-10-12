package com.nbatch.job.core.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 运行流程状态
 *
 * @author Mr.ni
 */
@Getter
@RequiredArgsConstructor
public enum FlowRunStatusEnum {

    WAIT(0, "待执行"),

    RUNNING(1, "进行中"),

    COMPLETE(2, "执行完毕"),

    EXCEPTION(3, "执行异常"),

    /**
     * 只有向客户端发送请求成功，才会修改节点状态为已下发
     */
    DISPATCHED(4, "已下发"),

    SKIPPED(5, "已跳过"),

    STOPPED(6, "已停止"),

    /**
     * 已向执行器发送停止请求，等待任务线程真实退出。
     */
    STOPPING(7, "停止中"),

    /**
     * 新增节点尚未初始化到本批次。
     */
    UNINITIALIZED(8, "未初始化"),
    ;

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String value;

    public static String getValueByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlowRunStatusEnum value : FlowRunStatusEnum.values()) {
            if (ObjUtil.equals(value.getCode(), code)) {
                return value.getValue();
            }
        }
        return null;
    }
}
