package com.precision.mdm.data.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldsDto {

	private String providerType;

	private String fieldName;

	private Integer fieldId;

	private String dataType;

	private String parentFieldName;

	private String jsonField;

	private String description;

	@JsonSetter(nulls = Nulls.SKIP)
	private boolean isJsonArray = false;
}
