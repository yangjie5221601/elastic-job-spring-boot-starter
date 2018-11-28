package com.chuangxin.elastic.job.model;

import lombok.Builder;
import lombok.Data;

/**
 * @Description:
 * @Author: yangjie
 * @Date: 2018/11/19 下午7:15
 */
@Data
@Builder
public class ElasticJob {
	private String jobTypeName;
	private String jobClass;
	private String jobName;
	private String cron;
	private String shardingItemParameters;
	private String description;
	private String jobParameter;
	private String jobExceptionHanprivatedler;
	private String executorServiceHandle;
	private String jobShardingStrategyClass;
	private String scriptCommandLine;
	private boolean failover;
	private boolean misfire;
	private boolean overwrite;
	private boolean disabled;
	private boolean monitorExecution;
	private boolean streamingProcess;
	private int shardingTotalCount;
	private int monitorPort;
	private int maxTimeDiffSeconds;
	private int reconcileIntervalMinutes;
	private String distributedListener;
	private long startedTimeoutMilliseconds;
	private long completedTimeoutMilliseconds;
	private String listener;

}