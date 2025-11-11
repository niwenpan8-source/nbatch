package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Mr.ni 2020-04-11 22:27
 */
@Data
@NoArgsConstructor
public class LogParam implements Serializable {
    private static final long serialVersionUID = 42L;

    public LogParam(long logDateTim, String logId, int fromLineNum) {
        this.logDateTim = logDateTim;
        this.logId = logId;
        this.fromLineNum = fromLineNum;
    }

    private long logDateTim;
    private String logId;
    private int fromLineNum;

}