package com.apple.inc.gateway.apirecord.repository;

import com.apple.inc.gateway.apirecord.entity.ApiRecordEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Reactive MongoDB repository for API audit records.
 */
@Repository
public interface ApiRecordRepository extends ReactiveMongoRepository<ApiRecordEntity, String> {
}

