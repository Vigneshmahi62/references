package com.precision.mdm.data.model;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Data;

@Table(value = "master_fields")
@Data
public class MasterFields {

	@PrimaryKey
	private MasterFieldsKey masterFieldsKey;

	@Column(value = "data_type")
	private String dataType;

	@Column(value = "parent_field_name")
	private String parentFieldName;

	@Column(value = "json_field")
	private String jsonField;

	@Column(value = "description")
	private String description;
	
	@Column(value = "is_json_array")
	private boolean isJsonArray;
}
