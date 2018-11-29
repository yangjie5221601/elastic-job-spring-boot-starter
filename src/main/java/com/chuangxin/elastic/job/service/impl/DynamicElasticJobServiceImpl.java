package com.chuangxin.elastic.job.service.impl;

import com.chuangxin.elastic.job.config.JobConfigPropertiesConstant;
import com.chuangxin.elastic.job.enums.ElasticJobTypeName;
import com.chuangxin.elastic.job.model.ElasticJob;
import com.chuangxin.elastic.job.service.DynamicElasticJobService;
import com.chuangxin.elastic.job.util.EnvironmentUtil;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @Description:
 * @Author: yangjie
 * @Date: 2018/11/27 下午7:06
 */
@Service
@Log4j2
public class DynamicElasticJobServiceImpl implements DynamicElasticJobService {
	private static final String SPRING_JOB_SCHEDULER_PREFIX = "SpringJobScheduler-";
	@Autowired
	private ZookeeperRegistryCenter zookeeperRegistryCenter;
	@Autowired
	private ApplicationContext applicationContext;


	@Override
	public boolean addElasticJob(ElasticJob job) {
		try {
			Environment environment = applicationContext.getEnvironment();
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
			LiteJobConfiguration jobConfig;
			JobTypeConfiguration typeConfig = null;

			ElasticJobTypeName typeName = ElasticJobTypeName.getElasticJobByTypeName(job.getJobTypeName());
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
				List<BeanDefinition> elasticJobListeners = getTargetElasticJobListeners(job, environment);

				// 构建SpringJobScheduler对象来初始化任务
				BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class);
				factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);
				if (ElasticJobTypeName.SCRIPT_JOB.getTypeName().equals(job.getJobTypeName())) {
					factory.addConstructorArgValue(null);
				} else {
					BeanDefinitionBuilder rdbFactory = BeanDefinitionBuilder.rootBeanDefinition(job.getJobClass());
					factory.addConstructorArgValue(rdbFactory.getBeanDefinition());
				}
				factory.addConstructorArgValue(zookeeperRegistryCenter);
				factory.addConstructorArgValue(jobConfig);
				factory.addConstructorArgValue(elasticJobListeners);
				if (StringUtils.hasText(job.getEventTraceRdbDataSource())) {
					BeanDefinitionBuilder rdbFactory = BeanDefinitionBuilder.rootBeanDefinition(JobEventRdbConfiguration.class);
					rdbFactory.addConstructorArgReference(job.getEventTraceRdbDataSource());
					factory.addConstructorArgValue(rdbFactory.getBeanDefinition());
				}
				DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
				defaultListableBeanFactory.registerBeanDefinition(SPRING_JOB_SCHEDULER_PREFIX + job.getJobName(), factory.getBeanDefinition());
				SpringJobScheduler springJobScheduler = (SpringJobScheduler) applicationContext.getBean(SPRING_JOB_SCHEDULER_PREFIX + job.getJobName());
				springJobScheduler.init();
			}
		} catch (Exception e) {
			log.error("init task error:", e);
			return Boolean.FALSE;
		}
		return Boolean.TRUE;

	}

	@Override
	public boolean deleteElasticJob(ElasticJob elasticJob) {
		CuratorFramework client = zookeeperRegistryCenter.getClient();
		try {
			client.delete().deletingChildrenIfNeeded().forPath("/" + elasticJob.getJobName());
		} catch (Exception e) {
			log.error("delete job error", e);
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	/**
	 * 获取任务监听
	 *
	 * @param elasticJob
	 * @param environment
	 * @return
	 */
	private List<BeanDefinition> getTargetElasticJobListeners(ElasticJob elasticJob, Environment environment) {
		List<BeanDefinition> result = new ManagedList<>(2);
		String listeners = EnvironmentUtil.getEnvironmentStringValue(environment, elasticJob.getJobName(), JobConfigPropertiesConstant.LISTENER, elasticJob.getListener());
		if (StringUtils.hasText(listeners)) {
			BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(listeners);
			factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);
			result.add(factory.getBeanDefinition());
		}
		String distributedListeners = EnvironmentUtil.getEnvironmentStringValue(environment, elasticJob.getJobName(), JobConfigPropertiesConstant.DISTRIBUTED_LISTENER, elasticJob.getDistributedListener());
		long startedTimeoutMilliseconds = EnvironmentUtil.getEnvironmentLongValue(environment, elasticJob.getJobName(), JobConfigPropertiesConstant.DISTRIBUTED_LISTENER_STARTED_TIMEOUT_MILLISECONDS, elasticJob.getStartedTimeoutMilliseconds());
		long completedTimeoutMilliseconds = EnvironmentUtil.getEnvironmentLongValue(environment, elasticJob.getJobName(), JobConfigPropertiesConstant.DISTRIBUTED_LISTENER_COMPLETED_TIMEOUT_MILLISECONDS, elasticJob.getCompletedTimeoutMilliseconds());
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