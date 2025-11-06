package com.nbatch.job.admin.core.model;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 *
 * @author nbatch
 * @date 2025/11/05
 */
@Data
public class XxlJobGroup {

    private int id;
    private String appname;
    private String title;

    /**
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    private int addressType;

    /**
     * 执行器地址列表，多地址逗号分隔(手动录入)
     */
    private String addressList;
    private Date updateTime;

    /**
     * 执行器地址列表(系统注册)
     */
    private List<String> registryList;
    public List<String> getRegistryList() {
        if (StrUtil.isNotBlank(addressList)) {
            registryList = new ArrayList<>(Arrays.asList(addressList.split(StrPool.COMMA)));
        }
        return registryList;
    }

}
