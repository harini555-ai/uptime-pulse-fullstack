package com.monitor.uptimepulse.service;

import com.monitor.uptimepulse.entity.Monitor;
import com.monitor.uptimepulse.entity.PingLog;
import com.monitor.uptimepulse.repository.MonitorRepository;
import com.monitor.uptimepulse.repository.PingLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Background worker that periodically pings every active {@link Monitor}
 * registered in the system, records the outcome as a {@link PingLog}
 * time-series entry, transitions monitor status between UP/DOWN, and fires
 * Discord alerts on state changes.
 *
 * Health checks for individual monitors are dispatched concurrently on a
 * dedicated executor so that a single slow/unreachable endpoint cannot
 * delay the checking of every other monitor in the fleet.
 */
@Slf4j
@Service
public class UptimeWorkerService {

    private final MonitorRepository monitorRepository;
    private final PingLogRepository pingLogRepository;
    private final AlertService alertService;
    private final RestTemplate restTemplate;

    @Value("${uptimepulse.worker.failure-threshold:1}")
    private int failureThreshold;

    @Value("${uptimepulse.worker.retention-days:14}")
    private int retentionDays;

    private final ExecutorService checkExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2));

    public UptimeWorkerService(MonitorRepository monitorRepository,
                                PingLogRepository pingLogRepository,
                                AlertService alertService,
                                RestTemplate restTemplate) {
        this.monitorRepository = monitorRepository;
        this.pingLogRepository = pingLogRepository;
        this.alertService = alertService;
        this.restTemplate = restTemplate;
    }

    /**
     * Main scheduled entry point. Runs every {@code uptimepulse.worker.fixed-delay-ms}
     * milliseconds (default 30 seconds), fanning out a health check for
     * every active monitor.
     */
    @Scheduled(fixedDelayString = "${uptimepulse.worker.fixed-delay-ms:30000}", initialDelayString = "5000")
    public void runScheduledHealthChecks() {
        List<Monitor> activeMonitors = monitorRepository.findByIsActiveTrue();
        if (activeMonitors.isEmpty()) {
            log.debug("No active monitors registered. Skipping health-check cycle.");
            return;
        }

        log.info("Starting health-check cycle for {} active monitor(s).", activeMonitors.size());

        List<CompletableFuture<Void>> futures = activeMonitors.stream()
                .map(monitor -> CompletableFuture.runAsync(() -> safelyCheckMonitor(monitor.getId()), checkExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Completed health-check cycle for {} monitor(s).", activeMonitors.size());
    }

    /**
     * Prunes ping_logs older than the configured retention window once a
     * day, keeping the time-series table from growing unbounded.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void pruneOldPingLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = pingLogRepository.deleteByCheckedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Pruned {} ping_log entries older than {} days.", deleted, retentionDays);
        }
    }

    private void safelyCheckMonitor(Long monitorId) {
        try {
            checkMonitor(monitorId);
        } catch (Exception ex) {
            log.error("Unexpected failure while checking monitor id={}: {}", monitorId, ex.getMessage(), ex);
        }
    }

    @Transactional
    public void checkMonitor(Long monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId).orElse(null);
        if (monitor == null || monitor.getIsActive() == null || !monitor.getIsActive()) {
            return;
        }

        long startTime = System.nanoTime();
        Integer statusCode = null;
        String errorMessage = null;
        boolean success;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(monitor.getUrl(), String.class);
            HttpStatusCode httpStatus = response.getStatusCode();
            statusCode = httpStatus.value();
            success = statusCode.equals(monitor.getExpectedStatusCode()) || httpStatus.is2xxSuccessful();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            statusCode = ex.getStatusCode().value();
            errorMessage = "HTTP error: " + ex.getStatusCode().value() + " " + ex.getStatusText();
            success = statusCode.equals(monitor.getExpectedStatusCode());
        } catch (ResourceAccessException ex) {
            errorMessage = "Connection failed or timed out: " + rootCauseMessage(ex);
            success = false;
        } catch (RestClientException ex) {
            errorMessage = "Request failed: " + rootCauseMessage(ex);
            success = false;
        } catch (Exception ex) {
            errorMessage = "Unexpected error: " + rootCauseMessage(ex);
            success = false;
        }

        long elapsedMs = Duration.ofNanos(System.nanoTime() - startTime).toMillis();

        persistResultAndUpdateMonitor(monitor, success, statusCode, elapsedMs, errorMessage);
    }

    private void persistResultAndUpdateMonitor(Monitor monitor,
                                                boolean success,
                                                Integer statusCode,
                                                long elapsedMs,
                                                String errorMessage) {
        Monitor.Status previousStatus = monitor.getStatus();
        Monitor.Status newResult = success ? Monitor.Status.UP : Monitor.Status.DOWN;

        PingLog pingLog = new PingLog();
        pingLog.setMonitor(monitor);
        pingLog.setStatusCode(statusCode);
        pingLog.setLatencyMs(elapsedMs);
        pingLog.setResult(newResult);
        pingLog.setErrorMessage(errorMessage);
        pingLog.setCheckedAt(LocalDateTime.now());
        pingLogRepository.save(pingLog);

        monitor.setLastCheckedAt(LocalDateTime.now());
        monitor.setLastLatencyMs(elapsedMs);
        monitor.setLastStatusCode(statusCode);

        if (success) {
            monitor.setConsecutiveFailures(0);
            if (previousStatus != Monitor.Status.UP) {
                monitor.setStatus(Monitor.Status.UP);
                monitor.setLastStatusChangeAt(LocalDateTime.now());
                monitorRepository.save(monitor);
                if (previousStatus == Monitor.Status.DOWN) {
                    alertService.sendRecoveryAlert(monitor);
                }
                log.info("Monitor '{}' transitioned {} -> UP", monitor.getName(), previousStatus);
                return;
            }
        } else {
            int failures = (monitor.getConsecutiveFailures() == null ? 0 : monitor.getConsecutiveFailures()) + 1;
            monitor.setConsecutiveFailures(failures);

            if (failures >= failureThreshold && previousStatus != Monitor.Status.DOWN) {
                monitor.setStatus(Monitor.Status.DOWN);
                monitor.setLastStatusChangeAt(LocalDateTime.now());
                monitorRepository.save(monitor);
                String reason = errorMessage != null
                        ? errorMessage
                        : "Received unexpected HTTP status code: " + statusCode;
                alertService.sendDownAlert(monitor, reason);
                log.warn("Monitor '{}' transitioned {} -> DOWN ({})", monitor.getName(), previousStatus, reason);
                return;
            }
        }

        monitorRepository.save(monitor);
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message != null ? message : cause.getClass().getSimpleName();
    }

    /**
     * Gracefully shuts down the internal executor when the Spring context
     * closes, preventing thread leaks between application restarts.
     */
    @jakarta.annotation.PreDestroy
    public void shutdownExecutor() {
        checkExecutor.shutdown();
        try {
            if (!checkExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                checkExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            checkExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
