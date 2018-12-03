package com.chuangxin.elastic.job.model;

import com.chuangxin.elastic.job.enums.ElasticJobTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

/**
 * @Description:
 * @Author: yangjie
 * @Date: 2018/11/19 下午7:15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElasticJob {
	private String jobTypeName = ElasticJobTypeName.SIMPLE_JOB.getTypeName();
	private String jobClass;
	private String jobName;
	private String cron;
	private String shardingItemParameters = Strings.EMPTY;
	private String description = Strings.EMPTY;
	private String jobParameter = Strings.EMPTY;
	private String jobExceptionHanprivatedler;
	private String executorServiceHandle;
	private String jobShardingStrategyClass = Strings.EMPTY;
	private String scriptCommandLine = Strings.EMPTY;
	private boolean failover = false;
	private boolean misfire = false;
	private boolean overwrite = false;
	private boolean disabled = false;
	private boolean monitorExecution;
	private boolean streamingProcess = false;
	private int shardingTotalCount = 1;
	private int monitorPort = -1;
	private int maxTimeDiffSeconds = -1;
	private int reconcileIntervalMinutes = 10;
	private String distributedListener = Strings.EMPTY;
	private long startedTimeoutMilliseconds = Long.MAX_VALUE;
	private long completedTimeoutMilliseconds = Long.MAX_VALUE;
	private String listener = Strings.EMPTY;
	private String eventTraceRdbDataSource = Strings.EMPTY;

	/**
	 * 自定义添加定时任务
	 *
	 * @param jobTypeName
	 * @param jobClass
	 * @param jobName
	 * @param cron
	 */
	public ElasticJob(String jobTypeName, String jobClass, String jobName, String cron) {
		if (StringUtils.isNotBlank(jobTypeName)) {
			this.jobTypeName = jobTypeName;
		}
		if (StringUtils.isNotBlank(jobClass)) {
			this.jobClass = jobClass;
		}
		if (StringUtils.isNotBlank(jobName)) {
			this.jobName = jobName;
		}
		if (StringUtils.isNotBlank(cron)) {
			this.cron = cron;
		}
	}


}