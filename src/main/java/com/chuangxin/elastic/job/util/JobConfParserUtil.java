package com.chuangxin.elastic.job.util;

import com.chuangxin.elastic.job.annotations.ElasticJobConf;
import com.chuangxin.elastic.job.config.JobConfigPropertiesConstant;
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

			// 核心配置
			JobCoreConfiguration coreConfig =
					JobCoreConfiguration.newBuilder(jobName, cron, shardingTotalCount)
							.shardingItemParameters(shardingItemParameters)
							.description(description)
							.failover(failover)
							.jobParameter(jobParameter)
							.misfire(misfire)
							.jobProperties(JobProperties.JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), jobExceptionHandler)
							.jobProperties(JobProperties.JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), executorServiceHandler)
							.build();

			// 不同类型的任务配置处理
			LiteJobConfiguration jobConfig = null;
			JobTypeConfiguration typeConfig = null;
			if (jobTypeName.equals("SimpleJob")) {
				typeConfig = new SimpleJobConfiguration(coreConfig, jobClass);
			}

			if (jobTypeName.equals("DataflowJob")) {
				typeConfig = new DataflowJobConfiguration(coreConfig, jobClass, streamingProcess);
			}

			if (jobTypeName.equals("ScriptJob")) {
				typeConfig = new ScriptJobConfiguration(coreConfig, scriptCommandLine);
			}

			jobConfig = LiteJobConfiguration.newBuilder(typeConfig)
					.overwrite(overwrite)
					.disabled(disabled)
					.monitorPort(monitorPort)
					.monitorExecution(monitorExecution)
					.maxTimeDiffSeconds(maxTimeDiffSeconds)
					.jobShardingStrategyClass(jobShardingStrategyClass)
					.reconcileIntervalMinutes(reconcileIntervalMinutes)
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
}