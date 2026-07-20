# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What Karnak is

Karnak is a DICOM gateway for medical-imaging data de-identification and DICOM
attribute normalization. It runs as a continuous DICOM listener (C-STORE SCP),
applies per-destination de-identification / tag-morphing pipelines, and forwards
studies to one or more destinations over either the DICOM protocol or DICOMWeb
(STOW-RS). It is a single Spring Boot application that also serves a Vaadin web
UI for configuration and monitoring.

## Build & run commands

Requires JDK 25 (pom enforces `[25,)`) and Maven 3.3+. Note the README's "JDK 21"
line is stale — the pom now targets Java 25.

```bash
# Build the deployable jar (production profile is active by default; builds the
# Vaadin frontend via pnpm)
mvn clean install -P production

# Build the self-contained portable package (embedded H2 + in-memory cache,
# output under build-portable/target/)
mvn clean install -Pportable

# Run all tests
mvn test

# Run a single test class / method
mvn test -Dtest=ProfileTest
mvn test -Dtest=ProfileTest#methodName

# Coverage report (JaCoCo; excludes frontend)
mvn test -Pcoverage
```

Tests and runtime require the native OpenCV library. The pom wires
`-Djava.library.path` automatically (`maven-dependency-plugin` copies the
platform-specific `libopencv_java` into `target/classes/lib/<os>-<cpu>` during
`generate-test-resources`, and surefire's `argLine` points the JVM at it). If you
run the app outside Maven (e.g. IntelliJ), you must set the lib path manually —
see the IntelliJ section below.

### Writing tests

Constraints for any new or edited test:

- **Prefer real data over mocks.** When possible, build real objects or reuse
  existing data structures / fixtures instead of fabricating mock data. Only mock
  what is genuinely awkward to instantiate (external services, I/O, time).
- **Frameworks: JUnit 6 (Jupiter) and Mockito only.** Do **not** use AssertJ
  (`org.assertj` / `Assertions.assertThat`) — use JUnit 6's built-in assertions
  (`org.junit.jupiter.api.Assertions`) and Mockito for stubbing/verification.
- **Readable test names:** annotate test classes with
  `@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)` and name
  methods with underscores (e.g. `void returns_default_when_config_is_null()`) so
  display names read as plain sentences instead of adding `@DisplayName` everywhere.
- **Use Java 25 best practices** (records for fixtures, pattern matching, `var`
  where it aids readability, text blocks, `List.of` / `Stream` idioms, etc.).

## Running locally for development

1. Start Postgres + Redis: `cd docker && docker compose up -d` (copy
   `.env.example` to `.env` first).
2. Run `org.karnak.KarnakApplication` with:
   - VM option: `-Djava.library.path="/tmp/dicom-opencv"` (the last folder must be
     `dicom-opencv`; adapt the temp path to your OS).
   - Env vars: `ENVIRONMENT=DEV` (mandatory). Defaults that match the docker setup:
     `DB_PORT=5433`, `DB_USER=karnak`, `DB_PASSWORD=karnak`, `DB_NAME=karnak`,
     `DB_HOST=localhost`, `DB_ENCRYPTION_KEY=...`, `KARNAK_ADMIN=admin`,
     `KARNAK_PASSWORD=karnak`, `IDP=undefined`.
3. Web UI defaults to `http://<host>:8081`.

`DB_ENCRYPTION_KEY` is mandatory — it is passed to Postgres via
`connection-init-sql` (`set_config('encryption.key', ...)`) and used for
column-level encryption of secrets.

## Code formatting

Two formatting plugins are bound to the build: `spring-javaformat`
(Spring code style) and `spotless` (enforces the EPL-2.0 OR Apache-2.0 license
header on every Java file). New/edited files must carry that header — copy it from
any existing source file. CI/Sonar will flag style violations.

## Architecture

The codebase splits cleanly into two halves under `src/main/java/org/karnak`:

- **`backend/`** — Spring data, services, DICOM/de-identification logic, config,
  security, REST controllers.
- **`frontend/`** — Vaadin views and components (excluded from JaCoCo coverage).

`KarnakApplication` is the Spring Boot entry point. If env var `IDP=oidc`, it
activates the `oidc` Spring profile (`application-oidc.yml`) for OpenID Connect;
any other value falls back to in-memory users. The default active profile is
`redis` (`application-redis.yml`); the portable build swaps in `portable`
(H2 file DB + in-memory cache instead of Postgres + Redis).

### DICOM forwarding flow (the core)

1. **`service/gateway/GatewayService`** implements
   `ApplicationListener<ContextRefreshedEvent>` and starts the DICOM gateway
   (`DicomGatewayService`) on context refresh — this is the C-STORE SCP listener.
2. **`service/gateway/GatewaySetUpService`** loads forward-node configuration from
   the DB into in-memory `ForwardDicomNode → List<ForwardDestination>` maps. It
   polls every 5 s (`@Scheduled(fixedRate=5000)` `checkRefreshGatewaySetUp`) and
   reloads when configuration changes and no transfer is in progress.
3. Incoming objects are handled by **`service/CStoreSCPService`** /
   **`service/StoreScpForwardService`**, then **`service/ForwardService`** applies
   per-destination `AttributeEditor`s (de-identification, tag morphing, masking,
   defacing, transfer-syntax adaptation) and stores to the destination via
   `StoreFromStreamSCU` (DICOM) or `DicomStowRS` (DICOMWeb).

A **source** can fan out to multiple **destinations**; each destination is DICOM
or DICOMWeb and carries its own profile. Sources are authenticated by AE Title
and/or hostname (`SourceNodeService`).

### De-identification / profile pipeline

- Profiles are ordered lists of profile *items*. The item types live in
  `backend/model/profiles/` (e.g. `BasicProfile`, `ActionTags`, `ActionDates`,
  `CleanPixelData`, `Defacing`, `AddTag`, `UpdateUIDsProfile`, `ReplaceApi`), all
  extending `AbstractProfileItem` / implementing `ProfileItem`.
- **`service/profilepipe/ProfilePipeService`** + `Profile` orchestrate applying the
  pipeline to a study's `Attributes`. `HMAC` / `HashContext` /
  `PatientMetadata` (in `model/profilepipe/`) drive deterministic pseudonymization.
- Pseudonym mapping uses a cache: `backend/cache/` (`ExternalIDCache`,
  `PatientClient`, backed by Redis or in-memory depending on profile). Mappings can
  also be supplied via CSV (`extid` frontend feature).

### Persistence

- JPA entities in `backend/data/entity/` (`ForwardNodeEntity`,
  `DestinationEntity`, `DicomSourceNodeEntity`, `ProfileEntity`,
  `ProfileElementEntity`, `ProjectEntity`, `KheopsAlbumsEntity`,
  `TransferStatusEntity`, etc.), repos in `backend/data/repo/`.
- **Schema is managed by Liquibase, not Hibernate** (`ddl-auto: none`). Any schema
  change must be added as a new changeset under
  `src/main/resources/db/changelog/changes/` and referenced from
  `db.changelog-master.yaml`.
- Secret columns are encrypted at the DB layer (see `SecretEntity` /
  `SecretService` and the `encryption.key` config above).

### Other notable pieces

- **REST API**: `backend/controller/` (`EchoController` at the echo path) and
  `backend/api/` (KHEOPS integration via `KheopsApi`, `EchoServlet`). Swagger
  annotations are present (springdoc).
- **KHEOPS** album/token integration: `service/kheops/`, `frontend/kheops/`.
- **Notifications**: `service/NotificationService` + Thymeleaf email templates in
  `src/main/resources/templates/`.
- **Security**: `backend/config/SecurityConfiguration` (OIDC) vs
  `SecurityInMemoryConfig`, roles in `enums/SecurityRole`.
- Enums in `backend/enums/` are the canonical source for cross-cutting constants
  (`DestinationType`, `ProfileItemType`, `PseudonymType`, `UIDType`,
  `EnvironmentVariable`, etc.).

## Conventions

- Lombok is used throughout (`@Slf4j`, getters/setters) — it's an annotation
  processor, configured in the compiler plugin.
- The Vaadin frontend build is driven by Maven (`vaadin-maven-plugin`,
  `pnpmEnable=true`); `package.json` / `vite.config.ts` exist but you normally
  don't run npm/pnpm directly — let the Maven production/portable profile do it.
- **Do not use Lumo** in frontend styling — the Lumo theme and its CSS custom
  properties (`var(--lumo-*)`, e.g. `--lumo-space-m`, `--lumo-primary-color`) are
  deprecated and must not be used in new or edited code. Use concrete CSS values
  (e.g. `1rem`, `16px`) or the project's own styles instead.
