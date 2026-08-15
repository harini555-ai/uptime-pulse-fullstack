package com.monitor.uptimepulse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single monitored endpoint (API or website) registered by a
 * tenant within UptimePulse. Each monitor is periodically pinged by the
 * {@link com.monitor.uptimepulse.service.UptimeWorkerService} background
 * worker, which appends the results to {@link PingLog}.
 */
@Entity
@Table(name = "monitors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Monitor {

    public enum Status {
        UP,
        DOWN,
        PENDING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Monitor name is required")
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(https?)://[^\\s/$.?#].[^\\s]*$", message = "URL must be a valid http(s) URL")
    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "tenant_id", length = 100)
    private String tenantId = "default";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "check_interval_seconds", nullable = false)
    private Integer checkIntervalSeconds = 30;

    @Column(name = "expected_status_code", nullable = false)
    private Integer expectedStatusCode = 200;

    @Column(name = "discord_webhook_url", length = 2048)
    private String discordWebhookUrl;

    @Column(name = "alerts_enabled", nullable = false)
    private Boolean alertsEnabled = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_status_change_at")
    private LocalDateTime lastStatusChangeAt;

    @Column(name = "last_latency_ms")
    private Long lastLatencyMs;

    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "monitor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PingLog> pingLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lastStatusChangeAt == null) {
            this.lastStatusChangeAt = now;
        }
        if (this.tenantId == null || this.tenantId.isBlank()) {
            this.tenantId = "default";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
