package com.monitor.uptimepulse.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload used when creating or updating a {@link com.monitor.uptimepulse.entity.Monitor}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorRequest {

    @NotBlank(message = "Monitor name is required")
    private String name;

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(https?)://[^\\s/$.?#].[^\\s]*$", message = "URL must be a valid http(s) URL")
    private String url;

    private String tenantId;

    private Integer checkIntervalSeconds;

    private Integer expectedStatusCode;

    private String discordWebhookUrl;

    private Boolean alertsEnabled;

    private Boolean isActive;
}
