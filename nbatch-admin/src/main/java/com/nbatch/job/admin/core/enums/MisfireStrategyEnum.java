package com.nbatch.job.admin.core.enums;

import com.nbatch.job.admin.core.util.I18nUtil;
import lombok.Getter;

/**
 * 错过策略枚举
 */
@Getter
public enum MisfireStrategyEnum {

    /**
     * 忽略
     */
    DO_NOTHING(I18nUtil.getString("misfire_strategy_do_nothing")),

    /**
     * 立即执行一次
     */
    FIRE_ONCE_NOW(I18nUtil.getString("misfire_strategy_fire_once_now"));

    private final String title;

    MisfireStrategyEnum(String title) {
        this.title = title;
    }

    public static MisfireStrategyEnum match(String name, MisfireStrategyEnum defaultItem){
        for (MisfireStrategyEnum item: MisfireStrategyEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }

}
