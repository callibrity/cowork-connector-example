package com.callibrity.cowork.connector.catalog.dto;

import com.callibrity.cowork.connector.catalog.domain.LifecycleStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

public record ServiceSummaryDto(
        @Schema(description = "Unique short name used in dependency references")
        String name,
        @Schema(description = "Human-readable name")
        String displayName,
        @Schema(description = "Business domain the service belongs to")
        String domain,
        @Schema(description = "Short name of the owning team, or null for orphaned services")
        String ownerTeam,
        @Schema(description = "Current lifecycle stage")
        LifecycleStage lifecycleStage,
        @Schema(description = "Tags such as 'pii', 'pci', 'soc2-scope', 'customer-facing'")
        Set<String> tags) {
}
