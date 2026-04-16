package com.callibrity.cowork.connector.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamDto(
        @Schema(description = "Unique short name")
        String name,
        @Schema(description = "Human-readable name")
        String displayName,
        @Schema(description = "On-call rotation handle (PagerDuty URL, Slack group, etc.)")
        String onCallRotation,
        @Schema(description = "Primary Slack channel for the team")
        String slackChannel,
        @Schema(description = "Count of services owned by this team")
        long serviceCount) {
}
