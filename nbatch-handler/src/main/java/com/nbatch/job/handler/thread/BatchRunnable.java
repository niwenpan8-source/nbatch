package com.nbatch.job.handler.thread;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * @description: 批量处理任务
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Log4j2
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BatchRunnable implements Runnable{

    private JSONObject cacheObj;

    @Override
    public void run() {
        // nothing to do
    }
}
