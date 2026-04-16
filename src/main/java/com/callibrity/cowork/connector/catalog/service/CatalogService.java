package com.callibrity.cowork.connector.catalog.service;

import com.callibrity.cowork.connector.catalog.domain.LifecycleStage;
import com.callibrity.cowork.connector.catalog.dto.BlastRadiusDto;
import com.callibrity.cowork.connector.catalog.dto.DeprecatedUsageDto;
import com.callibrity.cowork.connector.catalog.dto.RelatedServicesDto;
import com.callibrity.cowork.connector.catalog.dto.ServiceDto;
import com.callibrity.cowork.connector.catalog.dto.ServiceSummaryDto;
import com.callibrity.cowork.connector.catalog.dto.TeamDto;
import com.callibrity.cowork.connector.catalog.dto.TeamSummaryDto;
import org.jwcarman.jpa.pagination.PageDto;

/**
 * Application-layer port for catalog queries. Adapter implementations — the MCP `CatalogTools`
 * bean today, a hypothetical REST controller tomorrow — call through this interface and stay
 * transport-agnostic. The service itself knows nothing about MCP, HTTP, Jackson, or Spring Data's
 * pagination types.
 */
public interface CatalogService {

    ServiceDto lookupService(String name);

    TeamDto lookupTeam(String name);

    PageDto<ServiceSummaryDto> listServices(String domain, String tag, LifecycleStage lifecycle,
                                            Integer pageIndex, Integer pageSize);

    PageDto<TeamSummaryDto> listTeams(Integer pageIndex, Integer pageSize);

    RelatedServicesDto serviceDependencies(String name, boolean transitive);

    RelatedServicesDto serviceDependents(String name, boolean transitive);

    BlastRadiusDto blastRadius(String name);

    PageDto<ServiceSummaryDto> orphanedServices(Integer pageIndex, Integer pageSize);

    PageDto<DeprecatedUsageDto> deprecatedInUse(Integer pageIndex, Integer pageSize);
}
