package com.nbatch.job.admin.service;

import com.nbatch.job.core.biz.model.ReturnT;

/**
 * 作业流程运行控制服务。
 */
public interface IJobWorkRunService {

    /**
     * 恢复运行中的作业流程。
     * @param workId 作业流程ID
     * @param force 是否强制恢复
     * @return 成功返回成功信息，失败返回失败信息
     */
    ReturnT<String> recoverRunWork(String workId, boolean force);

    /**
     * 恢复运行中的作业流程。
     * @param workId 作业流程ID
     * @return 成功返回成功信息，失败返回失败信息
     */
    default ReturnT<String> recoverRunWork(String workId) {
        return recoverRunWork(workId, false);
    }

    /**
     * 停止运行中的作业流程。
     * @param workId 作业流程ID
     * @return 成功返回成功信息，失败返回失败信息
     */
    ReturnT<String> stopLatestRunWork(String workId);

    /**
     * 重新运行作业流程。
     * @param workId 作业流程ID
     * @param turnDate 转牌日期
     * @param force 是否强制重新运行
     * @return 成功返回成功信息，失败返回失败信息
     */
    ReturnT<String> rerunRunWork(String workId, String turnDate, boolean force);

    /**
     * 重新运行作业流程。
     * @param workId 作业流程ID
     * @param turnDate 转牌日期
     * @return 成功返回成功信息，失败返回失败信息
     */
    default ReturnT<String> rerunRunWork(String workId, String turnDate) {
        return rerunRunWork(workId, turnDate, false);
    }
}
