package com.precision.mdm.data.model;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AllArgsConstructor;
import lombok.Data;

@PrimaryKeyClass
@Data
@AllArgsConstructor
public class MasterFieldsKey {

	@PrimaryKeyColumn(name = "provider_type", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
	private String providerType;

	@PrimaryKeyColumn(name = "field_name", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
	private String fieldName;

	@PrimaryKeyColumn(name = "field_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
	private Integer fieldId;

}
