package com.chuangxin.elastic.job.util;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @Description:
 * @Author: yangjie
 * @Date: 2018/11/19 下午3:34
 */
public class EnvironmentUtil {

	private static String prefix = "elastic.job.";

	/**
	 * 获取配置中的任务属性值，environment没有就用注解中的值
	 *
	 * @param jobName      任务名称
	 * @param fieldName    属性名称
	 * @param defaultValue 默认值
	 * @return
	 */
	public static String getEnvironmentStringValue(Environment environment, String jobName, String fieldName, String defaultValue) {
		String key = prefix + jobName + "." + fieldName;
		String value = environment.getProperty(key);
		if (StringUtils.hasText(value)) {
			return value;
		}
		return defaultValue;
	}

	/**
	 * 获取配置中的任务属性值，environment没有就用注解中的值
	 *
	 * @param environment
	 * @param jobName
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public static int getEnvironmentIntValue(Environment environment, String jobName, String fieldName, int defaultValue) {
		String key = prefix + jobName + "." + fieldName;
		String value = environment.getProperty(key);
		if (StringUtils.hasText(value)) {
			return Integer.valueOf(value);
		}
		return defaultValue;
	}

	/**
	 * 获取配置中的任务属性值，environment没有就用注解中的值
	 *
	 * @param environment
	 * @param jobName
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public static long getEnvironmentLongValue(Environment environment, String jobName, String fieldName, long defaultValue) {
		String key = prefix + jobName + "." + fieldName;
		String value = environment.getProperty(key);
		if (StringUtils.hasText(value)) {
			return Long.valueOf(value);
		}
		return defaultValue;
	}

	/**
	 * 获取配置中的任务属性值，environment没有就用注解中的值
	 *
	 * @param environment
	 * @param jobName
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public static boolean getEnvironmentBooleanValue(Environment environment, String jobName, String fieldName, boolean defaultValue) {
		String key = prefix + jobName + "." + fieldName;
		String value = environment.getProperty(key);
		if (StringUtils.hasText(value)) {
			return Boolean.valueOf(value);
		}
		return defaultValue;
	}
}