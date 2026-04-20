# Changelog

All notable changes to this project are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.9.0] - 2026-04-20

### Added

- Redis-backed session state via `org.jwcarman.substrate:substrate-redis`. Mocapi's session Mailbox / Notifier / Journal / Atom now persist to Redis instead of Substrate's in-memory fallback, so sessions survive across replicas. Consumes `SPRING_DATA_REDIS_HOST` and `SPRING_DATA_REDIS_PASSWORD` via Spring Boot's Redis autoconfiguration. Lettuce is the transport (added explicitly alongside `spring-boot-starter-data-redis`). The matching infra — a `redis:alpine` container app with TCP ingress internal-only on 6379 and a terraform-generated AUTH password — ships in `azure-infra`.

### Changed

- Import `org.jwcarman.substrate:substrate-bom` so Substrate module versions align without per-dependency version pins.
- `spring.data.redis.repositories.enabled=false` — Spring Data Redis is on the classpath for Substrate's backend, but our `@Repository` interfaces are JPA. Disabling the Redis repo scan prevents it from claiming them.

## [0.8.1] - 2026-04-20

### Fixed

- OpenTelemetry traces/metrics/logs now actually reach Application Insights. 0.8.0 shipped `com.azure:azure-monitor-opentelemetry-autoconfigure`, which only hooks in through the OpenTelemetry SDK auto-configure SPI — but Spring Boot 4's `OpenTelemetryAutoConfiguration` constructs its own `OpenTelemetry` bean and never invokes that SPI, so the Azure Monitor exporter silently never initialized. Swapped to `com.azure.spring:spring-cloud-azure-starter-monitor` (7.2.0), which is a real Spring Boot auto-configuration that reads `APPLICATIONINSIGHTS_CONNECTION_STRING` and plugs the exporter into Spring's OTel bean at startup. Also pinned `opentelemetry-logback-appender-1.0` to `2.24.0-alpha` to align with the incubator-api version pulled in by the Spring Cloud Azure starter's OTel JDBC instrumentation.

## [0.8.0] - 2026-04-20

### Changed

- Release workflow now publishes a JVM image via Paketo buildpacks instead of a native image. Static-final-Logger `<clinit>` traps across several mocapi classes (`MocapiO11yAutoConfiguration`, `McpResourcesService`, and likely others) cause the GraalVM heap scanner to reject `NOP_FallbackServiceProvider` instances baked in during build-time init. Rather than chase each class via `native-image.properties`, we're on JVM runtime until an upstream mocapi patch removes the pattern. Image starts in ~1.5s rather than ~0.15s; RSS rises from ~140 MiB to ~300 MiB. Release cycle drops from ~8 minutes to ~1 minute, which is the point for now.

## [0.7.1] - 2026-04-20

### Fixed

- Native-image build: force `com.callibrity.mocapi.o11y.MocapiO11yAutoConfiguration` to initialize at run time via a local `native-image.properties`. The class otherwise gets build-time-initialized by Spring Boot's AOT pass, its `<clinit>` invokes `LoggerFactory.getLogger(...)` before Logback's SLF4J provider has been registered with SVM, SLF4J caches the `NOP_FallbackServiceProvider`, and the resulting NOP instance ends up in the image heap — which GraalVM rejects because NOP's class is `initialize-at-run-time`. This is a local workaround; the upstream fix in `mocapi-o11y` is expected in a subsequent Mocapi patch release.

## [0.7.0] - 2026-04-20

### Added

- Observability stack via Mocapi + OpenTelemetry. `mocapi-logging` stamps `mcp.session`, `mcp.handler.kind`, and `mcp.handler.name` on the SLF4J MDC for every handler invocation. `mocapi-o11y` wraps each handler call in a Micrometer Observation, producing metrics and tracing spans. `mocapi-actuator` exposes `/actuator/mcp` with a read-only inventory of registered tools / prompts / resources with schema digests.
- OTel shipping path to Azure Application Insights. `micrometer-tracing-bridge-otel` bridges Micrometer Observations to OTel traces; `opentelemetry-logback-appender-1.0` bridges Logback events to OTel logs (with MDC captured as searchable attributes); `azure-monitor-opentelemetry-autoconfigure` (SDK mode, not the Java agent — GraalVM-native compatible) exports everything to App Insights when `APPLICATIONINSIGHTS_CONNECTION_STRING` is set. Default sampler is `parentbased_traceidratio` at 0.25; override via `OTEL_TRACES_SAMPLER` / `OTEL_TRACES_SAMPLER_ARG` without a rebuild.

### Changed

- `/actuator/mcp` is exposed by default (`management.endpoints.web.exposure.include` now includes `mcp` alongside `health` and `info`).

## [0.6.0] - 2026-04-20

### Changed

- Bump mocapi to 0.11.0. 0.11.0 removes `@ToolService` (tool-bearing beans are now discovered from `@Component` directly) and renames `@ToolMethod` to `@McpTool`. `CatalogTools`, `FeedbackTools`, and `ToolProposalTools` have been updated accordingly.

## [0.5.1] - 2026-04-18

### Fixed

- Actuator endpoints (including `/actuator/health/liveness` and `/actuator/health/readiness`) are no longer covered by Spring Security's default filter chain. The `mocapi-oauth2-spring-boot-starter` added in 0.5.0 scopes its chain to `/mcp/**` and the metadata path, but Spring Security's default chain then required auth on every remaining path — including the Kubernetes / Azure Container Apps liveness and readiness probes. Probes 401'd, the orchestrator marked the container unhealthy, and the revision crash-looped. A dedicated `SecurityFilterChain` now permits all actuator endpoints via `EndpointRequest.toAnyEndpoint()`.

## [0.5.0] - 2026-04-18

### Added

