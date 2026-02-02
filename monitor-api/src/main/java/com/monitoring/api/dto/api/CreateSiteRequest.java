package com.monitoring.api.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSiteRequest(
    @NotBlank(message = "Name is required")
    String name,
    
    @NotBlank(message = "Base URL is required")
    String baseUrl,
    
    @NotNull(message = "Enabled status is required")
    Boolean enabled,
    
    Integer frequencySeconds
) {}
