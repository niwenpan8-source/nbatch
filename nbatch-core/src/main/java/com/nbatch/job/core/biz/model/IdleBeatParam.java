package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空闲检测
 * @author Mr.ni
 */
@Data
@NoArgsConstructor
public class IdleBeatParam implements Serializable {
    private static final long serialVersionUID = 42L;

    public IdleBeatParam(String jobId) {
        this.jobId = jobId;
    }

    private String jobId;


}