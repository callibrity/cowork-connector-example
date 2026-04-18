# Changelog

All notable changes to this project are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/callibrity/cowork-connector-example/compare/0.5.1...HEAD
[0.5.1]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.5.1
[0.5.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.5.0
[0.4.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.4.0
[0.3.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.3.0
[0.2.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.2.0
[0.1.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.1.0
