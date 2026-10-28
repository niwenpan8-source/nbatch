package com.nbatch.job.admin.core.enums;

import com.nbatch.job.admin.core.util.I18nUtil;
import lombok.Getter;

/**
 * 定时类型
 */
@Getter
public enum ScheduleTypeEnum {

    NONE(I18nUtil.getString("schedule_type_none")),

    /**
     * 按 Cron 表达式调度
     */
    CRON(I18nUtil.getString("schedule_type_cron")),

    /**
     * 按固定频率调度（单位：秒）
     */
    FIX_RATE(I18nUtil.getString("schedule_type_fix_rate")),

    /**
     * 按固定延迟调度（单位：秒），以上次执行结束时间为基准
     */
    /*FIX_DELAY(I18nUtil.getString("schedule_type_fix_delay"))*/;

    private final String title;

    ScheduleTypeEnum(String title) {
        this.title = title;
    }

    public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultItem){
        for (ScheduleTypeEnum item: ScheduleTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }

}
