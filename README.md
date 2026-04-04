# Cowork Connector Example

An example MCP connector for [Claude Cowork](https://claude.ai/cowork), built with Spring Boot and [Mocapi](https://github.com/callibrity/mocapi).

This project demonstrates how to build a custom Cowork connector using the `mocapi-spring-boot-starter`. It exposes MCP tools and prompts over a secured HTTP endpoint that Cowork can connect to.

## What's Included

### Tools

| Tool | Description |
|---|---|
| `fs.list-directory` | Lists files and subdirectories at a given path |
| `fs.search-files` | Recursively searches for files matching a glob pattern |
| `system.info` | Returns OS name/version, hostname, CPU count, memory, and disk usage |
| `shell.run` | Runs a safe, read-only shell command (`date`, `df`, `echo`, `hostname`, `ls`, `pwd`, `uname`, `uptime`, `whoami`) |
| `todo.add` | Adds a new item to the in-memory to-do list |
| `todo.list` | Lists to-do items, filterable by `all`, `pending`, or `completed` |
| `todo.complete` | Marks a to-do item as completed |
| `todo.delete` | Removes a to-do item |

### Prompts

| Prompt | Description |
|---|---|
| `summarize-text` | Summarizes provided text at a `brief`, `standard`, or `detailed` level |
| `extract-action-items` | Extracts a numbered list of action items from meeting notes, emails, or documents |

## Prerequisites

- Java 24
- Maven 3.6+
- The `mocapi` library installed to your local Maven repository (see below)

## Building Mocapi Locally

This project depends on `mocapi` at version `0.0.1-SNAPSHOT`. If you haven't already, build and install it:

```bash
cd ~/IdeaProjects/mocapi
mvn install -DskipTests
```

## Configuration

Before running, configure your identity provider's issuer URI in `src/main/resources/application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-identity-provider.example.com
```

The application uses Spring Security as an OAuth2 Resource Server. All requests to the `/mcp` endpoint must include a valid JWT bearer token issued by the configured identity provider:

```
Authorization: Bearer <your-jwt-token>
```

The JWKS endpoint is auto-discovered from `<issuer-uri>/.well-known/openid-configuration`.

### Common Identity Provider Issuer URIs

| Provider | Issuer URI Format |
|---|---|
| Keycloak | `https://keycloak.example.com/realms/<realm-name>` |
| Auth0 | `https://<your-tenant>.us.auth0.com/` |
| Okta | `https://<your-domain>.okta.com/oauth2/default` |
| Azure AD | `https://login.microsoftonline.com/<tenant-id>/v2.0` |

## Running

```bash
mvn spring-boot:run
```

The MCP endpoint will be available at `http://localhost:8080/mcp`.

## Connecting to Cowork

1. Start the connector (`mvn spring-boot:run`)
2. In Cowork, go to **Settings → Connectors → Add Connector**
3. Set the URL to `http://localhost:8080/mcp`
4. Configure the bearer token for authentication

## Project Structure

```
src/main/java/com/callibrity/cowork/connector/
├── CoworkConnectorApplication.java   # Spring Boot entry point
├── config/
│   └── SecurityConfig.java           # OAuth2 Resource Server / JWT security
├── tools/
│   ├── FileSystemTool.java           # fs.list-directory, fs.search-files
│   ├── SystemInfoTool.java           # system.info
│   ├── ShellCommandTool.java         # shell.run (allowlisted commands only)
│   └── TodoListTool.java             # todo.add/list/complete/delete
└── prompts/
    └── SummaryPrompts.java           # summarize-text, extract-action-items
```

## Extending This Example

To add your own tools, create a new class annotated with `@Component` and `@ToolService`, then annotate methods with `@Tool`. Use `@Schema(description = "...")` on parameters to provide descriptions that appear in the MCP tool schema.

```java
@Component
@ToolService
public class MyCustomTool {

    @Tool(name = "my-tool.do-something", description = "Does something useful")
    public MyResponse doSomething(
            @Schema(description = "Input value") String input) {
        // ...
        return new MyResponse(result);
    }

    public record MyResponse(String result) {}
}
```

To add prompts, use `@PromptService` and `@Prompt`, returning a `GetPromptResult`.
