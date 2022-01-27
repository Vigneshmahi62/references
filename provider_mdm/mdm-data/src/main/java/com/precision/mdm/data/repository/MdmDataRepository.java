package com.precision.mdm.data.repository;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import com.precision.mdm.data.model.MdmData;
import com.precision.mdm.data.model.MdmDataKey;

public interface MdmDataRepository extends CassandraRepository<MdmData, MdmDataKey> {

	@Query("select * from mdm_data where mdm_id = :mdmId ALLOW FILTERING")
	List<MdmData> findByMdmId(@Param("mdmId") Integer mdmId);
}
