package com.chuangxin.elastic.job.util;

import com.chuangxin.elastic.job.annotations.ElasticJobConf;
import com.chuangxin.elastic.job.autoconfig.ElasticJobAutoConfiguration;
import com.chuangxin.elastic.job.config.JobConfigPropertiesConstant;
import com.chuangxin.elastic.job.enums.ElasticJobTypeName;
import com.chuangxin.elastic.job.model.ElasticJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
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
	private ZookeeperRegistryCenter zookeeperRegistryCenter;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Environment environment = applicationContext.getEnvironment();
		Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(ElasticJobConf.class);
		beanMap.forEach((key, value) -> {
			Class<?> clz = value.getClass();
			String jobTypeName = value.getClass().getInterfaces()[0].getSimpleName();
			ElasticJobConf conf = clz.getAnnotation(ElasticJobConf.class);
			ElasticJob job = convert2Model(conf, environment, clz);
			// 核心配置
			JobCoreConfiguration coreConfig =
					JobCoreConfiguration.newBuilder(job.getJobName(), job.getCron(), job.getShardingTotalCount())
							.shardingItemParameters(job.getShardingItemParameters())
							.description(job.getDescription())
							.failover(job.isFailover())
							.jobParameter(job.getJobParameter())
							.misfire(job.isMisfire())
							.jobProperties(JobProperties.JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), job.getJobExceptionHanprivatedler())
							.jobProperties(JobProperties.JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), job.getExecutorServiceHandle())
							.build();

			// 不同类型的任务配置处理
			LiteJobConfiguration jobConfig = null;
			JobTypeConfiguration typeConfig = null;

			ElasticJobTypeName typeName = ElasticJobTypeName.getElasticJobByTypeName(jobTypeName);
			if (typeName != null) {
				switch (typeName) {
					case SIMPLE_JOB: {
						typeConfig = new SimpleJobConfiguration(coreConfig, job.getJobClass());
						break;
					}
					case DATA_FLOW_JOB: {
						typeConfig = new DataflowJobConfiguration(coreConfig, job.getJobClass(), job.isStreamingProcess());
						break;
					}
					case SCRIPT_JOB: {
						typeConfig = new ScriptJobConfiguration(coreConfig, job.getScriptCommandLine());
						break;
					}
					default:
						break;

				}
				jobConfig = LiteJobConfiguration.newBuilder(typeConfig)
						.overwrite(job.isOverwrite())
						.disabled(job.isDisabled())
						.monitorPort(job.getMonitorPort())
						.monitorExecution(job.isMonitorExecution())
						.maxTimeDiffSeconds(job.getMaxTimeDiffSeconds())
						.jobShardingStrategyClass(job.getJobShardingStrategyClass())
						.reconcileIntervalMinutes(job.getReconcileIntervalMinutes())
						.build();

				List<BeanDefinition> elasticJobListeners = getTargetElasticJobListeners(conf, environment);
				// 构建SpringJobScheduler对象来初始化任务
				BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class);
				factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);
				if ("ScriptJob".equals(jobTypeName)) {
					factory.addConstructorArgValue(null);
				} else {
					factory.addConstructorArgValue(value);
				}
				factory.addConstructorArgValue(zookeeperRegistryCenter);
				factory.addConstructorArgValue(jobConfig);
				factory.addConstructorArgValue(elasticJobListeners);
				DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
				defaultListableBeanFactory.registerBeanDefinition("SpringJobScheduler", factory.getBeanDefinition());
				SpringJobScheduler springJobScheduler = (SpringJobScheduler) applicationContext.getBean("SpringJobScheduler");
				springJobScheduler.init();
			}


		});
	}

	private List<BeanDefinition> getTargetElasticJobListeners(ElasticJobConf conf, Environment environment) {
		List<BeanDefinition> result = new ManagedList<BeanDefinition>(2);
		String listeners = EnvironmentUtil.getEnvironmentStringValue(environment, conf.name(), JobConfigPropertiesConstant.LISTENER, conf.listener());
		if (StringUtils.hasText(listeners)) {
			BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(listeners);
			factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);
			result.add(factory.getBeanDefinition());
		}

		String distributedListeners = EnvironmentUtil.getEnvironmentStringValue(environment, conf.name(), JobConfigPropertiesConstant.DISTRIBUTED_LISTENER, conf.distributedListener());
		long startedTimeoutMilliseconds = EnvironmentUtil.getEnvironmentLongValue(environment, conf.name(), JobConfigPropertiesConstant.DISTRIBUTED_LISTENER_STARTED_TIMEOUT_MILLISECONDS, conf.startedTimeoutMilliseconds());
		long completedTimeoutMilliseconds = EnvironmentUtil.getEnvironmentLongValue(environment, conf.name(), JobConfigPropertiesConstant.DISTRIBUTED_LISTENER_COMPLETED_TIMEOUT_MILLISECONDS, conf.completedTimeoutMilliseconds());

		if (StringUtils.hasText(distributedListeners)) {
			BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(distributedListeners);
			factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);
			factory.addConstructorArgValue(startedTimeoutMilliseconds);
			factory.addConstructorArgValue(completedTimeoutMilliseconds);
			result.add(factory.getBeanDefinition());
		}
		return result;
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
		String cron = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.CRON, conf.cron());
		String shardingItemParameters = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.SHARDING_ITEM_PARAMETERS, conf.shardingItemParameters());
		String description = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.DESCRIPTION, conf.description());
		String jobParameter = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.JOB_PARAMETER, conf.jobParameter());
		String jobExceptionHandler = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.JOB_EXCEPTION_HANDLER, conf.jobExceptionHandler());
		String executorServiceHandler = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.EXECUTOR_SERVICE_HANDLER, conf.executorServiceHandler());
		String jobShardingStrategyClass = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.JOB_SHARDING_STRATEGY_CLASS, conf.jobShardingStrategyClass());
		String scriptCommandLine = EnvironmentUtil.getEnvironmentStringValue(environment, jobName, JobConfigPropertiesConstant.SCRIPT_COMMAND_LINE, conf.scriptCommandLine());
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
		return ElasticJob.builder().reconcileIntervalMinutes(reconcileIntervalMinutes).maxTimeDiffSeconds(maxTimeDiffSeconds).monitorPort(monitorPort).shardingTotalCount(shardingTotalCount).streamingProcess(streamingProcess).monitorExecution(monitorExecution).disabled(disabled).overwrite(overwrite).misfire(misfire).failover(failover).scriptCommandLine(scriptCommandLine).jobShardingStrategyClass(jobShardingStrategyClass).executorServiceHandle(executorServiceHandler).jobClass(jobClass).jobName(jobName).jobClass(cron).shardingItemParameters(shardingItemParameters).description(description).jobParameter(jobParameter).jobExceptionHanprivatedler(jobExceptionHandler).build();
	}
}