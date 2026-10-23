package com.nbatch.job.executor.service.jobhandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NbatchSampleJobCpuRoundTest {

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void cpuRoundsShouldCompleteWithinOneSchedulingSlice() throws Exception {
        NbatchSampleJob sampleJob = new NbatchSampleJob();
        int processors = Math.min(Runtime.getRuntime().availableProcessors(), 4);

        assertTrue(invokeRound(sampleJob, "runCpuRound", processors) > 0);
        assertTrue(invokeRound(sampleJob, "runCpuRoundIgnoringInterrupt", processors) > 0);
    }

    private long invokeRound(NbatchSampleJob sampleJob, String methodName, int processors) throws Exception {
        Method method = NbatchSampleJob.class.getDeclaredMethod(methodName, int.class);
        method.setAccessible(true);
        return (Long) method.invoke(sampleJob, processors);
    }
}
