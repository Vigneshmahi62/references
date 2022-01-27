package com.precision.mdm.data.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.precision.mdm.data.annotations.Timer;
import com.precision.mdm.data.exception.InvalidDataException;
import com.precision.mdm.data.model.MasterFields;
import com.precision.mdm.data.model.MasterFieldsKey;
import com.precision.mdm.data.model.MdmData;
import com.precision.mdm.data.model.MdmDataKey;
import com.precision.mdm.data.repository.MasterFieldsRepository;
import com.precision.mdm.data.utils.MandatoryFields;
import com.precision.mdm.data.utils.RowType;

import lombok.RequiredArgsConstructor;

/**
 * This class helps in constructing json to mdm_data & vice-versa.
 * 
 * @author Vignesh
 *
 */
@Service
@RequiredArgsConstructor
public class JsonMappingService {

	private final MasterFieldsRepository masterFieldsRepository;

	private final ObjectMapper objectMapper;

	private static final Class<MdmData> CLASSOBJ = MdmData.class;

	/**
	 * This method helps in organizing & structuring the json from lists of mdm data
	 * 
	 * @param mdmDatas - List of mdm_data
	 * @return Json string
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings("unchecked")
	@Timer
	public String objectsToJson(final List<MdmData> mdmDatas) throws JsonProcessingException {
		final List<Object> rootObj = new ArrayList<>();
		Map<String, Object> jsonMap = null;
		Map<String, Object> obj = null;
		final Map<String, Map<Integer, MasterFields>> masterFieldWithProviderType = new HashMap<>();
		final Map<Integer, List<MdmData>> mdmDataMap = convertToMap(mdmDatas);
		for (final Entry<Integer, List<MdmData>> mdmDataEntry : mdmDataMap.entrySet()) {
			jsonMap = new TreeMap<>();
			final List<MdmData> value = mdmDataEntry.getValue();
			for (MdmData mdmData : value) {
				final String rowType = mdmData.getMdmDataKey().getRowType();
				String providerType = mdmData.getMdmDataKey().getProviderType();
				Map<Integer, MasterFields> masterFieldWithId = getMasterFields(
						masterFieldWithProviderType, providerType);
				if (mdmData.getMdmDataKey().getRowType().equals(RowType.GENERAL.toString())) {
					objectToMap(mdmData, jsonMap, masterFieldWithId);
				} else {
					List<Object> listOfRowType = (List<Object>) jsonMap.get(rowType);
					if (Objects.isNull(listOfRowType)) {
						listOfRowType = new ArrayList<>();
					}
					obj = new TreeMap<>();
					objectToMap(mdmData, obj, masterFieldWithId);
					listOfRowType.add(obj);
					jsonMap.put(rowType, listOfRowType);
				}
			}

			rootObj.add(jsonMap);
		}
		masterFieldWithProviderType.clear();
		if (rootObj.isEmpty()) {
			return "[]";
		} else if (rootObj.size() > 1) {
			return convertToJson(rootObj);
		} else {
			return convertToJson(rootObj.get(0));
		}
	}

	/**
	 * @param masterFieldWithProviderType
	 * @param providerType
	 * @return
	 */
	private Map<Integer, MasterFields> getMasterFields(
			Map<String, Map<Integer, MasterFields>> masterFieldWithProviderType,
			String providerType) {
		Map<Integer, MasterFields> masterFieldWithId = masterFieldWithProviderType
				.get(providerType);
		if (masterFieldWithId == null) {
			masterFieldWithId = getMasterFieldsWithFieldIdAsKey(providerType);
			masterFieldWithProviderType.put(providerType, masterFieldWithId);
		}
		return masterFieldWithId;
	}

	/**
	 * Converts Object to Json String
	 * 
	 * @param object - Object to construct as json string
	 * @return
	 * @throws JsonProcessingException
	 */
	private String convertToJson(final Object object) throws JsonProcessingException {
		return objectMapper.writeValueAsString(object);
	}

