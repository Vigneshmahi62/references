package com.precision.mdm.data.model;

import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AllArgsConstructor;
import lombok.Data;

@PrimaryKeyClass
@Data
@AllArgsConstructor
public class MdmDataKey {

	@PrimaryKeyColumn(name = "provider_type", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
	@NotNull
	private String providerType;

	@PrimaryKeyColumn(name = "mdm_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
	@NotNull
	private int mdmId;

	@PrimaryKeyColumn(name = "row_type", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
	@NotNull
	private String rowType;

	@PrimaryKeyColumn(name = "row_id", ordinal = 3, type = PrimaryKeyType.CLUSTERED)
	@NotNull
	private UUID rowId;
}
