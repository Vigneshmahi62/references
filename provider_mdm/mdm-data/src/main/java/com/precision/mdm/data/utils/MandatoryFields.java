package com.precision.mdm.data.utils;

/**
 * Mandatory Fields of Mdm data
 * 
 * @author Vignesh
 *
 */
public enum MandatoryFields {
	PROVIDERTYPE("providerType"), MDMID("mdmId"), ROWID("rowId"), ROWTYPE("rowType"),
	MDMCREATEDTIME("mdmCreatedTime"), MDMUPDATEDTIME("mdmUpdatedTime");

	MandatoryFields(final String field) {
		this.field = field;
	}

	private String field;

	public String getValue() {
		return this.field;
	}
}
