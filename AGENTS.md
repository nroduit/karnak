# AGENTS.md

Guidance for AI coding agents and human contributors working in this repository.
The nearest `AGENTS.md` up the directory tree applies.
Read [CONTRIBUTING.md](CONTRIBUTING.md) for how issues and pull requests are handled.

## Project overview

Karnak is a DICOM gateway for de-identification, tag morphing and DICOM conformance checks.
It receives studies through a DICOM listener, transforms them with YAML profiles, and forwards them over DICOM (C-STORE) or DICOMweb (STOW-RS); everything is configured and monitored from a Vaadin web portal.
It is a Spring Boot application with a Vaadin frontend, Liquibase for the database schema (Postgres, or embedded H2 in the portable build), Redis or an in-memory cache, and [weasis-dicom-tools](https://github.com/nroduit/weasis-dicom-tools) for the DICOM stack.
It requires **JDK 25** (`java.version` in `pom.xml`) and Maven.
The [user guide](https://weasis.org/karnak-documentation/) lives in a separate repository.

## Commands

```bash
mvn spring-javaformat:apply spotless:apply              # format, import order, license header (before committing)
mvn -B verify                                           # build and tests
mvn -B verify -Pcoverage                                # what CI runs
mvn test -Dtest=ClassName#methodName                    # one test
mvn test -Pperformance                                  # the slow "performance" tests, skipped by default
mvn clean install -P production                         # deployable jar with the production frontend (docker image)
mvn clean install -Pportable                            # portable package (embedded H2, in-memory cache)
```

Run `mvn clean install` once before running from the IDE: it copies the native OpenCV library into `target/classes/lib/<os>-<cpu>/`, which the gateway loads at startup.
The README describes the IDE launcher, the environment variables and the local Postgres and Redis containers.

## Definition of done

A change is complete when:

- Both formatters have been applied and `mvn -B verify` passes.
- New behavior has tests, and a fixed bug has a test that failed before the fix.
- A change that can affect which attributes are kept, removed or replaced comes with tests on the resulting DICOM output: a regression there leaks patient data in production.
- A schema change comes with a new Liquibase changelog and works on both Postgres and H2.
- A doc in `docs/` that describes a changed concept is updated in the same change.
- The description of the change says how it was verified: which tests, which manual scenario, which synthetic dataset.

## Architecture

- `org.karnak.backend`: the server side.
  `api` (REST API), `controller`, `service` (forwarding in `ForwardService`, the profile pipeline in `service.profilepipe`), `dicom` (listener and DICOM helpers), `model` (profiles, de-identification, images, patients), `data` (JPA entities and repositories), `cache` (pseudonym mappings), `config`, `security`, `h2` (portable build), `util`.
- `org.karnak.frontend`: the Vaadin views, one package per screen (`forwardnode`, `profile`, `project`, `extid`, `monitoring`, `dicom` tools, `authconfig`, …) with shared `component` and `util` packages.
  Client-side code and themes live in `src/main/frontend/`.
- `src/main/resources`: `application.yml` and the profile files (`application-portable.yml`, `application-redis.yml`, `application-oidc.yml`), the Liquibase changelogs in `db/changelog/changes/` (included in order by `db.changelog-master.yaml`), the OpenAPI contract of the OCR de-identification service (`deidentification-api.yaml`), and the e-mail templates.
- `docs/`: developer guides for the main mechanisms (forwarding, gateway integration tests, sequence tag paths, conformance report, automatic pixel de-identification, profile builder).
  Read the relevant one before changing that area.

## Code conventions

- **Formatting**: spring-javaformat for the code style, Spotless for the import order (static imports first) and the EPL-2.0 OR Apache-2.0 license header; CI and Sonar flag any violation.
- **De-identification first**: never weaken an action of the DICOM PS3.15 basic profile or a profile item silently.
  An intentional deviation from the DICOM standard or an IHE profile carries a short comment that says what the standard requires and why the code diverges, citing the part and section.
- **Logging**: use SLF4J through Lombok `@Slf4j` with `{}` placeholders, never `System.out` or `printStackTrace()`.
  Never log patient identifiers, project secrets or tokens above `DEBUG`.
- **Secrets**: project secrets and credentials stay encrypted at the database layer and come from the environment or secret files; never hard-code them or write them to logs, exports or exceptions.
- **Database**: never edit a Liquibase changeset that has been released; add a new `db.changelog-<version>.xml` instead.
- **Vaadin UI**: user-visible strings stay consistent with the existing screens; long work (forwarding, queries, exports) never runs on the UI thread, and the UI is updated through `UI.access`.
- **Java**: records for value types, Lombok only where the surrounding code uses it, `Path` and `Files` over `java.io.File`, `Optional` returns rather than `null` at API boundaries.
- **Javadoc**: keep it minimal.
  A private method gets at most a one-line comment, or none when its name and signature are clear.
  A public API gets one short sentence on its intent, with `@param` / `@return` / `@throws` only when they add what the signature does not say.
- **Code quality**: small focused methods, expressive names, early returns.
  Remove redundant code: unused imports and locals, dead branches, duplicated logic, comments that repeat the code.

## Testing

- Use **JUnit Jupiter** (`org.junit.jupiter.*`), **Mockito** and Spring Boot test support.
  Use JUnit's `Assertions`; a few tests use AssertJ, do not spread it to new ones.
- Tests mirror the main packages under `src/test/java`; fixtures go in `src/test/resources` and are synthetic DICOM only.
- The gateway integration tests start a real C-STORE SCP on a loopback port (see `docs/gateway-integration-tests.md`); slow saturation tests carry the `performance` tag and run only with `-Pperformance`.
- Tests need no external Postgres, Redis or network, and write files only under a `@TempDir`.

## Writing conventions

These rules apply to Markdown docs, Javadoc, comments, UI messages and commit messages.

- **American English**: center, color, gray, behavior, normalize, anonymize, de-identify, license (noun).
  DICOM keywords and external API names keep their own spelling.
- **Markdown**:
  - One sentence per line, so that diffs show the changed sentence only.
  - A blank line before and after headings, lists, tables, code blocks and quotes; never more than one blank line.
  - A hard line break is a trailing `\`, never two trailing spaces.
  - CommonMark with GFM tables and fenced code blocks that name their language (`java`, `bash`, `yaml`, `text`, …); no raw HTML.
  - Relative links between files in the repository, absolute links to the user guide.
- `.editorconfig` sets UTF-8, LF and a final newline; `.markdownlint.yaml` holds the Markdown rules a linter can check.

## Git and commits

- One logical change per commit; never mix formatting or refactoring with a functional change.
- Subject line: an imperative summary without a trailing period, at most about 72 characters, optionally with a type prefix (`feat:`, `fix:`, `style:`, `docs:`, `test:`, `build:`, `ci:`) as in the history.
  Reference the issue or pull request as `#123` when there is one.
- A body, after a blank line, explains why when the subject is not enough.
- Never push, force-push, rewrite published history or create a release unless explicitly asked.
- `CHANGELOG.md` is generated by the `changelog` workflow: never edit it by hand.

## Boundaries

### Never

- Commit or paste patient data: no real DICOM, screenshots, logs, database dumps or profiles containing names, IDs, accession numbers or other identifiers.
  Fixtures and examples are synthetic or fully anonymized.
- Commit secrets, real hostnames, AE Titles of real sites, tokens or project secrets; the development values of the README are the only ones allowed.
- Edit generated content: `target/`, `src/main/frontend/generated/`, `vite.generated.ts`, `CHANGELOG.md`.
- Disable or skip the formatters or tests to make a build pass.

### Ask first

- Any change to the de-identification pipeline, the pseudonymization, or the default actions of a profile.
- A database schema change, or a change to the security configuration (authentication, OIDC, roles).
- Adding, upgrading or removing a dependency or a Maven plugin; a new dependency must have a license compatible with EPL-2.0 OR Apache-2.0.
- Changing the REST API, the OCR service contract (`deidentification-api.yaml`), the YAML profile format or the configuration properties, which existing deployments and clients use.
- Changing the CI, docker or release workflows.
