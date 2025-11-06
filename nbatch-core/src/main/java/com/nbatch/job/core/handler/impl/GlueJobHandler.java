package com.nbatch.job.core.handler.impl;

import com.nbatch.job.core.context.XxlJobHelper;
import com.nbatch.job.core.handler.IJobHandler;
import lombok.Getter;

/**
 * glue job handler
 *
 * @author Mr.ni 2016-5-19 21:05:45
 */
public class GlueJobHandler extends IJobHandler {

	@Getter
	private final long glueUpdatetime;
	private final IJobHandler jobHandler;
	public GlueJobHandler(IJobHandler jobHandler, long glueUpdatetime) {
		this.jobHandler = jobHandler;
		this.glueUpdatetime = glueUpdatetime;
	}

	@Override
	public void execute() throws Exception {
		XxlJobHelper.log("----------- glue.version:"+ glueUpdatetime +" -----------");
		jobHandler.execute();
	}

	@Override
	public void init() throws Exception {
		this.jobHandler.init();
	}

	@Override
	public void destroy() throws Exception {
		this.jobHandler.destroy();
	}
}
