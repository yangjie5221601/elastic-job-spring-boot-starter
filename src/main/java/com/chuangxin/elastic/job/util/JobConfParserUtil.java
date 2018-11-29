package com.chuangxin.elastic.job.util;

import com.chuangxin.elastic.job.annotations.ElasticJobConf;
import com.chuangxin.elastic.job.autoconfig.ElasticJobAutoConfiguration;
import com.chuangxin.elastic.job.config.JobConfigPropertiesConstant;
import com.chuangxin.elastic.job.model.ElasticJob;
import com.chuangxin.elastic.job.service.DynamicElasticJobService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Description:
 * @Author: yangjie
 * @Date: 2018/11/19 下午3:23
 */
@Component
@Configuration
@Log4j2
@Import({ElasticJobAutoConfiguration.class})
public class JobConfParserUtil implements ApplicationContextAware {
	@Autowired
	private DynamicElasticJobService dynamicElasticJobService;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Environment environment = applicationContext.getEnvironment();
		Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(ElasticJobConf.class);
		beanMap.forEach((key, value) -> {
			Class<?> clz = value.getClass();
			ElasticJobConf conf = clz.getAnnotation(ElasticJobConf.class);
			ElasticJob job = convert2Model(conf, environment, clz);
			dynamicElasticJobService.addElasticJob(job);
		});
	}

	/**
	 * 转换参数到model
	 *
	 * @param conf
	 * @param environment
	 * @param clz
	 * @return
	 */
	private ElasticJob convert2Model(ElasticJobConf conf, Environment environment, Class clz) {
		String jobClass = clz.getName();
		String jobName = conf.name();
		String jobTypeName = clz.getInterfaces()[0].getSimpleName();
		String cron = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.CRON, conf.cron());
		String shardingItemParameters = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.SHARDING_ITEM_PARAMETERS, conf.shardingItemParameters());
		String description = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.DESCRIPTION, conf.description());
		String jobParameter = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.JOB_PARAMETER, conf.jobParameter());
		String jobExceptionHandler = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.JOB_EXCEPTION_HANDLER, conf.jobExceptionHandler());
		String executorServiceHandler = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.EXECUTOR_SERVICE_HANDLER, conf.executorServiceHandler());
		String jobShardingStrategyClass = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.JOB_SHARDING_STRATEGY_CLASS, conf.jobShardingStrategyClass());
		String scriptCommandLine = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.SCRIPT_COMMAND_LINE, conf.scriptCommandLine());
		String eventTraceRdbDataSource = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.EVENT_TRACE_RDB_DATA_SOURCE, conf.eventTraceRdbDataSource());
		boolean failover = EnvironmentUtil.getEnvironmentBooleanValue(environment, jobName, JobConfigPropertiesConstant.FAILOVER, conf.failover());
		boolean misfire = EnvironmentUtil.getEnvironmentBooleanValue(environment, jobName, JobConfigPropertiesConstant.MISFIRE, conf.misfire());
		boolean overwrite = EnvironmentUtil.getEnvironmentBooleanValue(environment, jobName, JobConfigPropertiesConstant.OVERWRITE, conf.overwrite());
		boolean disabled = EnvironmentUtil.getEnvironmentBooleanValue(environment, jobName, JobConfigPropertiesConstant.DISABLED, conf.disabled());
		boolean monitorExecution = EnvironmentUtil.getEnvironmentBooleanValue(environment, jobName, JobConfigPropertiesConstant.MONITOR_EXECUTION, conf.monitorExecution());
		boolean streamingProcess = EnvironmentUtil.getEnvironmentBooleanValue(environment, jobName, JobConfigPropertiesConstant.STREAMING_PROCESS, conf.streamingProcess());
		int shardingTotalCount = EnvironmentUtil.getEnvironmentIntValue(environment, jobName, JobConfigPropertiesConstant.SHARDING_TOTAL_COUNT, conf.shardingTotalCount());
		int monitorPort = EnvironmentUtil.getEnvironmentIntValue(environment, jobName, JobConfigPropertiesConstant.MONITOR_PORT, conf.monitorPort());
		int maxTimeDiffSeconds = EnvironmentUtil.getEnvironmentIntValue(environment, jobName, JobConfigPropertiesConstant.MAX_TIME_DIFF_SECONDS, conf.maxTimeDiffSeconds());
		int reconcileIntervalMinutes = EnvironmentUtil.getEnvironmentIntValue(environment, jobName, JobConfigPropertiesConstant.RECONCILE_INTERVAL_MINUTES, conf.reconcileIntervalMinutes());
		return ElasticJob.builder().reconcileIntervalMinutes(reconcileIntervalMinutes).
				maxTimeDiffSeconds(maxTimeDiffSeconds).monitorPort(monitorPort).shardingTotalCount(shardingTotalCount).
				streamingProcess(streamingProcess).monitorExecution(monitorExecution).
				disabled(disabled).overwrite(overwrite).misfire(misfire).failover(failover).
				scriptCommandLine(scriptCommandLine).jobShardingStrategyClass(jobShardingStrategyClass).
				executorServiceHandle(executorServiceHandler).jobClass(jobClass).jobName(jobName).jobClass(jobClass)
				.shardingItemParameters(shardingItemParameters).description(description)
				.jobParameter(jobParameter).cron(cron).jobExceptionHanprivatedler(jobExceptionHandler).eventTraceRdbDataSource(eventTraceRdbDataSource).jobTypeName(jobTypeName).build();
	}
}