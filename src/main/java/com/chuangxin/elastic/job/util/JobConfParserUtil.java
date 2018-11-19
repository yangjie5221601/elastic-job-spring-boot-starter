package com.chuangxin.elastic.job.util;

import com.chuangxin.elastic.job.annotations.ElasticJobConf;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
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
public class JobConfParserUtil implements ApplicationContextAware {
	@Autowired
	private ZookeeperRegistryCenter zookeeperRegistryCenter;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Environment environment = applicationContext.getEnvironment();
		Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(ElasticJobConf.class);
		beanMap.forEach((key, value) -> {
			Class<?> clz = value.getClass();

		});
	}
}