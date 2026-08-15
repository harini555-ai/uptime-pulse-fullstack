package com.monitor.uptimepulse.repository;

import com.monitor.uptimepulse.entity.PingLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link PingLog} time-series records.
 */
@Repository
public interface PingLogRepository extends JpaRepository<PingLog, Long> {

    List<PingLog> findByMonitorIdOrderByCheckedAtDesc(Long monitorId, Pageable pageable);

    List<PingLog> findByMonitorIdAndCheckedAtAfterOrderByCheckedAtAsc(Long monitorId, LocalDateTime after);

    @Modifying
    @Query("DELETE FROM PingLog p WHERE p.checkedAt < :cutoff")
    int deleteByCheckedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    long countByMonitorIdAndResult(Long monitorId, com.monitor.uptimepulse.entity.Monitor.Status result);

    long countByMonitorId(Long monitorId);
}
