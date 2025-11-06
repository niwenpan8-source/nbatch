package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 日志结果
 * @author Mr.ni
 * @date 2025/11/05
 */
@Data
@NoArgsConstructor
public class LogResult implements Serializable {
    private static final long serialVersionUID = 42L;

    public LogResult(int fromLineNum, int toLineNum, String logContent, boolean isEnd) {
        this.fromLineNum = fromLineNum;
        this.toLineNum = toLineNum;
        this.logContent = logContent;
        this.isEnd = isEnd;
    }

    private int fromLineNum;
    private int toLineNum;
    private String logContent;
    private boolean isEnd;

}
