package com.callibrity.cowork.connector.catalog.mcp;

import com.callibrity.cowork.connector.catalog.domain.LifecycleStage;
import com.callibrity.cowork.connector.catalog.dto.BlastRadiusDto;
import com.callibrity.cowork.connector.catalog.dto.DeprecatedUsageDto;
import com.callibrity.cowork.connector.catalog.dto.RelatedServicesDto;
import com.callibrity.cowork.connector.catalog.dto.ServiceDto;
import com.callibrity.cowork.connector.catalog.dto.ServiceSummaryDto;
import com.callibrity.cowork.connector.catalog.dto.TeamDto;
import com.callibrity.cowork.connector.catalog.dto.TeamSummaryDto;
import com.callibrity.cowork.connector.catalog.service.CatalogService;
import com.callibrity.mocapi.api.tools.ToolMethod;
import com.callibrity.mocapi.api.tools.ToolService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.jwcarman.jpa.pagination.PageDto;
import org.springframework.stereotype.Component;

/**
 * Thin MCP adapter over {@link CatalogService}. Each {@code @ToolMethod} here is a one-liner that
 * delegates to the service layer, so the adapter carries only MCP concerns (tool names, titles,
 * parameter schemas) and the service stays transport-agnostic. A future REST adapter would look
 * symmetrically thin over the same {@code CatalogService}.
 */
@ToolService
@Component
@RequiredArgsConstructor
public class CatalogTools {

    private final CatalogService catalog;

    @ToolMethod(name = "service-lookup", title = "Service Lookup",
            description = "Fetch a single service by its short name, including owner team, tags, lifecycle, and runbook links.")
    public ServiceDto serviceLookup(
            @Schema(description = "Short name of the service, e.g. 'payment-processor'") String name) {
        return catalog.lookupService(name);
    }

    @ToolMethod(name = "team-lookup", title = "Team Lookup",
            description = "Fetch a team by its short name, including on-call rotation and Slack channel.")
    public TeamDto teamLookup(
            @Schema(description = "Short name of the team, e.g. 'platform'") String name) {
        return catalog.lookupTeam(name);
    }

    @ToolMethod(name = "services-list", title = "Services List",
            description = "Paginated list of services, optionally filtered by domain, tag, and lifecycle stage.")
    public PageDto<ServiceSummaryDto> servicesList(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Filter by business domain (e.g. 'checkout'). Omit for all domains.") String domain,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Filter by tag (e.g. 'pii', 'pci', 'soc2-scope'). Omit for all tags.") String tag,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Filter by lifecycle stage: ACTIVE, DEPRECATED, RETIRING. Omit for all.") LifecycleStage lifecycle,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        return catalog.listServices(domain, tag, lifecycle, pageIndex, pageSize);
    }

    @ToolMethod(name = "teams-list", title = "Teams List",
            description = "Paginated list of all teams with the number of services each one owns.")
    public PageDto<TeamSummaryDto> teamsList(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        return catalog.listTeams(pageIndex, pageSize);
    }

    @ToolMethod(name = "service-dependencies", title = "Service Dependencies",
            description = "Services that the given service depends on. Set transitive=true for the full downstream tree.")
    public RelatedServicesDto serviceDependencies(
            @Schema(description = "Short name of the service") String name,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "If true, follow dependency edges recursively. Defaults to false.") Boolean transitive) {
        return catalog.serviceDependencies(name, Boolean.TRUE.equals(transitive));
    }

    @ToolMethod(name = "service-dependents", title = "Service Dependents",
            description = "Services that depend on the given service. Set transitive=true for the full upstream caller tree.")
    public RelatedServicesDto serviceDependents(
            @Schema(description = "Short name of the service") String name,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "If true, follow dependent edges recursively. Defaults to false.") Boolean transitive) {
        return catalog.serviceDependents(name, Boolean.TRUE.equals(transitive));
    }

    @ToolMethod(name = "blast-radius", title = "Blast Radius",
            description = "If this service fails, which services are transitively impacted and which teams would be paged?")
    public BlastRadiusDto blastRadius(
            @Schema(description = "Short name of the service whose failure is being assessed") String name) {
        return catalog.blastRadius(name);
    }

    @ToolMethod(name = "orphaned-services", title = "Orphaned Services",
            description = "Services with no owning team. Ownership gaps are the 'who do we page?' question nobody wants to answer at 2 a.m.")
    public PageDto<ServiceSummaryDto> orphanedServices(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        return catalog.orphanedServices(pageIndex, pageSize);
    }

    @ToolMethod(name = "deprecated-in-use", title = "Deprecated Services Still In Use",
            description = "Deprecated services that something still depends on — the migration backlog the LLM can summarize.")
    public PageDto<DeprecatedUsageDto> deprecatedInUse(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        return catalog.deprecatedInUse(pageIndex, pageSize);
    }
}
