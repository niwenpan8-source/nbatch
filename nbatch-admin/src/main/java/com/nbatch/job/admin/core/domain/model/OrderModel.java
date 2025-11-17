package com.nbatch.job.admin.core.domain.model;

import lombok.Data;

/**
 * 排序参数
 * @author Mr.ni
 */
@Data
public class OrderModel {

    /**
     * 排序字段
     */
    private String orderField;

    /**
     * 排序类型
     */
    private String orderType;
}
