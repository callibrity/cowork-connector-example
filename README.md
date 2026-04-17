# Cowork Connector Example

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A Claude Cowork connector built with Spring Boot 4 and [Mocapi](https://github.com/callibrity/mocapi). It exposes a fictitious service catalog over the MCP protocol so an LLM inside Cowork can answer the questions every enterprise engineering org struggles to answer:

- **Who owns this service?** And what happens if it breaks?
- **Which services touch PII / PCI?** Are any of them orphaned?
- **What's our migration backlog?** Which deprecated services are still being called, and by whom?
- **If we scale this team down, what are we on the hook for?**

The demo ships with a seeded 36-service / 8-team catalog modeled on a mid-size fictitious company ("Meridian"). The data is intentionally messy — the dysfunctions are the point.

## Meet Meridian

Meridian is a mid-size fictitious e-commerce platform. Eight engineering teams; thirty-six services across eight business domains; roughly eighty-six in-production dependencies. The catalog is deliberately shaped around patterns every real service catalog has — deprecated services still in use, a compliance-scoped orphan, a naming-drift cluster, a retiring service that still carries traffic — so the tool-orchestrated answers stay concrete rather than academic.

### The eight teams

- **platform** — foundational services (auth, feature flags, Kafka gateway, log aggregator, secrets manager).
- **identity** — accounts, SSO broker, session store, password reset.
- **checkout** — cart, payments, order coordination, tax calculation, checkout UI.
- **catalog** — products, search indexer, inventory, catalog admin.
- **fulfillment** — shipping, label generation, returns, carrier integrations.
- **data** — analytics ingester, events bus, reporting (plural), ETL orchestrator.
- **notifications** — email, push, SMS dispatchers, template renderer.
- **integrations** — partner gateway, webhook relay.

### The dysfunctions, on purpose

The seed data is shaped around five specific patterns every enterprise engineering org has some version of — these are what make the demo answers resonate:

1. **The deprecated-in-use case.** `reporting-legacy` is marked `DEPRECATED` but is still called by `payment-processor` (for a tax-summary endpoint), `analytics-ingester`, and the orphaned `legacy-invoicing`. Every org has this — the service nobody's migrated off of because it still works. Shows up in `deprecated-in-use` results and in `payment-processor`'s direct dependencies.

2. **The orphan with compliance scope.** `legacy-invoicing` has **no owner** — "Owner left in 2023; team was never reassigned" per its seeded description — and is tagged `pci` and `soc2-scope`. It's still called by other services in the graph. This is the compliance nightmare that every `orphaned-services` query should surface.

3. **The naming-drift cluster.** The data team owns `reports-v2`, `reports-v2-new`, and `reporting-legacy`. The "current" one is ambiguously `reports-v2` (used by `catalog-admin`) while `reports-v2-new` was "the replacement, but the rollout was paused in Q3" and `reporting-legacy` is deprecated-but-still-called. Classic every-shop-has-three-versions cluster.

4. **The retiring service still in the graph.** `webhook-relay` is `RETIRING` but still publishes to `kafka-gateway`. Illustrates how retirement plans and real traffic don't always line up.

5. **The foundational blast radius.** `auth-service` has **17 transitively-impacted dependents across 6 teams**, plus the `legacy-invoicing` orphan. `kafka-gateway`, `feature-flags`, and `events-bus` are similarly load-bearing. Drives `blast-radius` queries and the "which services deserve the strongest SLO" conversation.

### Why the seed looks like this

The tool surface is generic — you can point this connector at your real CMDB export, Backstage catalog, or any service-inventory system and every query still works. The seeded Meridian data exists because a demo answering catalog queries against pristine, well-owned, perfectly-documented services isn't a useful demo. Real service catalogs have a naming-drift cluster, a compliance-scoped orphan, a deprecated service nobody's migrated off. Compressing those shapes into 36 services gives the LLM something substantive to reason over — the answers are concrete and the dysfunctions are the kind you'd actually want surfaced from your own catalog.

## What you can ask Claude

The demo is at its best when the question sounds like one a real staff engineer, engineering manager, or compliance lead would ask out loud. The LLM composes answers by orchestrating multiple tool calls over the seeded catalog; because the seed includes real-shaped dysfunctions, the answers stay concrete and actionable rather than academic.

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

## Not production-ready

This is a reference example meant to illustrate how an MCP connector is put together, not a template you can deploy as-is. In particular: **there is no authentication** — the `/mcp` endpoint is open to any caller. A production MCP server for Claude Cowork should sit behind one of:

- An OAuth2 resource server validating JWTs from your IdP (Auth0, Microsoft Entra, Okta, etc.) via `spring-boot-starter-oauth2-resource-server`.
- A reverse proxy or API gateway that handles authentication + TLS termination.
- A service mesh enforcing mTLS between Cowork and the connector.

Earlier revisions of this example carried a working `spring-boot-starter-oauth2-resource-server` configuration; it was removed to keep the catalog-demo focus sharp. If you're evaluating this for production, the Mocapi docs and any recent Spring Boot 4 OAuth2 resource-server tutorial will walk you through wiring it back in.

## Tools

Nine read-only catalog tools plus two self-improvement tools, all returning structured DTOs that Mocapi publishes via auto-generated JSON Schema:

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
| `submit-feedback` | `FeedbackAckDto` | Lets the calling LLM report friction with an existing tool — emits an `MCP_FEEDBACK:` log line |
| `suggest-tool` | `FeedbackAckDto` | Lets the calling LLM propose a brand-new tool the server should add — emits an `MCP_TOOL_PROPOSAL:` log line |

Pagination returns `PaginationDto` metadata (`totalElementCount`, `hasNext`, etc.) so the LLM knows when to page further without being told.

### The self-improvement loop

`submit-feedback` and `suggest-tool` are an experiment in closing the loop on MCP server design: the LLM is the actual user, and most of what makes a tool surface awkward only shows up mid-conversation. The two tools split the signal deliberately:

- **`submit-feedback`** is for friction with an existing tool — a missing field, an extra round-trip, an ambiguous name, an awkward response shape. The description enumerates concrete triggers and demands a `suggestedChange` field that takes a position (forces the LLM past vague hedging).
- **`suggest-tool`** is for genuinely new tool ideas — questions you needed to answer this session that no existing tool covered. The schema demands an `existingToolGap` field describing which tools you tried first (filters out "wouldn't it be nice" speculation) and a `frequency` estimate (`ONCE_THIS_SESSION` / `RECURRING_PATTERN` / `FOUNDATIONAL`), so the maintainer can filter for the truly load-bearing proposals.

Both tools tell the LLM explicitly: if you didn't hit the trigger condition, don't call the tool — silence is the most useful signal. Each call emits a structured JSON payload at INFO level with a distinct marker (`MCP_FEEDBACK:` vs. `MCP_TOOL_PROPOSAL:`), so a downstream aggregator can split the streams and route them to different decision flows: friction reports cluster into "should we change an existing tool?", proposals cluster into "should we add this tool?". Humans review the resulting PRs; the LLM's individual submissions are noisy signals, not commitments.

## Architecture

- **Spring Boot 4.0.5** — web server, DI, AOT processing
- **Mocapi 0.4.1** — `@ToolService` / `@ToolMethod` bean discovery, streamable-HTTP MCP transport, JSON Schema generation from method signatures. Bundles native-image hints for every type it owns plus the `json-sKema` meta-schemas it uses for tool input validation.
- **Substrate 0.7.0** — in-memory atom store for MCP session persistence (swap in `substrate-redis` for clustered deployments)
- **Spring Data JPA + H2** — catalog persistence. The H2 tables are seeded in-memory on each startup; swap in Postgres by changing one dep
- **jpa-utils 0.0.12** — `BaseEntity` (UUID + `@Version`), framework-agnostic `PageDto<T>` for paginated tool returns
- **GraalVM native-image** — the chain ships clean reachability metadata end-to-end: Mocapi, Substrate, Odyssey, and Ripcurl each ship their own `RuntimeHintsRegistrar`. This repo carries **zero** native-image-specific code; dropping `spring-boot-starter-data-jpa` or adding another MCP tool doesn't require touching hint files.

### Data model

Three entities, with UUID primary keys via `jpa-utils`' `BaseEntity`:

- **`Service`** — name, display name, description, business domain, owner `Team` (nullable — a deliberately orphaned service is part of the demo), `LifecycleStage` (`ACTIVE` / `DEPRECATED` / `RETIRING`), repo URL, runbook URL, and a free-form tag set (`pii`, `pci`, `soc2-scope`, `customer-facing`, `foundation`, `gdpr`).
- **`Team`** — name, display name, on-call rotation handle, primary Slack channel.
- **`Dependency`** — directed edge from one `Service` to another, plus a `DependencyType` (`CALLS`, `READS_FROM`, `PUBLISHES_TO`, `CONSUMES_FROM`).

The catalog is loaded into H2 on startup from [`CatalogSeeder`](src/main/java/com/callibrity/cowork/connector/catalog/seed/CatalogSeeder.java) and thrown away when the process exits. DTOs in [`catalog/dto/`](src/main/java/com/callibrity/cowork/connector/catalog/dto/) are the wire shape — entities never leak into tool or service responses.

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

## CI / Release

- `.github/workflows/ci.yml` — `mvn verify` on every push and PR to `main`
- `.github/workflows/release.yml` — on tag push, builds a native image via buildpacks and publishes it to GHCR (`ghcr.io/<owner>/<repo>:<tag>` and `:latest`). Runs on `ubuntu-latest` (amd64) — suitable for Azure Container Apps and other amd64 hosts. On Apple Silicon, `docker run` will fall back to Rosetta emulation, which works but doesn't reflect production startup numbers.

## Extending

The catalog model is intentionally compact — three entities (`Service`, `Team`, `Dependency`), four enums, nine tools. Real CMDBs have dozens of fields (SLOs, cost center, data classification, compliance scope). Adding them is additive: extend the entity, re-expose the field in the DTO, either extend an existing tool or add a new one. The native-image metadata story doesn't change — Mocapi's per-bean AOT processor covers new `@ToolMethod` signatures automatically.
