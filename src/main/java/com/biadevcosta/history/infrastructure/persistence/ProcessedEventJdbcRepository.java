package com.biadevcosta.history.infrastructure.persistence;

import org.springframework.data.repository.CrudRepository;

/** Spring Data JDBC repository over {@link ProcessedEventEntity}. */
public interface ProcessedEventJdbcRepository extends CrudRepository<ProcessedEventEntity, String> {
}
