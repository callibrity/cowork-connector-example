package com.callibrity.cowork.connector.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record DeprecatedUsageDto(
        @Schema(description = "The deprecated service that is still being called")
        ServiceSummaryDto deprecatedService,
        @Schema(description = "Active services that still depend on the deprecated one — migration backlog")
        List<ServiceSummaryDto> callers) {
}
