package com.precision.mdm.data.repository;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import com.precision.mdm.data.model.MasterFields;
import com.precision.mdm.data.model.MasterFieldsKey;

public interface MasterFieldsRepository extends CassandraRepository<MasterFields, MasterFieldsKey> {

	@Query("select * from master_fields where provider_type = :providerType")
	List<MasterFields> findByProviderType(@Param("providerType") String providerType);

}
