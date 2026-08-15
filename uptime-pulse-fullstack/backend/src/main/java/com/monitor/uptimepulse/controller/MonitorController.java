package com.monitor.uptimepulse.controller;

import com.monitor.uptimepulse.entity.Monitor;
import com.monitor.uptimepulse.entity.PingLog;
import com.monitor.uptimepulse.repository.MonitorRepository;
import com.monitor.uptimepulse.repository.PingLogRepository;
import com.monitor.uptimepulse.service.UptimeWorkerService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * REST controller exposing full CRUD operations for {@link Monitor}
 * resources, along with endpoints for retrieving latency/ping-log history
 * and manually triggering an immediate health check.
 */
@Slf4j
@RestController
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorRepository monitorRepository;
    private final PingLogRepository pingLogRepository;
    private final UptimeWorkerService uptimeWorkerService;

    public MonitorController(MonitorRepository monitorRepository,
                              PingLogRepository pingLogRepository,
                              UptimeWorkerService uptimeWorkerService) {
        this.monitorRepository = monitorRepository;
        this.pingLogRepository = pingLogRepository;
        this.uptimeWorkerService = uptimeWorkerService;
    }

    /**
     * Returns every registered monitor, optionally filtered by tenant.
     */
    @GetMapping
    public ResponseEntity<List<Monitor>> getAllMonitors(
            @RequestParam(required = false) String tenantId) {
        List<Monitor> monitors;
        if (tenantId != null && !tenantId.isBlank()) {
            monitors = monitorRepository.findByTenantId(tenantId);
        } else {
            monitors = monitorRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return ResponseEntity.ok(monitors);
    }

    /**
     * Returns a single monitor by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Monitor> getMonitorById(@PathVariable Long id) {
        Monitor monitor = findMonitorOrThrow(id);
        return ResponseEntity.ok(monitor);
    }

    /**
     * Creates a new monitor. The initial status is PENDING until the
     * background worker performs its first health check.
     */
    @PostMapping
    public ResponseEntity<Monitor> createMonitor(@Valid @RequestBody MonitorRequest request) {
        Monitor monitor = new Monitor();
        applyRequestToMonitor(request, monitor);
        monitor.setStatus(Monitor.Status.PENDING);
        monitor.setConsecutiveFailures(0);

        Monitor saved = monitorRepository.save(monitor);
        log.info("Created new monitor '{}' (id={}) for URL {}", saved.getName(), saved.getId(), saved.getUrl());

        triggerImmediateCheckAsync(saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Updates an existing monitor's configuration.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Monitor> updateMonitor(@PathVariable Long id,
                                                  @Valid @RequestBody MonitorRequest request) {
        Monitor monitor = findMonitorOrThrow(id);
        applyRequestToMonitor(request, monitor);
        Monitor saved = monitorRepository.save(monitor);
        log.info("Updated monitor '{}' (id={})", saved.getName(), saved.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * Partially updates a monitor's active/alerting flags without requiring
     * a full payload.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Monitor> patchMonitor(@PathVariable Long id,
                                                 @RequestBody Map<String, Object> updates) {
        Monitor monitor = findMonitorOrThrow(id);

        if (updates.containsKey("isActive")) {
            monitor.setIsActive(Boolean.valueOf(String.valueOf(updates.get("isActive"))));
        }
        if (updates.containsKey("alertsEnabled")) {
            monitor.setAlertsEnabled(Boolean.valueOf(String.valueOf(updates.get("alertsEnabled"))));
        }
        if (updates.containsKey("discordWebhookUrl")) {
            Object value = updates.get("discordWebhookUrl");
            monitor.setDiscordWebhookUrl(value != null ? String.valueOf(value) : null);
        }
        if (updates.containsKey("name")) {
            monitor.setName(String.valueOf(updates.get("name")));
        }

        Monitor saved = monitorRepository.save(monitor);
        return ResponseEntity.ok(saved);
    }

    /**
     * Deletes a monitor along with all of its associated ping_log history
     * (cascade configured on the entity relationship).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {
        Monitor monitor = findMonitorOrThrow(id);
        monitorRepository.delete(monitor);
        log.info("Deleted monitor '{}' (id={})", monitor.getName(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the most recent ping_log entries for a monitor, most recent
     * first, capped at the given limit (default 100).
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<PingLog>> getMonitorHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "100") int limit) {
        findMonitorOrThrow(id);
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        List<PingLog> logs = pingLogRepository.findByMonitorIdOrderByCheckedAtDesc(
                id, PageRequest.of(0, safeLimit));
        return ResponseEntity.ok(logs);
    }

    /**
     * Returns aggregate uptime statistics for a monitor over its recorded
     * history, used to render an uptime percentage badge on the dashboard.
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getMonitorStats(@PathVariable Long id) {
        Monitor monitor = findMonitorOrThrow(id);

        long totalChecks = pingLogRepository.countByMonitorId(id);
        long upChecks = pingLogRepository.countByMonitorIdAndResult(id, Monitor.Status.UP);

        double uptimePercentage = totalChecks == 0 ? 100.0 : (upChecks * 100.0) / totalChecks;

        Map<String, Object> stats = new HashMap<>();
        stats.put("monitorId", monitor.getId());
        stats.put("totalChecks", totalChecks);
        stats.put("upChecks", upChecks);
        stats.put("downChecks", totalChecks - upChecks);
        stats.put("uptimePercentage", Math.round(uptimePercentage * 100.0) / 100.0);
        stats.put("currentStatus", monitor.getStatus());
        stats.put("lastCheckedAt", monitor.getLastCheckedAt());

        return ResponseEntity.ok(stats);
    }

    /**
     * Manually triggers an immediate health check for a single monitor,
     * bypassing the scheduled 30-second cycle. Runs synchronously so the
     * caller receives the freshly updated monitor state.
     */
    @PostMapping("/{id}/check-now")
    public ResponseEntity<Monitor> checkNow(@PathVariable Long id) {
        findMonitorOrThrow(id);
        uptimeWorkerService.checkMonitor(id);
        Monitor refreshed = findMonitorOrThrow(id);
        return ResponseEntity.ok(refreshed);
    }

    /**
     * Lightweight summary endpoint used to populate dashboard header
     * counters (total / up / down / pending).
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(required = false) String tenantId) {
        List<Monitor> monitors = (tenantId != null && !tenantId.isBlank())
                ? monitorRepository.findByTenantId(tenantId)
                : monitorRepository.findAll();

        Map<Monitor.Status, Long> counts = monitors.stream()
                .collect(Collectors.groupingBy(Monitor::getStatus, Collectors.counting()));

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", monitors.size());
        summary.put("up", counts.getOrDefault(Monitor.Status.UP, 0L));
        summary.put("down", counts.getOrDefault(Monitor.Status.DOWN, 0L));
        summary.put("pending", counts.getOrDefault(Monitor.Status.PENDING, 0L));
        summary.put("generatedAt", LocalDateTime.now());

        return ResponseEntity.ok(summary);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Monitor findMonitorOrThrow(Long id) {
        return monitorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Monitor not found with id: " + id));
    }

    private void applyRequestToMonitor(MonitorRequest request, Monitor monitor) {
        monitor.setName(request.getName());
        monitor.setUrl(request.getUrl());
        monitor.setTenantId(request.getTenantId() != null && !request.getTenantId().isBlank()
                ? request.getTenantId() : "default");
        monitor.setCheckIntervalSeconds(
                request.getCheckIntervalSeconds() != null ? request.getCheckIntervalSeconds() : 30);
        monitor.setExpectedStatusCode(
                request.getExpectedStatusCode() != null ? request.getExpectedStatusCode() : 200);
        monitor.setDiscordWebhookUrl(request.getDiscordWebhookUrl());
        monitor.setAlertsEnabled(request.getAlertsEnabled() != null && request.getAlertsEnabled());
        monitor.setIsActive(request.getIsActive() == null || request.getIsActive());
    }

    /**
     * Fires off a check for a newly created monitor on a background thread
     * so the API response returns immediately without waiting on network
     * I/O to the target URL.
     */
    private void triggerImmediateCheckAsync(Long monitorId) {
        CompletableFuture.runAsync(() -> {
            try {
                uptimeWorkerService.checkMonitor(monitorId);
            } catch (Exception ex) {
                log.warn("Initial health check failed for new monitor id={}: {}", monitorId, ex.getMessage());
            }
        }, Executors.newSingleThreadExecutor());
    }
}
