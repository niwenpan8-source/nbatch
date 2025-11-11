package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Mr.ni 2020-04-11 22:27
 */
@Data
@NoArgsConstructor
public class KillParam implements Serializable {
    private static final long serialVersionUID = 42L;

    public KillParam(String jobId) {
        this.jobId = jobId;
    }

    private String jobId;


}