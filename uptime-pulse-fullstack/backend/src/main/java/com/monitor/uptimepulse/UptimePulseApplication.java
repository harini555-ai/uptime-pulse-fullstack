package com.monitor.uptimepulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the UptimePulse Multi-Tenant API Monitoring and Uptime
 * Status Platform.
 *
 * Enables Spring's task scheduling infrastructure so that
 * {@link com.monitor.uptimepulse.service.UptimeWorkerService} can run its
 * periodic health-check job via {@code @Scheduled}.
 */
@SpringBootApplication
@EnableScheduling
public class UptimePulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(UptimePulseApplication.class, args);
    }
}
