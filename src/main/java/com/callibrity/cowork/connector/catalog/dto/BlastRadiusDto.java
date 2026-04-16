package com.callibrity.cowork.connector.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BlastRadiusDto(
        @Schema(description = "Short name of the service whose failure is being assessed (echoed from the request for LLM context; call service-lookup if you need the full record)")
        String target,
        @Schema(description = "Services transitively impacted if the target fails, one row per service with owner team and on-call info inline so the LLM can group or filter however the question wants")
        List<ImpactedServiceDetail> impactedServices,
        @Schema(description = "Count of orphaned services in the blast radius — each one has no on-call to page")
        int orphanedImpactedCount) {
}
