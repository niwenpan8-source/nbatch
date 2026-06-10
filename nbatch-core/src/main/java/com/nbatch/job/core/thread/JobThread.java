package com.nbatch.job.core.thread;

import cn.hutool.core.collection.CollUtil;
import com.nbatch.job.core.biz.model.HandleCallbackParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.context.BatchJobContext;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.core.handler.IJobHandler;
import com.nbatch.job.core.log.JobFileAppender;
import com.nbatch.job.core.util.ThrowableUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.nbatch.job.core.enums.CallbackTypeEnum.LOG_CALLBACK;


/**
 * handler thread
 * @author Mr.ni
 */
@Slf4j
public class JobThread extends Thread{

	private final String jobId;
	@Getter
    private final IJobHandler handler;
	private final LinkedBlockingQueue<TriggerParam> triggerQueue;
	// avoid repeat trigger for the same TRIGGER_LOG_ID
	private final Set<String> triggerLogIdSet;

	private volatile boolean toStop = false;
	private String stopReason;

	// if running job
    private boolean running = false;
	// idle times
	private int idleTimes = 0;


	public JobThread(String jobId, IJobHandler handler) {
		this.jobId = jobId;
		this.handler = handler;
		this.triggerQueue = new LinkedBlockingQueue<>();
		this.triggerLogIdSet = Collections.synchronizedSet(new HashSet<>());

		// assign job thread name
		this.setName("job, JobThread-"+jobId+"-"+System.currentTimeMillis());
	}

    /**
     * new trigger to queue
     *
     * @param triggerParam 调度参数
     */
	public ReturnT<String> pushTriggerQueue(TriggerParam triggerParam) {
		// avoid repeat
		if (triggerLogIdSet.contains(triggerParam.getLogId())) {
			log.info(">>>>>>>>>>> repeate trigger job, logId:{}", triggerParam.getLogId());
			return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "repeate trigger job, logId:" + triggerParam.getLogId());
		}

