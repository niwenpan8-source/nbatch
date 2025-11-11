package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * 处理回调
 * @author nbatch
 * @date 17/3/2
 */
@Data
@ToString
@NoArgsConstructor
public class HandleCallbackParam implements Serializable {
    private static final long serialVersionUID = 42L;

    private String logId;
    private long logDateTim;

    private int handleCode;
    private String handleMsg;

    public HandleCallbackParam(String logId, long logDateTim, int handleCode, String handleMsg) {
        this.logId = logId;
        this.logDateTim = logDateTim;
        this.handleCode = handleCode;
        this.handleMsg = handleMsg;
    }

}
