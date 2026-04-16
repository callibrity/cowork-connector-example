package com.callibrity.cowork.connector.catalog.tool;

import com.callibrity.cowork.connector.catalog.domain.Dependency;
import com.callibrity.cowork.connector.catalog.domain.LifecycleStage;
import com.callibrity.cowork.connector.catalog.domain.Service;
import com.callibrity.cowork.connector.catalog.domain.Team;
import com.callibrity.cowork.connector.catalog.dto.BlastRadiusDto;
import com.callibrity.cowork.connector.catalog.dto.DeprecatedUsageDto;
import com.callibrity.cowork.connector.catalog.dto.RelatedServicesDto;
import com.callibrity.cowork.connector.catalog.dto.ServiceDto;
import com.callibrity.cowork.connector.catalog.dto.ServiceSummaryDto;
import com.callibrity.cowork.connector.catalog.dto.TeamDto;
import com.callibrity.cowork.connector.catalog.dto.TeamImpactDto;
import com.callibrity.cowork.connector.catalog.dto.TeamSummaryDto;
import com.callibrity.cowork.connector.catalog.repository.DependencyRepository;
import com.callibrity.cowork.connector.catalog.repository.ServiceRepository;
import com.callibrity.cowork.connector.catalog.repository.TeamRepository;
import com.callibrity.mocapi.api.tools.ToolMethod;
import com.callibrity.mocapi.api.tools.ToolService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.jwcarman.jpa.pagination.PageDto;
import org.jwcarman.jpa.spring.page.Pages;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@ToolService
@Component
@RequiredArgsConstructor
public class CatalogTools {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ServiceRepository serviceRepo;
    private final TeamRepository teamRepo;
    private final DependencyRepository dependencyRepo;

    @ToolMethod(name = "service-lookup", title = "Service Lookup",
            description = "Fetch a single service by its short name, including owner team, tags, lifecycle, and runbook links.")
    public ServiceDto serviceLookup(
            @Schema(description = "Short name of the service, e.g. 'payment-processor'") String name) {
        Service service = requireService(name);
        int directDeps = dependencyRepo.findAllByFromService(service).size();
        int directDependents = dependencyRepo.findAllByToService(service).size();
        return toServiceDto(service, directDeps, directDependents);
    }

    @ToolMethod(name = "team-lookup", title = "Team Lookup",
            description = "Fetch a team by its short name, including on-call rotation and Slack channel.")
    public TeamDto teamLookup(
            @Schema(description = "Short name of the team, e.g. 'platform'") String name) {
        Team team = teamRepo.findByName(name).orElseThrow(() ->
                new IllegalArgumentException("Unknown team: " + name));
        long serviceCount = serviceRepo.findAllByOwnerName(team.getName()).size();
        return new TeamDto(team.getName(), team.getDisplayName(), team.getOnCallRotation(),
                team.getSlackChannel(), serviceCount);
    }

