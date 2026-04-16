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
            description = """
                    Look up a single service by short name. Returns the owner team, business domain,
                    lifecycle stage (ACTIVE / DEPRECATED / RETIRING), compliance tags (pii, pci,
                    soc2-scope, customer-facing, gdpr), repo URL, runbook URL, and direct
                    dependency / dependent counts. Use this when asked about a specific service —
                    who owns it, what it does, whether it is still active, or where its code and
                    runbook live.""")
    public ServiceDto serviceLookup(
            @Schema(description = "Short name of the service, e.g. 'payment-processor'") String name) {
        return catalog.lookupService(name);
    }

    @ToolMethod(name = "team-lookup", title = "Team Lookup",
            description = """
                    Look up a team by short name. Returns the on-call rotation handle (PagerDuty,
                    Slack group, etc.), the team's primary Slack channel, and the number of
                    services the team owns. Use this when asked who to contact, who is on-call,
                    which Slack to ping, or how big a team's surface area is.""")
    public TeamDto teamLookup(
            @Schema(description = "Short name of the team, e.g. 'platform'") String name) {
        return catalog.lookupTeam(name);
    }

    @ToolMethod(name = "services-list", title = "Services List",
            description = """
                    List services across the catalog, optionally filtered by business domain,
                    compliance tag (pii, pci, soc2-scope, customer-facing, foundation, gdpr), or
                    lifecycle stage. Use this for discovery-shaped questions like "what services
                    handle PII", "which services are deprecated", "what's in the checkout domain",
                    or anything starting with "show me all services that...". Results are
                    paginated.""")
    public PageDto<ServiceSummaryDto> servicesList(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Filter by business domain (e.g. 'checkout'). Omit for all domains.") String domain,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Filter by tag (e.g. 'pii', 'pci', 'soc2-scope'). Omit for all tags.") String tag,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Filter by lifecycle stage: ACTIVE, DEPRECATED, RETIRING. Omit for all.") LifecycleStage lifecycle,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        return catalog.listServices(domain, tag, lifecycle, pageIndex, pageSize);
    }

    @ToolMethod(name = "teams-list", title = "Teams List",
            description = """
                    List all engineering teams with the number of services each one owns. Use
                    this for org-chart-shaped questions like "what teams do we have", "which team
                    is biggest", or to enumerate teams before drilling into any one of them.
                    Results are paginated.""")
    public PageDto<TeamSummaryDto> teamsList(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        return catalog.listTeams(pageIndex, pageSize);
    }

    @ToolMethod(name = "service-dependencies", title = "Service Dependencies",
            description = """
                    List what a service depends on — its outbound calls, reads, and event
                    publications. Set transitive=true to get the full downstream tree of
                    everything the service transitively relies on. Use this when asked "what does
                    X use", "what does X depend on", "what are X's upstream systems", or to
                    enumerate a service's outbound surface area.""")
    public RelatedServicesDto serviceDependencies(
            @Schema(description = "Short name of the service") String name,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "If true, follow dependency edges recursively. Defaults to false.") Boolean transitive) {
        return catalog.serviceDependencies(name, Boolean.TRUE.equals(transitive));
    }

    @ToolMethod(name = "service-dependents", title = "Service Dependents",
            description = """
                    List what depends on a service — its inbound callers, readers, and event
                    consumers. Set transitive=true to get the full upstream caller tree. Use this
                    when asked "what calls X", "what uses X", "who consumes X", or to scope a
                    deprecation / refactor. For the "who gets paged if X breaks" question, prefer
                    blast-radius — it returns the same reachability answer grouped by owning team
                    with on-call rotations.""")
    public RelatedServicesDto serviceDependents(
            @Schema(description = "Short name of the service") String name,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "If true, follow dependent edges recursively. Defaults to false.") Boolean transitive) {
        return catalog.serviceDependents(name, Boolean.TRUE.equals(transitive));
    }

    @ToolMethod(name = "blast-radius", title = "Blast Radius",
            description = """
                    Compute the full impact of a service failure. Returns every transitively-
                    impacted downstream service, grouped by owning team with each team's on-call
                    rotation and Slack channel, plus a count of orphaned (unowned) services in
                    the radius. Use this when asked "who gets paged if X breaks", "what's the
                    blast radius of X", "what's the impact of taking X down", "who needs to know
                    about an X outage", or drafting a maintenance-window comms list.""")
    public BlastRadiusDto blastRadius(
            @Schema(description = "Short name of the service whose failure is being assessed") String name) {
        return catalog.blastRadius(name);
    }

    @ToolMethod(name = "orphaned-services", title = "Orphaned Services",
            description = """
                    List services with no assigned owning team — orphans, unowned, unassigned,
                    nobody-is-on-call services. Use this when asked about ownership gaps, services
                    nobody owns, services without a team to contact, or auditing team coverage.
                    Orphaned services with compliance tags (pci, soc2-scope, pii) are the 2-a.m.
                    pages nobody wants to answer.""")
    public PageDto<ServiceSummaryDto> orphanedServices(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        return catalog.orphanedServices(pageIndex, pageSize);
    }

    @ToolMethod(name = "deprecated-in-use", title = "Deprecated Services Still In Use",
            description = """
                    List deprecated services that are still being called, together with the
                    active services that call them. Use this for migration-backlog and tech-debt
                    questions: "what do we need to sunset", "what legacy services are still in
                    use", "what's our deprecation debt", "which services need migration plans",
                    or "what's blocking us from turning off service X".""")
    public PageDto<DeprecatedUsageDto> deprecatedInUse(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        return catalog.deprecatedInUse(pageIndex, pageSize);
    }
}