		triggerLogIdSet.add(triggerParam.getLogId());
		triggerQueue.add(triggerParam);
        return ReturnT.SUCCESS;
	}

    /**
     * kill job thread
     *
     * @param stopReason stop reason
     */
	public void toStop(String stopReason) {
		/*
		 * Thread.interrupt只支持终止线程的阻塞状态(wait、join、sleep)，
		 * 在阻塞出抛出InterruptedException异常,但是并不会终止运行的线程本身；
		 * 所以需要注意，此处彻底销毁本线程，需要通过共享变量方式；
		 */
		this.toStop = true;
		this.stopReason = stopReason;
	}

    /**
     * is running job
     */
    public boolean isRunningOrHasQueue() {
        return running || CollUtil.isNotEmpty(triggerQueue);
    }

    @Override
	public void run() {

    	// init
    	try {
			handler.init();
		} catch (Throwable e) {
    		log.error(e.getMessage(), e);
		}

		// execute
		while(!toStop){
			running = false;
			idleTimes++;

            TriggerParam triggerParam = null;
            try {
				// to check toStop signal, we need cycle, so wo cannot use queue.take(), instand of poll(timeout)
				triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
				if (triggerParam!=null) {
					running = true;
					idleTimes = 0;
					triggerLogIdSet.remove(triggerParam.getLogId());

					// log filename, like "logPath/yyyy-MM-dd/9999.log"
					String logFileName = JobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()), triggerParam.getLogId());
					BatchJobContext batchJobContext = new BatchJobContext(
							triggerParam.getJobId(),
							triggerParam.getExecutorParams(),
							logFileName,
							triggerParam.getBroadcastIndex(),
							triggerParam.getBroadcastTotal());

					// init job context
					BatchJobContext.setBatchJobContext(batchJobContext);

					// execute
					BatchJobHelper.log("<br>----------- job job execute start -----------<br>----------- Param:" + batchJobContext.getJobParam());

					if (triggerParam.getExecutorTimeout() > 0) {
						// limit timeout
						Thread futureThread = null;
						try {
							FutureTask<Boolean> futureTask = new FutureTask<>(() -> {

                                // init job context
                                BatchJobContext.setBatchJobContext(batchJobContext);

                                handler.execute();
                                return true;
                            });
							futureThread = new Thread(futureTask);
							futureThread.start();

							futureTask.get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
						} catch (TimeoutException e) {

							BatchJobHelper.log("<br>----------- job job execute timeout");
							BatchJobHelper.log(e);

							// handle result
							BatchJobHelper.handleTimeout("job execute timeout ");
						} finally {
                            assert futureThread != null;
                            futureThread.interrupt();
						}
					} else {
						// just execute
						handler.execute();
					}

					// valid execute handle data
					if (BatchJobContext.getBatchJobContext().getHandleCode() <= 0) {
						BatchJobHelper.handleFail("job handle result lost.");
					} else {
						String tempHandleMsg = BatchJobContext.getBatchJobContext().getHandleMsg();
						tempHandleMsg = (tempHandleMsg!=null && tempHandleMsg.length()>50000)
								?tempHandleMsg.substring(0, 50000).concat("...")
								:tempHandleMsg;
						BatchJobContext.getBatchJobContext().setHandleMsg(tempHandleMsg);
					}
					BatchJobHelper.log("<br>----------- job job execute end(finish) -----------<br>----------- Result: handleCode="
							+ BatchJobContext.getBatchJobContext().getHandleCode()
							+ ", handleMsg = "
							+ BatchJobContext.getBatchJobContext().getHandleMsg()
					);

				} else {
					if (idleTimes > 30) {
						// avoid concurrent trigger causes jobId-lost
						if(CollUtil.isEmpty(triggerQueue)) {
							BatchJobExecutor.removeJobThread(jobId, "excutor idle times over limit.");
						}
					}
				}
			} catch (Throwable e) {
				if (e instanceof InterruptedException && triggerParam == null) {
					Thread.currentThread().interrupt();
					break;
				}
				if (toStop) {
					BatchJobHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
				}

				// handle result
                String errorMsg = ThrowableUtil.toString(e);
				log.error("处理器执行任务发生错误！", e);
				BatchJobHelper.handleFail(errorMsg);

				BatchJobHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- job job execute end(error) -----------");
			} finally {
                if(triggerParam != null) {
                    // callback handler info
                    if (!toStop) {
						HandleCallbackParam handleCallbackParam = new HandleCallbackParam();
						handleCallbackParam.setLogId(triggerParam.getLogId());
						handleCallbackParam.setCallBackType(LOG_CALLBACK.getValue());
						handleCallbackParam.getLogCallBackParam()
								.setLogDateTim(triggerParam.getLogDateTime())
								.setHandleCode(BatchJobContext.getBatchJobContext().getHandleCode())
								.setHandleMsg(BatchJobContext.getBatchJobContext().getHandleMsg());
						TriggerCallbackThread.pushCallBack(handleCallbackParam);
                    } else {
						HandleCallbackParam handleCallbackParam = new HandleCallbackParam();
						handleCallbackParam.setCallBackType(LOG_CALLBACK.getValue());
						handleCallbackParam.setLogId(triggerParam.getLogId());
						handleCallbackParam.getLogCallBackParam()
								.setLogDateTim(triggerParam.getLogDateTime())
								.setHandleCode(HandleCodeConstant.HANDLE_CODE_FAIL)
								.setHandleMsg(stopReason + " [job running, killed]");
                        // is killed
                        TriggerCallbackThread.pushCallBack(handleCallbackParam);
                    }
                }
            }
        }

		// callback trigger request in queue
		while(CollUtil.isNotEmpty(triggerQueue)){
			TriggerParam triggerParam = triggerQueue.poll();
			if (triggerParam != null) {
				HandleCallbackParam handleCallbackParam = new HandleCallbackParam();
				handleCallbackParam.setCallBackType(LOG_CALLBACK.getValue());
				handleCallbackParam.setLogId(triggerParam.getLogId());
				handleCallbackParam.getLogCallBackParam()
						.setLogDateTim(triggerParam.getLogDateTime())
						.setHandleCode(HandleCodeConstant.HANDLE_CODE_FAIL)
						.setHandleMsg(stopReason + " [job not executed, in the job queue, killed.]");
				// is killed
				TriggerCallbackThread.pushCallBack(handleCallbackParam);
			}
		}

		// destroy
		try {
			handler.destroy();
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		log.info(">>>>>>>>>>> job JobThread stoped, hashCode:{}", Thread.currentThread());
	}
}