    @ToolMethod(name = "services-list", title = "Services List",
            description = "Paginated list of services, optionally filtered by domain, tag, and lifecycle stage.")
    public PageDto<ServiceSummaryDto> servicesList(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Filter by business domain (e.g. 'checkout'). Omit for all domains.") String domain,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Filter by tag (e.g. 'pii', 'pci', 'soc2-scope'). Omit for all tags.") String tag,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Filter by lifecycle stage: ACTIVE, DEPRECATED, RETIRING. Omit for all.") LifecycleStage lifecycle,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        Pageable pageable = pageable(pageIndex, pageSize, Sort.by("name"));
        return Pages.pageDtoOf(
                serviceRepo.search(blankToNull(domain), lifecycle, blankToNull(tag), pageable)
                        .map(this::toServiceSummary));
    }

    @ToolMethod(name = "teams-list", title = "Teams List",
            description = "Paginated list of all teams with the number of services each one owns.")
    public PageDto<TeamSummaryDto> teamsList(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        Pageable pageable = pageable(pageIndex, pageSize, Sort.by("name"));
        return Pages.pageDtoOf(teamRepo.findAll(pageable).map(this::toTeamSummary));
    }

    @ToolMethod(name = "service-dependencies", title = "Service Dependencies",
            description = "Services that the given service depends on. Set transitive=true for the full downstream tree.")
    public RelatedServicesDto serviceDependencies(
            @Schema(description = "Short name of the service") String name,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "If true, follow dependency edges recursively. Defaults to false.") Boolean transitive) {
        Service start = requireService(name);
        boolean recursive = Boolean.TRUE.equals(transitive);
        List<ServiceSummaryDto> services = traverse(start, recursive,
                s -> dependencyRepo.findAllByFromService(s),
                Dependency::getToService).stream().map(this::toServiceSummary).toList();
        return new RelatedServicesDto(start.getName(), recursive, services);
    }

    @ToolMethod(name = "service-dependents", title = "Service Dependents",
            description = "Services that depend on the given service. Set transitive=true for the full upstream caller tree.")
    public RelatedServicesDto serviceDependents(
            @Schema(description = "Short name of the service") String name,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "If true, follow dependent edges recursively. Defaults to false.") Boolean transitive) {
        Service start = requireService(name);
        boolean recursive = Boolean.TRUE.equals(transitive);
        List<ServiceSummaryDto> services = traverse(start, recursive,
                s -> dependencyRepo.findAllByToService(s),
                Dependency::getFromService).stream().map(this::toServiceSummary).toList();
        return new RelatedServicesDto(start.getName(), recursive, services);
    }

    @ToolMethod(name = "blast-radius", title = "Blast Radius",
            description = "If this service fails, which services are transitively impacted and which teams would be paged?")
    public BlastRadiusDto blastRadius(
            @Schema(description = "Short name of the service whose failure is being assessed") String name) {
        Service target = requireService(name);
        List<Service> impacted = traverse(target, true,
                s -> dependencyRepo.findAllByToService(s),
                Dependency::getFromService);

        Map<String, List<ServiceSummaryDto>> byTeam = new LinkedHashMap<>();
        Map<String, Team> teamByName = new LinkedHashMap<>();
        int orphans = 0;
        for (Service s : impacted) {
            Team owner = s.getOwner();
            if (owner == null) {
                orphans++;
                continue;
            }
            teamByName.putIfAbsent(owner.getName(), owner);
            byTeam.computeIfAbsent(owner.getName(), k -> new ArrayList<>()).add(toServiceSummary(s));
        }

        List<TeamImpactDto> teamImpacts = byTeam.entrySet().stream().map(entry -> {
            Team team = teamByName.get(entry.getKey());
            return new TeamImpactDto(
                    toTeamSummary(team),
                    team.getOnCallRotation(),
                    team.getSlackChannel(),
                    entry.getValue());
        }).toList();

        return new BlastRadiusDto(
                toServiceSummary(target),
                impacted.stream().map(this::toServiceSummary).toList(),
                teamImpacts,
                orphans);
    }

    @ToolMethod(name = "orphaned-services", title = "Orphaned Services",
            description = "Services with no owning team. Ownership gaps are the 'who do we page?' question nobody wants to answer at 2 a.m.")
    public PageDto<ServiceSummaryDto> orphanedServices(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        Pageable pageable = pageable(pageIndex, pageSize, Sort.by("name"));
        return Pages.pageDtoOf(serviceRepo.findAllByOwnerIsNull(pageable).map(this::toServiceSummary));
    }

    @ToolMethod(name = "deprecated-in-use", title = "Deprecated Services Still In Use",
            description = "Deprecated services that something still depends on — the migration backlog the LLM can summarize.")
    public PageDto<DeprecatedUsageDto> deprecatedInUse(
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Zero-based page index. Defaults to 0.") Integer pageIndex,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Page size. Defaults to 20, capped at 100.") Integer pageSize) {
        Pageable pageable = pageable(pageIndex, pageSize, Sort.by("name"));
        return Pages.pageDtoOf(serviceRepo.findDeprecatedInUse(pageable).map(svc -> {
            List<ServiceSummaryDto> callers = dependencyRepo.findAllByToService(svc).stream()
                    .map(Dependency::getFromService)
                    .map(this::toServiceSummary)
                    .toList();
            return new DeprecatedUsageDto(toServiceSummary(svc), callers);
        }));
    }

    private Service requireService(String name) {
        return serviceRepo.findByName(name).orElseThrow(() ->
                new IllegalArgumentException("Unknown service: " + name));
    }

    private List<Service> traverse(Service start, boolean transitive,
                                   Function<Service, List<Dependency>> edges,
                                   Function<Dependency, Service> next) {
        if (!transitive) {
            return edges.apply(start).stream().map(next).toList();
        }
        Set<Service> visited = new LinkedHashSet<>();
        Deque<Service> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        List<Service> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            Service current = queue.poll();
            for (Dependency edge : edges.apply(current)) {
                Service neighbor = next.apply(edge);
                if (visited.add(neighbor)) {
                    queue.offer(neighbor);
                    result.add(neighbor);
                }
            }
        }
        return result;
    }

    private ServiceSummaryDto toServiceSummary(Service s) {
        Team owner = s.getOwner();
        return new ServiceSummaryDto(
                s.getName(),
                s.getDisplayName(),
                s.getDomain(),
                owner == null ? null : owner.getName(),
                s.getLifecycleStage(),
                s.getTags());
    }

    private ServiceDto toServiceDto(Service s, int directDeps, int directDependents) {
        Team owner = s.getOwner();
        return new ServiceDto(
                s.getName(),
                s.getDisplayName(),
                s.getDescription(),
                s.getDomain(),
                owner == null ? null : toTeamSummary(owner),
                s.getLifecycleStage(),
                s.getRepoUrl(),
                s.getRunbookUrl(),
                s.getTags(),
                directDeps,
                directDependents);
    }

    private TeamSummaryDto toTeamSummary(Team t) {
        long count = serviceRepo.findAllByOwnerName(t.getName()).size();
        return new TeamSummaryDto(t.getName(), t.getDisplayName(), count);
    }

    private static Pageable pageable(Integer pageIndex, Integer pageSize, Sort sort) {
        int idx = pageIndex == null || pageIndex < 0 ? 0 : pageIndex;
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        return PageRequest.of(idx, size, sort);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
