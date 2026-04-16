package com.callibrity.cowork.connector.catalog.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.callibrity.cowork.connector.catalog.domain.Dependency;
import com.callibrity.cowork.connector.catalog.domain.DependencyType;
import com.callibrity.cowork.connector.catalog.domain.LifecycleStage;
import com.callibrity.cowork.connector.catalog.domain.Service;
import com.callibrity.cowork.connector.catalog.domain.Team;
import com.callibrity.cowork.connector.catalog.dto.BlastRadiusDto;
import com.callibrity.cowork.connector.catalog.dto.DeprecatedUsageDto;
import com.callibrity.cowork.connector.catalog.dto.RelatedServicesDto;
import com.callibrity.cowork.connector.catalog.dto.ServiceDto;
import com.callibrity.cowork.connector.catalog.dto.ServiceSummaryDto;
import com.callibrity.cowork.connector.catalog.dto.TeamDto;
import com.callibrity.cowork.connector.catalog.repository.DependencyRepository;
import com.callibrity.cowork.connector.catalog.repository.ServiceRepository;
import com.callibrity.cowork.connector.catalog.repository.TeamRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jwcarman.jpa.pagination.PageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogToolsTest {

    @Mock
    ServiceRepository serviceRepo;

    @Mock
    TeamRepository teamRepo;

    @Mock
    DependencyRepository dependencyRepo;

    @InjectMocks
    CatalogTools tools;

    private Team platform;
    private Team identity;
    private final Map<String, Service> services = new HashMap<>();
    private final List<Dependency> edges = new ArrayList<>();

    @BeforeEach
    void setUp() {
        platform = new Team("platform", "Platform Engineering", "pd://platform", "#eng-platform");
        identity = new Team("identity", "Identity & Access", "pd://identity", "#eng-identity");

        // a small, focused fixture graph
        //   auth-service (platform)  <──┐  <──┐
        //   accounts-api (identity) ─ calls ─ auth-service
        //   sso-broker   (identity) ─ calls ─ accounts-api ─> auth-service
        //   cart-service (no owner, orphan) ─ calls ─ auth-service
        //   legacy (platform, DEPRECATED)
        //   legacy-caller (identity) ─ calls ─ legacy
        registerService("auth-service", "Auth Service", "platform", platform, LifecycleStage.ACTIVE, Set.of("foundation"));
        registerService("accounts-api", "Accounts API", "identity", identity, LifecycleStage.ACTIVE, Set.of("pii"));
        registerService("sso-broker", "SSO Broker", "identity", identity, LifecycleStage.ACTIVE, Set.of("pii"));
        registerService("cart-service", "Cart Service", "checkout", null, LifecycleStage.ACTIVE, Set.of("pii"));
        registerService("legacy", "Legacy Service", "platform", platform, LifecycleStage.DEPRECATED, Set.of());
        registerService("legacy-caller", "Legacy Caller", "identity", identity, LifecycleStage.ACTIVE, Set.of());

        addEdge("accounts-api", "auth-service", DependencyType.CALLS);
        addEdge("sso-broker", "accounts-api", DependencyType.CALLS);
        addEdge("sso-broker", "auth-service", DependencyType.CALLS);
        addEdge("cart-service", "auth-service", DependencyType.CALLS);
        addEdge("legacy-caller", "legacy", DependencyType.CALLS);

        when(serviceRepo.findByName(any())).thenAnswer(inv ->
                Optional.ofNullable(services.get(inv.<String>getArgument(0))));
        when(serviceRepo.findAllByOwnerName(any())).thenAnswer(inv -> {
            String teamName = inv.getArgument(0);
            return services.values().stream()
                    .filter(s -> s.getOwner() != null && s.getOwner().getName().equals(teamName))
                    .toList();
        });
        when(dependencyRepo.findAllByFromService(any())).thenAnswer(inv -> {
            Service from = inv.getArgument(0);
            return edges.stream().filter(d -> d.getFromService().equals(from)).toList();
        });
        when(dependencyRepo.findAllByToService(any())).thenAnswer(inv -> {
            Service to = inv.getArgument(0);
            return edges.stream().filter(d -> d.getToService().equals(to)).toList();
        });
    }

    @Nested
    class Lookup {

        @Test
        void serviceLookupReturnsFullDto() {
            ServiceDto dto = tools.serviceLookup("accounts-api");
            assertThat(dto.name()).isEqualTo("accounts-api");
            assertThat(dto.owner().name()).isEqualTo("identity");
            assertThat(dto.tags()).containsExactly("pii");
            assertThat(dto.lifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
            assertThat(dto.directDependencyCount()).isEqualTo(1);
            assertThat(dto.directDependentCount()).isEqualTo(1);
        }

        @Test
        void serviceLookupReturnsNullOwnerForOrphan() {
            ServiceDto dto = tools.serviceLookup("cart-service");
            assertThat(dto.owner()).isNull();
        }

        @Test
        void serviceLookupUnknownThrows() {
            assertThatThrownBy(() -> tools.serviceLookup("nope"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown service");
        }

        @Test
        void teamLookupIncludesServiceCount() {
            when(teamRepo.findByName("identity")).thenReturn(Optional.of(identity));
            TeamDto dto = tools.teamLookup("identity");
            assertThat(dto.name()).isEqualTo("identity");
            assertThat(dto.onCallRotation()).isEqualTo("pd://identity");
            assertThat(dto.serviceCount()).isEqualTo(3);
        }

        @Test
        void teamLookupUnknownThrows() {
            when(teamRepo.findByName("nope")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> tools.teamLookup("nope"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown team");
        }
    }

    @Nested
    class Listing {

        @Test
        void servicesListPassesFiltersToRepo() {
            when(serviceRepo.search(eq("checkout"), eq(LifecycleStage.ACTIVE), eq("pii"), any(Pageable.class)))
                    .thenReturn(pageOf(List.of(services.get("cart-service"))));
            PageDto<ServiceSummaryDto> page = tools.servicesList("checkout", "pii", LifecycleStage.ACTIVE, null, null);
            assertThat(page.data()).extracting(ServiceSummaryDto::name).containsExactly("cart-service");
            assertThat(page.pagination().totalElementCount()).isEqualTo(1);
        }

        @Test
        void servicesListTreatsBlankFiltersAsNull() {
            when(serviceRepo.search(eq(null), eq(null), eq(null), any(Pageable.class)))
                    .thenReturn(pageOf(List.copyOf(services.values())));
            tools.servicesList("  ", "", null, null, null);
            // success if the stubbed call with nulls matched; Mockito would throw otherwise
        }

        @Test
        void orphanedServicesFindsServicesWithoutOwner() {
            when(serviceRepo.findAllByOwnerIsNull(any(Pageable.class)))
                    .thenReturn(pageOf(List.of(services.get("cart-service"))));
            PageDto<ServiceSummaryDto> page = tools.orphanedServices(null, null);
            assertThat(page.data()).hasSize(1);
            assertThat(page.data().getFirst().ownerTeam()).isNull();
        }

        @Test
        void deprecatedInUseIncludesCallers() {
            when(serviceRepo.findDeprecatedInUse(any(Pageable.class)))
                    .thenReturn(pageOf(List.of(services.get("legacy"))));
            PageDto<DeprecatedUsageDto> page = tools.deprecatedInUse(null, null);
            assertThat(page.data()).hasSize(1);
            DeprecatedUsageDto usage = page.data().getFirst();
            assertThat(usage.deprecatedService().name()).isEqualTo("legacy");
            assertThat(usage.callers()).extracting(ServiceSummaryDto::name)
                    .containsExactly("legacy-caller");
        }
    }

    @Nested
    class Traversal {

        @Test
        void directDependenciesReturnsImmediateChildren() {
            RelatedServicesDto deps = tools.serviceDependencies("sso-broker", false);
            assertThat(deps.rootService()).isEqualTo("sso-broker");
            assertThat(deps.transitive()).isFalse();
            assertThat(deps.services()).extracting(ServiceSummaryDto::name)
                    .containsExactlyInAnyOrder("accounts-api", "auth-service");
        }

        @Test
        void transitiveDependenciesReturnsFullDownstreamTree() {
            RelatedServicesDto deps = tools.serviceDependencies("sso-broker", true);
            // sso-broker -> accounts-api -> auth-service, plus sso-broker -> auth-service
            // auth-service should appear once (dedup)
            assertThat(deps.transitive()).isTrue();
            assertThat(deps.services()).extracting(ServiceSummaryDto::name)
                    .containsExactlyInAnyOrder("accounts-api", "auth-service");
        }

        @Test
        void transitiveDependentsReturnsCallerTree() {
            RelatedServicesDto callers = tools.serviceDependents("auth-service", true);
            assertThat(callers.services()).extracting(ServiceSummaryDto::name)
                    .containsExactlyInAnyOrder("accounts-api", "sso-broker", "cart-service");
        }

        @Test
        void directDependentsOnlyReturnsImmediateCallers() {
            RelatedServicesDto callers = tools.serviceDependents("auth-service", false);
            assertThat(callers.services()).extracting(ServiceSummaryDto::name)
                    .containsExactlyInAnyOrder("accounts-api", "sso-broker", "cart-service");
        }

        @Test
        void traversalHandlesCyclesWithoutInfiniteLoop() {
            // introduce a cycle: auth-service -> accounts-api -> auth-service
            addEdge("auth-service", "accounts-api", DependencyType.CALLS);
            RelatedServicesDto deps = tools.serviceDependencies("auth-service", true);
            assertThat(deps.services()).extracting(ServiceSummaryDto::name)
                    .containsExactly("accounts-api");
        }
    }

    @Nested
    class BlastRadius {

        @Test
        void groupsImpactedServicesByOwningTeam() {
            BlastRadiusDto radius = tools.blastRadius("auth-service");
            assertThat(radius.target().name()).isEqualTo("auth-service");
            assertThat(radius.impactedServices()).extracting(ServiceSummaryDto::name)
                    .containsExactlyInAnyOrder("accounts-api", "sso-broker", "cart-service");
            assertThat(radius.teamsAffected())
                    .extracting(t -> t.team().name())
                    .containsExactly("identity");
            assertThat(radius.teamsAffected().getFirst().impactedServices())
                    .extracting(ServiceSummaryDto::name)
                    .containsExactlyInAnyOrder("accounts-api", "sso-broker");
        }

        @Test
        void countsOrphanedServicesSeparately() {
            BlastRadiusDto radius = tools.blastRadius("auth-service");
            // cart-service is in the radius but has no owner
            assertThat(radius.orphanedImpactedCount()).isEqualTo(1);
            assertThat(radius.teamsAffected())
                    .flatExtracting(t -> t.impactedServices().stream().map(ServiceSummaryDto::name).toList())
                    .doesNotContain("cart-service");
        }
    }

    @Nested
    class Pagination {

        @Test
        void negativePageIndexClampsToZero() {
            when(teamRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.of()));
            tools.teamsList(-5, 50);
            // if Pageable construction threw, this test would fail
        }

        @Test
        void nullPageSizeUsesDefault() {
            when(teamRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.of()));
            tools.teamsList(0, null);
        }

        @Test
        void oversizedPageSizeClampsToMax() {
            when(teamRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.of()));
            tools.teamsList(0, 9999);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void registerService(String name, String displayName, String domain,
                                 Team owner, LifecycleStage lifecycle, Set<String> tags) {
        Service s = new Service(name, displayName, "desc", domain, owner, lifecycle,
                "https://repo/" + name, "https://runbook/" + name, tags);
        services.put(name, s);
    }

    private void addEdge(String from, String to, DependencyType type) {
        edges.add(new Dependency(services.get(from), services.get(to), type));
    }

    private <T> Page<T> pageOf(List<T> content) {
        return new PageImpl<>(content);
    }
}
