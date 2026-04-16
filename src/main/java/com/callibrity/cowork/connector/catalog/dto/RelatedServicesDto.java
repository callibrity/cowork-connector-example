package com.callibrity.cowork.connector.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RelatedServicesDto(
        @Schema(description = "Service that the traversal started from")
        String rootService,
        @Schema(description = "True if the traversal followed edges recursively; false if only direct neighbors were returned")
        boolean transitive,
        @Schema(description = "Related services — either dependencies or dependents depending on which tool was called")
        List<ServiceSummaryDto> services) {
}
