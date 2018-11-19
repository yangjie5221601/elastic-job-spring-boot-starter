package com.chuangxin.elastic.job.autoconfig;

import com.chuangxin.elastic.job.util.JobConfParserUtil;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description:
 * @Author: yangjie
 * @Date: 2018/11/19 下午3:10
 */
@Configuration
@EnableConfigurationProperties(ZkConfigurationProperties.class)
public class ElasticJobAutoConfiguration {
	@Autowired
	private ZkConfigurationProperties zkConfigurationProperties;

	/**
	 * 初始化zk注册中心
	 *
	 * @return
	 */
	@Bean
	public ZookeeperRegistryCenter zookeeperRegistryCenter() {
		ZookeeperConfiguration config = new ZookeeperConfiguration(zkConfigurationProperties.getServerLists(), zkConfigurationProperties.getNamespace());
		config.setBaseSleepTimeMilliseconds(zkConfigurationProperties.getBaseSleepTimeMilliseconds());
		config.setConnectionTimeoutMilliseconds(zkConfigurationProperties.getConnectionTimeoutMilliseconds());
		config.setDigest(zkConfigurationProperties.getDigest());
		config.setMaxRetries(zkConfigurationProperties.getMaxRetries());
		config.setMaxSleepTimeMilliseconds(zkConfigurationProperties.getMaxSleepTimeMilliseconds());
		config.setSessionTimeoutMilliseconds(zkConfigurationProperties.getSessionTimeoutMilliseconds());
		ZookeeperRegistryCenter center = new ZookeeperRegistryCenter(config);
		return center;
	}

	/**
	 * 初始化elastic job解析类
	 *
	 * @return
	 */
	@Bean
	public JobConfParserUtil jobConfParserUtil() {
		return new JobConfParserUtil();
	}

}