package com.precision.mdm.data.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Service;

import com.precision.mdm.data.dto.FieldsDto;
import com.precision.mdm.data.exception.NoSuchElementFoundException;
import com.precision.mdm.data.model.MasterFields;
import com.precision.mdm.data.model.MasterFieldsKey;
import com.precision.mdm.data.repository.MasterFieldsRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class of {@code MasterFields}. This class interacts with DAO layer
 * and returns response to controller class.
 * 
 * @author Vignesh
 *
 */
@Service
@RequiredArgsConstructor
public class MasterFieldsService {

	private final MasterFieldsRepository masterFieldsRepository;

	private final ModelMapper modelMapper;

	private final CassandraOperations cassandraTemplate;

	/**
	 * Gets the lists of {@link MasterFields} and returns as lists of
	 * {@link FieldsDto}
	 * 
	 * @return Lists of Fields
	 */
	public List<FieldsDto> getMasterFields() {
		return convertToDtos(masterFieldsRepository.findAll());
	}

	/**
	 * Finds the {@link MasterFields} based on input primary key
	 * {@link MasterFieldsKey}. Returns the {@link FieldsDto}
	 * 
	 * @param masterFieldsKey - Input Primary Key
	 * @return Fields
	 */
	public FieldsDto getMasterFieldsById(final MasterFieldsKey masterFieldsKey) {
		Optional<MasterFields> masterFields = masterFieldsRepository.findById(masterFieldsKey);
		if (!masterFields.isPresent()) {
			throw new NoSuchElementFoundException(
					String.format("No record found against the Id :: %s", masterFieldsKey));
		}
		return convertToDto(masterFields.get());
	}

	/**
	 * Inserts the lists of {@link MasterFields}
	 * 
	 * @param fieldsDto - Lists of {@link FieldsDto}
	 * @return List of created Fields
	 */
	public List<FieldsDto> createMasterFields(final List<FieldsDto> fieldsDto) {
		List<MasterFields> masterFields = convertToEntities(fieldsDto);
		final CassandraBatchOperations batchOps = cassandraTemplate.batchOps();
		batchOps.insert(masterFields);
		batchOps.execute();
		return fieldsDto;
	}

	/**
	 * Updates the {@link MasterFields}
	 * 
	 * @param fieldsDto - Dto of {@link MasterFields}
	 * @return Id of the {@link MasterFields}
	 */
	public List<FieldsDto> updateMasterFields(final List<FieldsDto> fieldsDtos) {
		final List<MasterFields> masterFields = convertToEntities(fieldsDtos);
		final CassandraBatchOperations batchOps = cassandraTemplate.batchOps();
		batchOps.update(masterFields);
		batchOps.execute();
		return fieldsDtos;
	}

	/**
	 * Deletes the {@link MasterFields}
	 * 
	 * @param masterFieldsKey - Input Primary Key
	 * @return Id of the {@link MasterFields}
	 */
	public String deleteMasterFields(final MasterFieldsKey masterFieldsKey) {
		if (Objects.nonNull(getMasterFieldsById(masterFieldsKey))) {
			masterFieldsRepository.deleteById(masterFieldsKey);
		}
		return masterFieldsKey.toString();
	}

	/**
	 * Converts the Lists of DTO to Lists of {@link MasterFields}
	 * 
	 * @param fieldsDtos - List of {@link FieldsDto}
	 * @return Lists of {@link MasterFields}
	 */
	private List<MasterFields> convertToEntities(final List<FieldsDto> fieldsDtos) {
		final List<MasterFields> masterFields = new ArrayList<>();
		for (final FieldsDto fieldsDto : fieldsDtos) {
			final MasterFields masterField = convertToEntity(fieldsDto);
			masterFields.add(masterField);
		}
		return masterFields;
	}

	/**
	 * Converts the DTO to {@link MasterFields}
	 * 
	 * @param fieldsDto - {@link FieldsDto}
	 * @return {@link MasterFields}
	 */
	private MasterFields convertToEntity(final FieldsDto fieldsDto) {
		final MasterFields masterField = modelMapper.map(fieldsDto, MasterFields.class);
		final MasterFieldsKey masterFieldsKey = new MasterFieldsKey(fieldsDto.getProviderType(),
				fieldsDto.getFieldName(), fieldsDto.getFieldId());
		masterField.setJsonField(getJsonField(fieldsDto.getFieldName().toLowerCase()));
		masterField.setMasterFieldsKey(masterFieldsKey);
		return masterField;
	}

	private String getJsonField(final String str) {
		final StringBuilder s = new StringBuilder();
		char ch = 0;
		for (int i = 0; i < str.length(); i++) {
			if (ch == ' ' && str.charAt(i) != ' ')
				s.append(Character.toUpperCase(str.charAt(i)));
			else if (str.charAt(i) != ' ')
				s.append(str.charAt(i));
			ch = str.charAt(i);
		}
		return s.toString().trim();
	}

	/**
	 * Converts the Lists of {@link MasterFields} to Lists of DTOs
	 * 
	 * @param masterFields - Lists of {@link MasterFields}
	 * @return Lists of {@link FieldsDto}
	 */
	private List<FieldsDto> convertToDtos(final List<MasterFields> masterFields) {
		final List<FieldsDto> fieldsDtos = new ArrayList<>();
		for (final MasterFields masterField : masterFields) {
			final FieldsDto fieldDto = convertToDto(masterField);
			fieldsDtos.add(fieldDto);
		}
		return fieldsDtos;
	}

	/**
	 * Converts the {@link MasterFields} to DTO
	 * 
	 * @param masterField - {@link MasterFields}
	 * @return {@link FieldsDto}
	 */
	private FieldsDto convertToDto(final MasterFields masterField) {
		final FieldsDto fieldDto = modelMapper.map(masterField, FieldsDto.class);
		final MasterFieldsKey masterFieldsKey = masterField.getMasterFieldsKey();
		fieldDto.setProviderType(masterFieldsKey.getProviderType());
		fieldDto.setFieldName(masterFieldsKey.getFieldName());
		fieldDto.setFieldId(masterFieldsKey.getFieldId());
		return fieldDto;
	}

}
