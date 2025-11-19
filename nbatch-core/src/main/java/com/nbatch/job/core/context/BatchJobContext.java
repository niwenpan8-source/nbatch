package com.nbatch.job.core.context;

import lombok.Getter;
import lombok.Setter;

/**
 * job context
 *
 * @author Mr.ni 2020-05-21
 * [Dear hj]
 */
@Getter
public class BatchJobContext {

    public static final int HANDLE_CODE_SUCCESS = 200;
    public static final int HANDLE_CODE_FAIL = 500;
    public static final int HANDLE_CODE_TIMEOUT = 502;

    // ---------------------- base info ----------------------

    /**
     * job id
     */
    private final String jobId;

    /**
     * job param
     */
    private final String jobParam;

    // ---------------------- for log ----------------------

    /**
     * job log filename
     */
    private final String jobLogFileName;

    // ---------------------- for shard ----------------------

    /**
     * shard index
     */
    private final int shardIndex;

    /**
     * shard total
     */
    private final int shardTotal;

    // ---------------------- for handle ----------------------

    /**
     * handleCode：The result status of job execution
     *      200 : success
     *      500 : fail
     *      502 : timeout
     *
     */
    @Setter
    private int handleCode;

    /**
     * handleMsg：The simple log msg of job execution
     */
    @Setter
    private String handleMsg;


    public BatchJobContext(String jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
        // default success
        this.handleCode = HANDLE_CODE_SUCCESS;
    }

    // ---------------------- tool ----------------------

    // support for child thread of job handler)
    private static final InheritableThreadLocal<BatchJobContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    public static void setXxlJobContext(BatchJobContext xxlJobContext){
        CONTEXT_HOLDER.set(xxlJobContext);
    }

    public static BatchJobContext getXxlJobContext(){
        return CONTEXT_HOLDER.get();
    }

}