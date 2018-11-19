package com.chuangxin.elastic.job.enums;

import lombok.Getter;

/**
 * @Description:
 * @Author: yangjie
 * @Date: 2018/11/19 下午7:41
 */
@Getter
public enum ElasticJobTypeName {
	SIMPLE_JOB("SimpleJob"), DATA_FLOW_JOB("DataflowJob"), SCRIPT_JOB("ScriptJob");
	private String typeName;

	ElasticJobTypeName(String typeName) {
		this.typeName = typeName;
	}

	public static ElasticJobTypeName getElasticJobByTypeName(String typeName) {
		for (ElasticJobTypeName ite : ElasticJobTypeName.values()) {
			if (typeName.equals(ite.getTypeName())) {
				return ite;
			}
		}
		return null;
	}
}