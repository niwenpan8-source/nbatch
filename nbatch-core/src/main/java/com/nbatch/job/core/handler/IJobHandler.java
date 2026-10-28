package com.nbatch.job.core.handler;

/**
 * 作业处理器
 */
public abstract class IJobHandler {

	/**
	 * 执行处理逻辑，执行器收到调度请求后调用
	 */
	public abstract void execute() throws Exception;

	/**
	 * 初始化处理器，在作业线程初始化时调用
	 */
	public void init() throws Exception {
		// 执行业务初始化
	}

	/**
	 * 销毁处理器，在作业线程销毁时调用
	 */
	public void destroy() throws Exception {
		// 执行业务清理
	}

}