- OAuth2 resource-server protection on the MCP endpoint via the `mocapi-oauth2-spring-boot-starter`. The demo defaults to the Callibrity Auth0 tenant (`callibrity-dev.us.auth0.com`) with the MCP endpoint URL as the token audience, per RFC 8707 / RFC 9728. Staging and production override `spring.security.oauth2.resourceserver.jwt.issuer-uri`, `.audiences`, and `.jwk-set-uri` via environment variables; no application-profile switching.
- Jakarta Bean Validation on `@ToolMethod` parameters via the `mocapi-jakarta-validation-spring-boot-starter`. `CatalogTools`, `FeedbackTools`, and `ToolProposalTools` now carry `@NotBlank`, `@NotNull`, `@Size`, and `@Valid` annotations; violations surface as `CallToolResult.isError=true` so the LLM can self-correct.

### Changed

- Bump mocapi to 0.10.0. Mocapi auto-derives `mocapi.oauth2.resource` from the single configured audience, so the OAuth2 block is the minimum three properties: issuer + audience + jwks.

## [0.4.0] - 2026-04-17

### Changed

- Bump mocapi to 0.5.0.
- CI now runs SonarCloud analysis with JaCoCo coverage; seed data and the main application class are excluded from coverage. Fixed the Sonar issues surfaced by the new analysis (lambda simplifications, unconditional logging guards, missing test assertions).
- License headers use the project `inceptionYear` instead of a hardcoded year.

### Documentation

- README updated to describe the SonarCloud setup, license-header enforcement, and the Release-triggered publish workflow.

### Dependencies

- Bump `io.swagger.core.v3:swagger-annotations` (#4).
- Bump `actions/checkout` to v6 (#1), `actions/setup-java` to v5 (#3), and `docker/login-action` to v4 (#2).

## [0.3.0] - 2026-04-16

### Added

- Two new MCP tools form a self-improvement feedback loop. `submit-feedback` lets the calling LLM report friction with an existing tool — the structured input demands a `toolName`, `FrictionType` (one of `MISSING_FIELD`, `EXTRA_ROUND_TRIPS`, `AWKWARD_SCHEMA`, `AMBIGUOUS_NAMING`, `AWKWARD_RESPONSE_SHAPE`), a specific description, and a concrete `suggestedChange`. `suggest-tool` lets the LLM propose a brand-new tool — the schema demands a `motivatingQuestion`, an `existingToolGap` (which tools you tried first), and a `frequency` estimate (`ONCE_THIS_SESSION` / `RECURRING_PATTERN` / `FOUNDATIONAL`). Both tools emit structured JSON log lines (`MCP_FEEDBACK:` / `MCP_TOOL_PROPOSAL:`) for downstream aggregation into PRs that evolve the server.

### Changed

- The repository is now a proper Apache 2.0 OSS project: `LICENSE`, `CONTRIBUTING.md`, and `CODE_OF_CONDUCT.md` files added; license headers on every Java file enforced by `license-maven-plugin`; code style enforced by Spotless with Google Java Format; pull-request and issue templates added; release workflow now triggers on GitHub Release creation rather than a bare tag push.

## [0.2.0] - 2026-04-16

### Fixed

- `service-dependencies` and `service-dependents` now return a typed `RelatedServicesDto` object (fields: `rootService`, `transitive`, `services`) instead of a bare `List<ServiceSummaryDto>`. Mocapi's schema generator emits an object schema with properly-typed items; MCP clients receive the result in `structuredContent` where they previously saw a bare-array schema that caused validation errors in Cowork.

### Changed

- Bump mocapi to 0.4.1, which bundles native-image hints for `json-sKema`'s draft meta-schemas. Removed the temporary `AppHints` `RuntimeHintsRegistrar` from `CoworkConnectorApplication` — the app now ships with no native-image-specific code.
- Expanded the README with an "Example questions" section showing the compliance, migration, incident, onboarding, and architectural questions the demo is designed to answer.

## [0.1.0] - 2026-04-16

### Added

- Seeded 36-service / 8-team / 86-dependency service catalog ("Meridian") backed by Spring Data JPA and H2.
- Nine read-only `@ToolMethod` tools on `CatalogTools`: `service-lookup`, `team-lookup`, `services-list`, `teams-list`, `service-dependencies`, `service-dependents`, `blast-radius`, `orphaned-services`, `deprecated-in-use`.
- Paginated tool returns using `PageDto<T>` from `jpa-utils`, with schema metadata published automatically through Mocapi's `MethodSchemaGenerator`.
- GraalVM native-image support via Paketo buildpacks. Measured ~0.14 s startup and ~140 MiB RSS on Apple Silicon.
- `spring-boot:build-info` and `git-commit-id` plugins surface build metadata and git commit through `/actuator/info`.
- GitHub Actions: `ci.yml` runs `mvn verify` on every push and pull request to `main`; `release.yml` builds a native image via buildpacks on tag push and publishes it to GHCR.
- Mockito-based unit tests for `CatalogTools` covering lookup, filtering, BFS traversal (with cycle handling), blast-radius aggregation, and governance (orphans, deprecated-in-use).

### Requirements

- JDK 25 (GraalVM 25 required for local native-image builds; Temurin 25 is sufficient for JVM mode).
- Docker Desktop for building or running native container images.

[Unreleased]: https://github.com/callibrity/cowork-connector-example/compare/0.9.0...HEAD
[0.9.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.9.0
[0.8.1]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.8.1
[0.8.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.8.0
[0.7.1]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.7.1
[0.7.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.7.0
[0.6.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.6.0
[0.5.1]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.5.1
[0.5.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.5.0
[0.4.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.4.0
[0.3.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.3.0
[0.2.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.2.0
[0.1.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.1.0