	/**
	 * Converts mdm_data to Map. Get the json fields to be converted from
	 * master_fields table.
	 * 
	 * @param mdmData - Model of MDM Data
	 * @param jsonMap
	 * @return
	 */
	private void objectToMap(final MdmData mdmData, final Map<String, Object> jsonObject,
			final Map<Integer, MasterFields> masterFieldWithId) {
		if (Objects.nonNull(mdmData)) {
			final MdmDataKey mdmDataKey = mdmData.getMdmDataKey();
			// Calling @setIdFields to set primary key fields which is static
			setIdFields(mdmDataKey, jsonObject, mdmData.getMdmCreatedTime(),
					mdmData.getMdmUpdatedTime());
			masterFieldWithId.forEach((id, fields) -> {
				try {
					Method method = CLASSOBJ.getDeclaredMethod(getMethodName(id, "get"));
					Object obj = method.invoke(mdmData);
					setJsonKeyAndValue(fields, jsonObject, obj, mdmDataKey.getRowType());
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			});

		}
	}

	/**
	 * Gets the jsonFieldName based on input column name. So, Key is jsonFieldName
	 * and Value is obj
	 * 
	 * @param masterField - Master Fields
	 * @param jsonObject  - Map used to construct json
	 * @param obj         - value to set in map
	 * @param rowType
	 */
	private void setJsonKeyAndValue(final MasterFields masterField,
			final Map<String, Object> jsonObject, Object obj, final String rowType) {
		if (Objects.nonNull(masterField)) {
			final String parentFieldName = masterField.getParentFieldName();
			final Object val = convertToStringFromTimeModule(obj);
			final boolean isJsonArray = masterField.isJsonArray();
			// Check if the value belongs to this key or field
			if (isValueMatchesWithField(parentFieldName, rowType, isJsonArray)) {
				jsonObject.put(masterField.getJsonField(), val);
			}
			// Check if the nested object is applicable to this Key or field
			else if (isNestedApplicableToThisKey(isJsonArray, parentFieldName, rowType)) {
				final String[] parentFieldNames = parentFieldName.split("\\.");
				setNestedJsonValues(0, parentFieldNames.length - 1, parentFieldNames, jsonObject,
						masterField.getJsonField(), val);
			}
		}
	}

	/**
	 * Checks if the key is not jsonArray type and parentFieldName starts with
	 * RowType or it is General RowType
	 * 
	 * @param isJsonArray
	 * @param parentFieldName
	 * @param rowType
	 * @return True, if key is not JSONArray type & RowType equals to General or
	 *         ParentFieldName Starts with RowType
	 */
	private boolean isNestedApplicableToThisKey(final boolean isJsonArray, String parentFieldName,
			final String rowType) {
		// If this value belongs to nested of nested json / nested of json array,
		// parentFieldName should start with 'Parent Key'
		return !isJsonArray && parentFieldName != null
				&& (rowType.equals(RowType.GENERAL.toString())
						|| parentFieldName.startsWith(rowType));
	}

	/**
	 * Checks if the value belongs to thid field or Key
	 * 
	 * @param parentFieldName
	 * @param rowType
	 * @param isJsonArray
	 * @return
	 */
	private boolean isValueMatchesWithField(final String parentFieldName, final String rowType,
			final boolean isJsonArray) {
		return (parentFieldName == null && rowType.equals(RowType.GENERAL.toString()))
				|| (isJsonArray && parentFieldName != null && parentFieldName.equals(rowType));
	}

	/**
	 * This method sets value of nested json objects based on master_field
	 * configuration.
	 * 
	 * @param indexPos
	 * @param endOfIndex
	 * @param parentFieldNames
	 * @param parentJsonObject
	 * @param jsonField
	 * @param val
	 */
	@SuppressWarnings("unchecked")
	private void setNestedJsonValues(int indexPos, final int endOfIndex,
			final String[] parentFieldNames, final Map<String, Object> parentJsonObject,
			final String jsonField, final Object val) {
		Map<String, Object> nestedObj = (Map<String, Object>) parentJsonObject
				.get(parentFieldNames[indexPos]);
		if (nestedObj == null) {
			nestedObj = new HashMap<>();
		}
		if (indexPos == endOfIndex) {
			nestedObj.put(jsonField, val);
		} else {
			indexPos++;
			setNestedJsonValues(indexPos, endOfIndex, parentFieldNames, nestedObj, jsonField, val);
			--indexPos;
		}
		parentJsonObject.put(parentFieldNames[indexPos], nestedObj);
	}

	/**
	 * Return as String if input object is instance of LocalDate/LocalDateTime
	 * 
	 * @param obj
	 * @return Object
	 */
	private Object convertToStringFromTimeModule(Object obj) {
		if (obj != null && (obj instanceof LocalDate || obj instanceof LocalDateTime)) {
			obj = obj.toString();
		}
		return obj;
	}

	/**
	 * Acts as a Helper method for mapAgainstFieldId. Gets the list of masterfields
	 * against the input provider type.
	 * 
	 * @param providerType
	 * @return
	 */
	private Map<Integer, MasterFields> getMasterFieldsWithFieldIdAsKey(final String providerType) {
		final List<MasterFields> masterFields = masterFieldsRepository
				.findByProviderType(providerType);
		return mapAgainstFieldId(masterFields);
	}

	/**
	 * Method to set primary key fields of mdmdata
	 * 
	 * @param mdmDataKey
	 * @param jsonObject
	 * @param createdDateTime
	 * @param updatedDateTime
	 */
	private void setIdFields(final MdmDataKey mdmDataKey, final Map<String, Object> jsonObject,
			final LocalDateTime createdDateTime, final LocalDateTime updatedDateTime) {
		jsonObject.put(MandatoryFields.MDMID.getValue(), mdmDataKey.getMdmId());
		jsonObject.put(MandatoryFields.PROVIDERTYPE.getValue(), mdmDataKey.getProviderType());
		jsonObject.put(MandatoryFields.ROWID.getValue(), mdmDataKey.getRowId().toString());
		jsonObject.put(MandatoryFields.ROWTYPE.getValue(), mdmDataKey.getRowType());
		jsonObject.put(MandatoryFields.MDMCREATEDTIME.getValue(), createdDateTime.toString());
		jsonObject.put(MandatoryFields.MDMUPDATEDTIME.getValue(), updatedDateTime.toString());
	}

	/**
	 * This method creates map with mdmid as key, lists of mdmdata against mdmid as
	 * value
	 * 
	 * @param mdmDatas - Lists of Mdm data
	 * @return Map
	 */
	private Map<Integer, List<MdmData>> convertToMap(final List<MdmData> mdmDatas) {
		final Map<Integer, List<MdmData>> mdmDataMap = new HashMap<>();
		for (final MdmData mdmData : mdmDatas) {
			final Integer mdmId = mdmData.getMdmDataKey().getMdmId();
			List<MdmData> mdmDataList = mdmDataMap.get(mdmId);
			if (Objects.isNull(mdmDataMap.get(mdmId))) {
				mdmDataList = new ArrayList<>();
			}
			mdmDataList.add(mdmData);
			mdmDataMap.put(mdmId, mdmDataList);
		}
		return mdmDataMap;
	}

	/**
	 * This method creates map with fieldid as key, lists of masterfiel against
	 * fieldid as value
	 * 
	 * @param masterFields - Lists of Master fields
	 * @return Map
	 */
	private Map<Integer, MasterFields> mapAgainstFieldId(final List<MasterFields> masterFields) {
		final Map<Integer, MasterFields> toMap = new HashMap<>();
		masterFields.forEach(masterField -> toMap.put(masterField.getMasterFieldsKey().getFieldId(),
				masterField));
		return toMap;
	}

	/**
	 * Parent Method!. Constructs Lists of {@code MdmData} from Json String
	 * 
	 * @param json
	 * @return
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings("unchecked")
	@Timer
	public List<MdmData> jsonToObject(final String json) {
		List<MdmData> mdmDatas = new ArrayList<>();
		try {
			List<Object> rootObj = objectMapper.readValue(json, List.class);
			jsonArraysToListOfMdmData(rootObj, mdmDatas, null, null, null, null, null, "");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			try {
				Map<String, Object> jsonMap = objectMapper.readValue(json, Map.class);
				jsonObjectToListOfMdmData(jsonMap, mdmDatas, null, null, null, null, null, "");
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
		}
		return mdmDatas;
	}

	/**
	 * Iterates the lists of root object and helps in constructing lists of
	 * {@code MdmData}
	 * 
	 * @param rootObj            - List of root object
	 * @param mdmDatas           - List of {@code MdmData}
	 * @param rootProviderType   - provider type of parent json field
	 * @param rootMdmId          - mdm id of parent json field
	 * @param rootRowTypeStr     - row type of parent json field
	 * @param rootMasterFieldMap - parent master field
	 * @param parentKey
	 */
	@SuppressWarnings("unchecked")
	private void jsonArraysToListOfMdmData(final List<Object> rootObj, final List<MdmData> mdmDatas,
			final String rootProviderType, final Integer rootMdmId, final String rootRowTypeStr,
			final Map<String, MasterFields> rootMasterFieldMap,
			final Map<String, List<MasterFields>> rootMasterFieldsWithParentField,
			final String parentKey) {
		for (final Object object : rootObj) {
			Map<String, Object> jsonMap = (Map<String, Object>) object;
			jsonObjectToListOfMdmData(jsonMap, mdmDatas, rootProviderType, rootMdmId,
					rootRowTypeStr, rootMasterFieldMap, rootMasterFieldsWithParentField, parentKey);
		}
	}

	/**
	 * @param jsonMap            - Root Object
	 * @param mdmDatas           - List of {@code MdmData}
	 * @param rootProviderType   - provider type of parent json field
	 * @param rootMdmId          - mdm id of parent json field
	 * @param rootRowTypeStr     - row type of parent json field
	 * @param rootMasterFieldMap - parent master field
	 * @param parentKey
	 */
	private void jsonObjectToListOfMdmData(final Map<String, Object> jsonMap,
			final List<MdmData> mdmDatas, final String rootProviderType, final Integer rootMdmId,
			final String rootRowTypeStr, final Map<String, MasterFields> rootMasterFieldMap,
			final Map<String, List<MasterFields>> rootMasterFieldsWithParentField,
			final String parentKey) {
		String providerType = (String) jsonMap.get(MandatoryFields.PROVIDERTYPE.getValue());
		Map<String, MasterFields> masterFieldMap = null;
		Map<String, List<MasterFields>> masterFieldsWithParentField = null;
		if (providerType == null) {
			providerType = rootProviderType;
			masterFieldMap = rootMasterFieldMap;
			masterFieldsWithParentField = rootMasterFieldsWithParentField;
		} else {
			masterFieldsWithParentField = new HashMap<>();
			masterFieldMap = getMasterFieldsWithJsonFieldNameAsKey(providerType,
					masterFieldsWithParentField);
		}
		Integer mdmId = (Integer) jsonMap.get(MandatoryFields.MDMID.getValue());
		if (mdmId == null) {
			mdmId = rootMdmId;
		}
		String rowTypeStr = (String) jsonMap.get(MandatoryFields.ROWTYPE.getValue());
		if (rowTypeStr == null) {
			rowTypeStr = rootRowTypeStr;
		}

		MdmData mdmData;
		MdmDataKey mdmDataKey;
		try {
			String rowIdStr = (String) jsonMap.get(MandatoryFields.ROWID.getValue());
			UUID rowId = null;
			if (rowIdStr == null) {
				rowId = Uuids.timeBased();
			} else
				rowId = UUID.fromString((String) jsonMap.get(MandatoryFields.ROWID.getValue()));
			mdmDataKey = new MdmDataKey(providerType, mdmId, rowTypeStr, rowId);
			mdmData = new MdmData();
		} catch (NullPointerException e) {
			throw new InvalidDataException("One of the mandatory fields such as "
					+ "providerType, mdmId, rowId, rowType is missing.");
		}
		mdmData.setMdmDataKey(mdmDataKey);

		String createdTime = (String) jsonMap.get(MandatoryFields.MDMCREATEDTIME.getValue());
		LocalDateTime createdDateTime = null;
		if (createdTime == null) {
			createdDateTime = LocalDateTime.now();
		} else {
			createdDateTime = LocalDateTime.parse(createdTime);
		}
		mdmData.setMdmCreatedTime(createdDateTime);

		// Assuming this conversion from json to list of mdmdatas will be called before
		// persisting records into database. So, here assigning new time to
		// mdmUpdatedTime
		mdmData.setMdmUpdatedTime(LocalDateTime.now());

		iterateAndSetColumnValue(jsonMap, masterFieldMap, mdmDatas, providerType, mdmId, mdmData,
				masterFieldsWithParentField, parentKey);
		mdmDatas.add(mdmData);
	}

	/**
	 * Iterates the input json map and helps in assigning column values to
	 * {@code MdmData}
	 * 
	 * @param jsonMap                     - Input Json Map
	 * @param masterFieldMap              - Map of MasterFields
	 * @param mdmDatas                    - List of {@code MdmData}
	 * @param providerType                - Provider Type
	 * @param mdmId                       - Mdm Id
	 * @param mdmData                     - {@code MdmData}
	 * @param masterFieldsWithParentField
	 * @param parentKey
	 */
	private void iterateAndSetColumnValue(final Map<String, Object> jsonMap,
			final Map<String, MasterFields> masterFieldMap, final List<MdmData> mdmDatas,
			final String providerType, final Integer mdmId, final MdmData mdmData,
			Map<String, List<MasterFields>> masterFieldsWithParentField, final String parentKey) {
		for (final Entry<String, Object> entry : jsonMap.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();
			final MasterFields masterField = masterFieldMap.get(parentKey + key);
			if (Objects.isNull(masterField)) {
				if (!isMandatoryField(key)) {
					setNestedJsonOrJsonArray(key, masterFieldsWithParentField, value, mdmDatas,
							providerType, mdmId, masterFieldMap, mdmData);
				}
			} else {
				final MasterFieldsKey masterFieldKey = masterField.getMasterFieldsKey();
				final String methodName = getMethodName(masterFieldKey.getFieldId(), "set");
				setColumnValue(methodName, masterField.getDataType(), mdmData, value);
			}
		}
	}

	/**
	 * This method responsible for setting json array values & nested json using
	 * recursion algorithm
	 * 
	 * @param key
	 * @param masterFieldsWithParentField
	 * @param value
	 * @param mdmDatas
	 * @param providerType
	 * @param mdmId
	 * @param masterFieldMap
	 * @param mdmData
	 */
	@SuppressWarnings("unchecked")
	private void setNestedJsonOrJsonArray(String key,
			Map<String, List<MasterFields>> masterFieldsWithParentField, Object value,
			List<MdmData> mdmDatas, String providerType, Integer mdmId,
			Map<String, MasterFields> masterFieldMap, MdmData mdmData) {
		// Calling jsonArraysToListOfMdmData to parse Json Array
		if (isJsonArray(key, masterFieldsWithParentField)) {
			final List<Object> newRowObj = (List<Object>) value;
			jsonArraysToListOfMdmData(newRowObj, mdmDatas, providerType, mdmId, key, masterFieldMap,
					masterFieldsWithParentField, key + ".");
		}

		// Calling iterateAndSetColumnValue to parse nested json
		else {
			Map<String, Object> nestedJsonMap = null;
			try {
				nestedJsonMap = (Map<String, Object>) value;
			} catch (final Exception e) {
				throw new InvalidDataException("Field :: " + key + " Message :: " + e.getMessage());
			}
			iterateAndSetColumnValue(nestedJsonMap, masterFieldMap, mdmDatas, providerType, mdmId,
					mdmData, masterFieldsWithParentField, key + ".");
		}
	}

	/**
	 * Checks if the input key is of jsonArray
	 * 
	 * @param key
	 * @param masterFieldsWithParentField
	 * @return
	 */
	private boolean isJsonArray(final String key,
			Map<String, List<MasterFields>> masterFieldsWithParentField) {
		return masterFieldsWithParentField.get(key) != null;
	}

	/**
	 * Returns true if input key is one of mandatory fields else false
	 * 
	 * @param key
	 * @return
	 */
	private boolean isMandatoryField(final String key) {
		for (final MandatoryFields mandateField : MandatoryFields.values()) {
			if (mandateField.getValue().equals(key)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Assign value to respective mdm_data column
	 * 
	 * @param fieldId  - Field Id
	 * @param dataType - Cassandra Data type of field id
	 * @param mdmData  - {@code MdmData}
	 * @param object   - Value of the field id
	 */
	private void setColumnValue(final String methodName, final String dataType,
			final MdmData mdmData, final Object object) {
		try {
			final Class<?> classType = getClassType(dataType);
			final Method method = CLASSOBJ.getDeclaredMethod(methodName, classType);
			final Object val = convertToTimeModuleFromObject(classType, object);
			method.invoke(mdmData, val);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new InvalidDataException(
					"Method Name :: " + methodName + " Message :: " + e.getMessage());
		}
	}

	/**
	 * Return as LocalDate or LocalDateTime if input classType is
	 * LocalDate/LocalDateTime
	 * 
	 * @param classType - Class Type
	 * @param object    - Value to set
	 * @return LocalDate or LocalDateTime
	 */
	private Object convertToTimeModuleFromObject(final Class<?> classType, Object object) {
		if (object != null) {
			if (classType == LocalDate.class) {
				object = LocalDate.parse(object.toString());
			} else if (classType == LocalDateTime.class) {
				object = LocalDateTime.parse(object.toString());
			}
		}
		return object;
	}

	/**
	 * Acts as a Helper method for mapAgainstJsonFieldName. Gets the list of
	 * masterfields against the input provider type.
	 * 
	 * @param providerType
	 * @return
	 */
	private Map<String, MasterFields> getMasterFieldsWithJsonFieldNameAsKey(
			final String providerType,
			final Map<String, List<MasterFields>> masterFieldsWithParentField) {
		final List<MasterFields> masterFields = masterFieldsRepository
				.findByProviderType(providerType);
		return mapAgainstJsonFieldName(masterFields, masterFieldsWithParentField);
	}

	/**
	 * Return java class type against cassandra datatype
	 * 
	 * @param dataType
	 * @return
	 */
	private Class<?> getClassType(final String dataType) {
		Class<?> returnType = null;
		switch (dataType) {
		case "text":
			returnType = String.class;
			break;
		case "int":
			returnType = Integer.class;
			break;
		case "boolean":
			returnType = Boolean.class;
			break;
		case "list<int>":
		case "list<text>":
			returnType = List.class;
			break;
		case "uuid":
			returnType = UUID.class;
			break;
		case "date":
			returnType = LocalDate.class;
			break;
		case "timstamp":
			returnType = LocalDateTime.class;
			break;
		default:
			break;
		}
		return returnType;
	}

	/**
	 * Construct method name of {@code MdmData} based on input field id
	 * 
	 * @param fieldId
	 * @return method name
	 */
	private String getMethodName(final Integer fieldId, final String methodNamePrefix) {
		String methodName = methodNamePrefix + "Field_";
		if (fieldId < 10) {
			methodName = methodName + "0" + fieldId;
		} else {
			methodName = methodName + fieldId;
		}
		return methodName;
	}

	/**
	 * This method creates map with fieldid as key, lists of masterfield against
	 * fieldid as value
	 * 
	 * @param masterFields                - Lists of Master fields
	 * @param masterFieldsWithParentField
	 * @return Map
	 */
	private Map<String, MasterFields> mapAgainstJsonFieldName(final List<MasterFields> masterFields,
			final Map<String, List<MasterFields>> masterFieldsWithParentField) {
		final Map<String, MasterFields> toMap = new HashMap<>();
		masterFields.forEach(masterField -> {
			String jsonField = masterField.getJsonField();
			String parentFieldName = masterField.getParentFieldName();
			if (parentFieldName == null)
				toMap.put(jsonField, masterField);
			else
				toMap.put(parentFieldName + "." + jsonField, masterField);
			if (parentFieldName != null && masterField.isJsonArray()) {
				List<MasterFields> masterFieldList = masterFieldsWithParentField
						.get(parentFieldName);
				if (masterFieldList == null) {
					masterFieldList = new ArrayList<>();
				}
				masterFieldList.add(masterField);
				masterFieldsWithParentField.put(parentFieldName, masterFieldList);
			}
		});
		return toMap;
	}

}
