package com.chuangxin.elastic.job.service;

import com.chuangxin.elastic.job.model.ElasticJob;

/**
 * @Description:
 * @Author: yangjie
 * @Date: 2018/11/27 下午6:53
 */
public interface DynamicElasticJobService {
	/**
	 * 动态添加定时任务
	 *
	 * @param elasticJob
	 * @return
	 */
	boolean addElasticJob(ElasticJob elasticJob);

	/**
	 * 删除定时任务
	 *
	 * @param elasticJob
	 * @return
	 */
	boolean deleteElasticJob(ElasticJob elasticJob);
}