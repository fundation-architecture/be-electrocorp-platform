package com.electrocorp.electrocorpplatform.reporting.infrastructure.persistence.jpa.repositories;

import com.electrocorp.electrocorpplatform.reporting.domain.model.aggregates.ReportingEvent;
import com.electrocorp.electrocorpplatform.reporting.domain.repositories.ReportingEventRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaReportingEventRepository extends JpaRepository<ReportingEvent, Long>, ReportingEventRepository {
}
