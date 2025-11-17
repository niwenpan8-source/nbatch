package com.nbatch.job.admin.core.domain.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

/**
 * 基础param
 * @author Mr.ni
 */
@Data
public class BaseModel<T> {

    /**
     * 页数num
     */
    private Integer start;

    /**
     * 每页条数size
     */
    private Integer length;

    /**
     * 排序字段list
     */
    private List<OrderModel> orderList;

    /**
     * 默认页数
     */
    private static final Integer DEFAULT_START = 1;

    /**
     * 默认每页条数
     */
    private static final Integer DEFAULT_LENGTH = 10;

    private static final String ORDER_DESC_TYPE = "descending";

    private static final String ORDER_ASC_TYPE = "ascending";

    /**
     * 得到page
     */
    public Page<T> getPage() {
        Page<T> page = Page.of(getStart(), getLength());
        if (CollUtil.isNotEmpty(orderList)) {
            for (OrderModel orderParam : orderList) {
                if (StrUtil.equals(orderParam.getOrderType(), ORDER_DESC_TYPE)) {
                    page.addOrder(OrderItem.desc(StrUtil.toUnderlineCase(orderParam.getOrderField())));
                } else if (StrUtil.equals(orderParam.getOrderType(), ORDER_ASC_TYPE)) {
                    page.addOrder(OrderItem.asc(StrUtil.toUnderlineCase(orderParam.getOrderField())));
                }
            }
        }
        return page;
    }

    /**
     * 获取页数
     */
    public int getStart() {
        return start == null ? DEFAULT_START : start;
    }

    /**
     * 获取每页条数
     */
    public int getLength() {
        return length == null ? DEFAULT_LENGTH : length;
    }


}
