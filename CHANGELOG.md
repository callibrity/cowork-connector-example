# Changelog

All notable changes to this project are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/callibrity/cowork-connector-example/compare/0.2.0...HEAD
[0.2.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.2.0
[0.1.0]: https://github.com/callibrity/cowork-connector-example/releases/tag/0.1.0
