package com.nbatch.job.admin.core.enums;

import com.nbatch.job.admin.core.util.I18nUtil;
import lombok.Getter;

/**
 * @author Mr.ni
 */
@Getter
public enum MisfireStrategyEnum {

    /**
     * do nothing
     */
    DO_NOTHING(I18nUtil.getString("misfire_strategy_do_nothing")),

    /**
     * fire once now
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
