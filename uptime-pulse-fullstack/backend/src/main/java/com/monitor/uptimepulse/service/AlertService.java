package com.monitor.uptimepulse.service;

import com.monitor.uptimepulse.entity.Monitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends alert notifications to a tenant-configured Discord webhook whenever
 * a monitor transitions between UP and DOWN states.
 */
@Slf4j
@Service
public class AlertService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int COLOR_DOWN = 15158332; // red
    private static final int COLOR_UP = 3066993;     // green

    private final RestTemplate restTemplate;

    public AlertService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Sends a DOWN alert for the given monitor. Failures are logged but
     * never thrown, so a broken webhook can never interrupt the health
     * check worker loop.
     */
    public void sendDownAlert(Monitor monitor, String reason) {
        sendStatusAlert(monitor, Monitor.Status.DOWN, reason);
    }

    /**
     * Sends a recovery (UP) alert for the given monitor.
     */
    public void sendRecoveryAlert(Monitor monitor) {
        sendStatusAlert(monitor, Monitor.Status.UP, "Endpoint responded successfully again.");
    }

    private void sendStatusAlert(Monitor monitor, Monitor.Status newStatus, String reason) {
        if (monitor.getAlertsEnabled() == null || !monitor.getAlertsEnabled()) {
            return;
        }
        String webhookUrl = monitor.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Alerts enabled for monitor '{}' but no Discord webhook URL is configured.", monitor.getName());
            return;
        }

        try {
            Map<String, Object> payload = buildDiscordPayload(monitor, newStatus, reason);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            org.springframework.http.ResponseEntity<String> response =
                    restTemplate.postForEntity(webhookUrl, request, String.class);

            HttpStatusCode statusCode = response.getStatusCode();
            if (statusCode.is2xxSuccessful()) {
                log.info("Discord alert sent for monitor '{}' (status={})", monitor.getName(), newStatus);
            } else {
                log.warn("Discord webhook for monitor '{}' returned non-2xx status: {}", monitor.getName(), statusCode.value());
            }
        } catch (RestClientException ex) {
            log.error("Failed to send Discord alert for monitor '{}': {}", monitor.getName(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error while sending Discord alert for monitor '{}': {}", monitor.getName(), ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildDiscordPayload(Monitor monitor, Monitor.Status newStatus, String reason) {
        boolean isDown = newStatus == Monitor.Status.DOWN;

        Map<String, Object> embed = new HashMap<>();
        embed.put("title", isDown
                ? ":red_circle: " + monitor.getName() + " is DOWN"
                : ":green_circle: " + monitor.getName() + " has RECOVERED");
        embed.put("description", reason != null ? reason : "");
        embed.put("color", isDown ? COLOR_DOWN : COLOR_UP);

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("Monitor", monitor.getName(), true));
        fields.add(field("URL", monitor.getUrl(), true));
        fields.add(field("Tenant", monitor.getTenantId() != null ? monitor.getTenantId() : "default", true));
        if (monitor.getLastLatencyMs() != null) {
            fields.add(field("Latency", monitor.getLastLatencyMs() + " ms", true));
        }
        if (monitor.getLastStatusCode() != null) {
            fields.add(field("HTTP Status", String.valueOf(monitor.getLastStatusCode()), true));
        }
        fields.add(field("Checked At", java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER), true));
        embed.put("fields", fields);

        Map<String, Object> footer = new HashMap<>();
        footer.put("text", "UptimePulse Monitoring");
        embed.put("footer", footer);

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "UptimePulse");
        payload.put("embeds", List.of(embed));
        return payload;
    }

    private Map<String, Object> field(String name, String value, boolean inline) {
        Map<String, Object> field = new HashMap<>();
        field.put("name", name);
        field.put("value", value == null || value.isBlank() ? "N/A" : value);
        field.put("inline", inline);
        return field;
    }
}
