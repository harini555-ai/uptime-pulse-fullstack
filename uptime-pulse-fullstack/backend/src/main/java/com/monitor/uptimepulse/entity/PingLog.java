package com.monitor.uptimepulse.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Time-series record of a single health-check ping executed against a
 * {@link Monitor}. Stored in the {@code ping_logs} MySQL table and used to
 * power the latency history chart on the front end.
 */
@Entity
@Table(name = "ping_logs", indexes = {
        @Index(name = "idx_ping_logs_monitor_id_checked_at", columnList = "monitor_id, checked_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = false)
    @JsonIgnoreProperties({"pingLogs"})
    private Monitor monitor;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Monitor.Status result;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    protected void onCreate() {
        if (this.checkedAt == null) {
            this.checkedAt = LocalDateTime.now();
        }
    }
}
