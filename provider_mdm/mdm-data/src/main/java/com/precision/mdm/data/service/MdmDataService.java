package com.precision.mdm.data.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.precision.mdm.data.exception.JsonMappingException;
import com.precision.mdm.data.exception.NoSuchElementFoundException;
import com.precision.mdm.data.model.MdmData;
import com.precision.mdm.data.model.MdmDataKey;
import com.precision.mdm.data.repository.MdmDataRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MdmDataService {

	private final MdmDataRepository mdmDataRepository;

	public final KieContainer kieContainer;

	private final JsonMappingService jsonMappingService;

	private final CassandraOperations cassandraTemplate;

	public String getMdmData() {
		final List<MdmData> mdmDatas = mdmDataRepository.findAll();
		try {
			return jsonMappingService.objectsToJson(mdmDatas);
		} catch (final JsonProcessingException e) {
			throw new JsonMappingException(e.getMessage());
		}
	}

	public String getMdmDataByMdmId(final Integer mdmId) {
		final List<MdmData> mdmDatas = mdmDataRepository.findByMdmId(mdmId);
		if (Objects.nonNull(mdmDatas)) {
			try {
				return jsonMappingService.objectsToJson(mdmDatas);
			} catch (final JsonProcessingException e) {
				throw new JsonMappingException(e.getMessage());
			}
		} else {
			throw new NoSuchElementFoundException(
					String.format("No record found against the MDM Id :: %d", mdmId));
		}
	}

	public String getMdmDataById(final MdmDataKey mdmDataKey) {
		final Optional<MdmData> mdmData = mdmDataRepository.findById(mdmDataKey);
		if (mdmData.isPresent()) {
			try {
				return jsonMappingService.objectsToJson(Arrays.asList(mdmData.get()));
			} catch (final JsonProcessingException e) {
				throw new JsonMappingException(e.getMessage());
			}
		} else
			throw new NoSuchElementFoundException(
					String.format("No record found against the Id :: %s", mdmDataKey));
	}

	public String createMdmData(final String request) {
		List<MdmData> mdmDatas = jsonMappingService.jsonToObject(request);
		final CassandraBatchOperations batchOps = cassandraTemplate.batchOps();
		batchOps.insert(mdmDatas);
		batchOps.execute();

		final KieSession session = kieContainer.newKieSession();
		for (final MdmData mdmData : mdmDatas) {
			session.insert(mdmData);
		}
		session.fireAllRules();
		session.dispose();
		try {
			return jsonMappingService.objectsToJson(mdmDatas);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String updateMdmData(String request) {
		List<MdmData> mdmDatas = jsonMappingService.jsonToObject(request);
		mdmDatas = mdmDataRepository.insert(mdmDatas);
		try {
			return jsonMappingService.objectsToJson(mdmDatas);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String deleteMdmData(final MdmDataKey mdmDataKey) {
		final Optional<MdmData> mdmData = mdmDataRepository.findById(mdmDataKey);
		if (mdmData.isPresent()) {
			mdmDataRepository.deleteById(mdmDataKey);
		} else
			throw new NoSuchElementFoundException(
					String.format("No record found against the Id :: %s", mdmDataKey));
		return mdmDataKey.toString();
	}

	public List<MdmDataKey> deleteMdmData(final Integer mdmId) {
		final List<MdmData> mdmDatas = mdmDataRepository.findByMdmId(mdmId);
		List<MdmDataKey> mdmDataKeys = null;
		if (Objects.nonNull(mdmDatas)) {
			mdmDataKeys = new ArrayList<>();
			for (final MdmData mdmData : mdmDatas) {
				mdmDataKeys.add(mdmData.getMdmDataKey());
			}
			final CassandraBatchOperations batchOps = cassandraTemplate.batchOps();
			batchOps.delete(mdmDataKeys);
			batchOps.execute();
		} else {
			throw new NoSuchElementFoundException(
					String.format("No record found against the MDM Id :: %d", mdmId));
		}
		return mdmDataKeys;
	}

}
