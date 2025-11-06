package com.nbatch.job.admin.core.model;

import lombok.Data;

import java.util.Date;

/**
 * job registry
 * @author nbatch
 * @date 2025/11/5
 */
@Data
public class XxlJobRegistry {

    private int id;
    private String registryGroup;
    private String registryKey;
    private String registryValue;
    private Date updateTime;

}
