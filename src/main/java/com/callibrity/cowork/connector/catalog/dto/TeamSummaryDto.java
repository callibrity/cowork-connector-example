package com.callibrity.cowork.connector.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamSummaryDto(
        @Schema(description = "Unique short name")
        String name,
        @Schema(description = "Human-readable name")
        String displayName,
        @Schema(description = "Count of services owned by this team")
        long serviceCount) {
}
