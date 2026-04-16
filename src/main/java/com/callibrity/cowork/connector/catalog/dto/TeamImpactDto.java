package com.callibrity.cowork.connector.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TeamImpactDto(
        @Schema(description = "Team that would be paged")
        TeamSummaryDto team,
        @Schema(description = "On-call rotation handle")
        String onCallRotation,
        @Schema(description = "Slack channel")
        String slackChannel,
        @Schema(description = "Services owned by this team that are in the blast radius")
        List<ServiceSummaryDto> impactedServices) {
}
