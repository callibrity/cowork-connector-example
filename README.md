# Cowork Connector Example

A Claude Cowork connector built with Spring Boot 4 and [Mocapi](https://github.com/callibrity/mocapi). It exposes a fictitious service catalog over the MCP protocol so an LLM inside Cowork can answer the questions every enterprise engineering org struggles to answer:

- **Who owns this service?** And what happens if it breaks?
- **Which services touch PII / PCI?** Are any of them orphaned?
- **What's our migration backlog?** Which deprecated services are still being called, and by whom?
- **If we scale this team down, what are we on the hook for?**

The demo ships with a seeded 36-service / 8-team catalog modeled on a mid-size fictitious company ("Meridian"). The data is intentionally messy — a deprecated service with three active callers, an orphaned service that still holds PCI-scoped data, a naming-drift cluster (`reports-v2`, `reports-v2-new`, `reporting-legacy`), cross-team dependency chains. The answers resonate because the messiness is real.

## Not production-ready

This is a reference example meant to illustrate how an MCP connector is put together, not a template you can deploy as-is. In particular: **there is no authentication** — the `/mcp` endpoint is open to any caller. A production MCP server for Claude Cowork should sit behind one of:

- An OAuth2 resource server validating JWTs from your IdP (Auth0, Microsoft Entra, Okta, etc.) via `spring-boot-starter-oauth2-resource-server`.
- A reverse proxy or API gateway that handles authentication + TLS termination.
- A service mesh enforcing mTLS between Cowork and the connector.

Earlier revisions of this example carried a working `spring-boot-starter-oauth2-resource-server` configuration; it was removed to keep the catalog-demo focus sharp. If you're evaluating this for production, the Mocapi docs and any recent Spring Boot 4 OAuth2 resource-server tutorial will walk you through wiring it back in.

## Tools

Nine read-only tools, all returning structured DTOs that Mocapi publishes via auto-generated JSON Schema:

| Tool | Returns | Purpose |
|---|---|---|
| `service-lookup` | `ServiceDto` | Full service record — owner team, tags, lifecycle, runbook links |
| `team-lookup` | `TeamDto` | Team record with on-call rotation, Slack channel, service count |
| `services-list` | `PageDto<ServiceSummaryDto>` | Paginated list with filters for domain, tag, and lifecycle stage |
| `teams-list` | `PageDto<TeamSummaryDto>` | Paginated list of teams with service counts |
| `service-dependencies` | `List<ServiceSummaryDto>` | Outbound deps (direct or transitive) |
| `service-dependents` | `List<ServiceSummaryDto>` | Inbound callers (direct or transitive) |
| `blast-radius` | `BlastRadiusDto` | Transitively-impacted services grouped by owning team — "who gets paged if this breaks" |
| `orphaned-services` | `PageDto<ServiceSummaryDto>` | Services with no owner |
| `deprecated-in-use` | `PageDto<DeprecatedUsageDto>` | Deprecated services still called by something |

Pagination returns `PaginationDto` metadata (`totalElementCount`, `hasNext`, etc.) so the LLM knows when to page further without being told.

## Example questions

The demo is at its best when the question sounds like one a real staff engineer, engineering manager, or compliance lead would ask out loud. The LLM composes answers by orchestrating multiple tool calls over the seeded catalog; the seeded data has specific, recognizable dysfunctions baked in so the answers read like something from the viewer's own org.

### Compliance / risk

- *"Do we have any compliance-scoped services that nobody owns?"* → surfaces `legacy-invoicing` (PCI + SOC2, deprecated, no team assigned).
- *"Map our PII footprint. Which teams carry the most of it?"* → returns 13 services, heavily concentrated in identity and checkout.
- *"If an auditor asks which services touch PCI data, what's the answer?"* → a two-service list, one of which is an unowned deprecated service — which is the interesting part.

### Migration backlog

- *"What's our migration backlog? Which deprecated services are still in use, and who needs to stop calling them?"* → `reporting-legacy` called by `payment-processor`, `analytics-ingester`, and `legacy-invoicing`; `cart-v1` called by `partner-gateway` and `legacy-invoicing`.
- *"We have `reports-v2`, `reports-v2-new`, and `reporting-legacy`. Which one should a new service use?"* → the LLM can reason about lifecycle state, caller counts, and naming signals.

### Incident prep / blast radius

- *"If `auth-service` goes down, what breaks and who gets paged?"* → 17 impacted services across 6 teams, plus one orphan with no on-call rotation.
- *"We're planning a 30-minute `payment-processor` maintenance window. Draft the customer-comm and internal-announcement copy."* → blast radius plus affected customer-facing features.
- *"What are our most load-bearing services — the ones whose failure would page the most teams?"* → iterative `service-dependents` calls; `auth-service`, `kafka-gateway`, and `events-bus` bubble to the top.

### Onboarding / ramp-up

- *"I just joined the checkout team. What do we own, what do we depend on, and what depends on us?"* → full picture in one prompt — owned services, outbound deps, inbound callers, runbook links.
- *"What's the platform team's surface area? How many other teams depend on them?"* → all seven other teams depend on at least one platform service.

### Architecture / change impact

- *"Map the end-to-end flow from cart to shipping notification."* → transitive dependency walk from `order-coordinator` through payment, inventory, shipping, and email-dispatcher.
- *"If we want to extract identity into its own platform, what's the dependency contract we'd need?"* → every caller of every identity-owned service, plus what those identity services themselves depend on.
- *"Find us any services that look like they were orphaned when someone left."* → exactly what `legacy-invoicing`'s seeded description says happened.

## Architecture

- **Spring Boot 4.0.5** — web server, DI, AOT processing
- **Mocapi 0.4.0** — `@ToolService` / `@ToolMethod` bean discovery, streamable-HTTP MCP transport, JSON Schema generation from method signatures
- **Substrate 0.7.0** — in-memory atom store for MCP session persistence (swap in `substrate-redis` for clustered deployments)
- **Spring Data JPA + H2** — catalog persistence. The H2 tables are seeded in-memory on each startup; swap in Postgres by changing one dep
- **jpa-utils 0.0.12** — `BaseEntity` (UUID + `@Version`), framework-agnostic `PageDto<T>` for paginated tool returns
- **GraalVM native-image** — the chain ships clean reachability metadata: Mocapi, Substrate, Odyssey, and Ripcurl each ship their own `RuntimeHintsRegistrar`. One app-local hint in `CoworkConnectorApplication` covers `json-sKema`'s meta-schema resources; that hint will move into Mocapi in a later release.

## Performance

Measured on Apple Silicon; containers built via Paketo buildpacks.

| Metric | Native image | JVM image | Fat jar |
|---|---|---|---|
| Startup (Spring-reported) | **0.14 s** | 1.34 s | 1.11 s |
| Idle RSS | **~140 MiB** | 306 MiB | 303 MiB |
| Image content size | 68 MB | 336 MB | — |

Native is ~10× faster to start and ~2× more memory-efficient. On scale-to-zero platforms (Azure Container Apps, Cloud Run, Fargate) this is the difference between "cold starts are imperceptible" and "users refresh the tab."

## Running

### Prerequisites

- JDK 25 (Temurin is fine for JVM mode; Oracle GraalVM for local native builds)
- Docker Desktop (for building / running native container images)

### JVM mode — fastest iteration loop

```
mvn spring-boot:run
```

Runs on `http://localhost:8080`. Health at `/actuator/health`, build + git info at `/actuator/info`, MCP endpoint at `/mcp`.

### Native container image — what gets deployed

```
mvn -Pnative spring-boot:build-image -DskipTests \
  -Dspring-boot.build-image.imageName=cowork-connector-example:native \
  -Dspring-boot.build-image.env.BP_NATIVE_IMAGE=true \
  -Dspring-boot.build-image.env.BP_JVM_VERSION=25

docker run --rm -p 8080:8080 \
  -e MOCAPI_SESSION_ENCRYPTION_MASTER_KEY=$(openssl rand -base64 32) \
  cowork-connector-example:native
```

The first build takes a few minutes (Paketo downloads the native-image builder); cached rebuilds are ~60–90 seconds.

### Exposing the local server to Cowork via ngrok

Cowork connectors need a public HTTPS URL — `localhost:8080` isn't reachable from Claude's servers. For local demos, [ngrok](https://ngrok.com/download) tunnels your laptop to a public endpoint:

```
ngrok http 8080
```

ngrok prints a `https://<random>.ngrok-free.app` URL. Point Cowork at `https://<random>.ngrok-free.app/mcp`.

ngrok's free tier inspects traffic at `http://localhost:4040`, which is useful for watching the MCP handshake and tool calls in real time while the demo is running. A fresh free-tier URL is issued each time you restart ngrok; paid plans give you a stable subdomain if you want to save the connector configuration in Cowork.

### Connecting from Cowork

Configure a custom connector in Cowork pointing at your ngrok URL (or your real deployment's URL). The server is unauthenticated in this demo — see [Not production-ready](#not-production-ready) above before pointing any non-demo Cowork workspace at it.

## Exploring the catalog

A few example tool calls the LLM might make:

- `services-list` with `tag=pii` → 13 services across identity, checkout, fulfillment, and notifications
- `orphaned-services` → `legacy-invoicing` (deprecated, PCI- and SOC2-scoped, no owner)
- `deprecated-in-use` → `reporting-legacy` still called by `payment-processor`, `analytics-ingester`, and `legacy-invoicing`; `cart-v1` still called by `partner-gateway`
- `blast-radius` for `auth-service` → 17 services across 6 teams + 1 orphan with no on-call rotation

The full MCP handshake (`initialize` → `notifications/initialized` → `tools/call`) is handled by any MCP client — Cowork, Claude Desktop, MCP Inspector.

## CI / Release

- `.github/workflows/ci.yml` — `mvn verify` on every push and PR to `main`
- `.github/workflows/release.yml` — on tag push, builds a native image via buildpacks and publishes it to GHCR (`ghcr.io/<owner>/<repo>:<tag>` and `:latest`). Runs on `ubuntu-latest` (amd64) — suitable for Azure Container Apps and other amd64 hosts. On Apple Silicon, `docker run` will fall back to Rosetta emulation, which works but doesn't reflect production startup numbers.

## Extending

The catalog model is intentionally compact — three entities (`Service`, `Team`, `Dependency`), four enums, nine tools. Real CMDBs have dozens of fields (SLOs, cost center, data classification, compliance scope). Adding them is additive: extend the entity, re-expose the field in the DTO, either extend an existing tool or add a new one. The native-image metadata story doesn't change — Mocapi's per-bean AOT processor covers new `@ToolMethod` signatures automatically.
