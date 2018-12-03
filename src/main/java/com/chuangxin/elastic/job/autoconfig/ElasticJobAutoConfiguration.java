package com.chuangxin.elastic.job.autoconfig;

import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @Description:
 * @Author: yangjie
 * @Date: 2018/11/19 下午3:10
 */
@Configuration
@ConditionalOnProperty(prefix = "elastic.job", value = "enable", havingValue = "true")
@EnableConfigurationProperties(ZkConfigurationProperties.class)
@ComponentScan(value = "com.chuangxin.elastic.job")
public class ElasticJobAutoConfiguration {
	@Autowired
	private ZkConfigurationProperties zkConfigurationProperties;

	/**
	 * 初始化zk注册中心
	 *
	 * @return
	 */
	@Bean(initMethod = "init")
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

}