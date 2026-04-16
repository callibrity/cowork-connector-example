package com.callibrity.cowork.connector.catalog.dto;

import com.callibrity.cowork.connector.catalog.domain.LifecycleStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

public record ServiceDto(
        @Schema(description = "Unique short name")
        String name,
        @Schema(description = "Human-readable name")
        String displayName,
        @Schema(description = "Longer description of the service's purpose")
        String description,
        @Schema(description = "Business domain")
        String domain,
        @Schema(description = "Owning team summary, or null if orphaned")
        TeamSummaryDto owner,
        @Schema(description = "Current lifecycle stage")
        LifecycleStage lifecycleStage,
        @Schema(description = "Source repository URL")
        String repoUrl,
        @Schema(description = "Runbook URL")
        String runbookUrl,
        @Schema(description = "Tags such as 'pii', 'pci', 'soc2-scope', 'customer-facing'")
        Set<String> tags,
        @Schema(description = "Count of direct outbound dependencies")
        int directDependencyCount,
        @Schema(description = "Count of direct inbound dependents")
        int directDependentCount) {
}
