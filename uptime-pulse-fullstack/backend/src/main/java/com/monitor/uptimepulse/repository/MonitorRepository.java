package com.monitor.uptimepulse.repository;

import com.monitor.uptimepulse.entity.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Monitor} entities.
 */
@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findByIsActiveTrue();

    List<Monitor> findByTenantId(@Param("tenantId") String tenantId);

    List<Monitor> findByTenantIdAndIsActiveTrue(@Param("tenantId") String tenantId);

    List<Monitor> findByStatus(Monitor.Status status);
}
