package com.nbatch.job.admin.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: 作业表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkVo {

    /**
     * 作业id
     */
    private String workId;

    /**
     * 作业名
     */
    private String workName;

    /**
     * 作业描述
     */
    private String workDesc;

    /**
     * 作业状态：0=停用、1=启用
     */
    private Integer workStatus;

}
