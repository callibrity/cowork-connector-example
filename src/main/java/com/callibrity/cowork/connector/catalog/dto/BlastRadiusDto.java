package com.callibrity.cowork.connector.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BlastRadiusDto(
        @Schema(description = "Service whose failure is being assessed")
        ServiceSummaryDto target,
        @Schema(description = "All services that would see a transitive impact, in breadth-first order")
        List<ServiceSummaryDto> impactedServices,
        @Schema(description = "Teams that would be paged, grouped by which services of theirs are impacted")
        List<TeamImpactDto> teamsAffected,
        @Schema(description = "Count of orphaned services in the blast radius — each one has no on-call to page")
        int orphanedImpactedCount) {
}
