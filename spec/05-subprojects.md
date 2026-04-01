# Subprojects

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## Naming

We prefer **module**, but Maven and Gradle have established **subproject**. Kalyx uses **subproject** for consistency with existing Java tooling.

> A subproject may produce 0..n **modules** (where module ≈ JAR artifact).

## Zero-Config Subprojects

Root requires configuration. Subprojects do not.

```
my-app/
├── kalyx.yaml           # Root required
├── api/                 # Subproject - no config needed
│   └── src/main/java/
├── service/             # Subproject - no config needed
│   └── src/main/java/
└── util/                # Subproject - no config needed
    └── src/main/java/
```

### Root Configuration

```yaml
# kalyx.yaml (root)
project:
  name: my-app

subprojects:
  # Auto-discovered by convention, or explicit:
  include:
    - api
    - service
    - util
```

Zero-config subprojects inherit from root:

- Group ID
- Version
- Repositories
- Plugin versions
- Java version

### Subproject Identity

```
api/
├── kalyx.yaml           # Optional - only if non-default
└── src/main/java/
```

If `kalyx.yaml` exists in a subproject, it layers with root:

```yaml
# api/kalyx.yaml
project:
  name: my-app-api # Override artifact name

dependencies:
  compile:
    - io.projectreactor:reactor-core:3.6.0
```

## Subproject Dependencies

Subprojects can depend on each other:

```yaml
# service/kalyx.yaml (optional)
dependencies:
  compile:
    - project:api # Depends on :api subproject
    - org.springframework.boot:spring-boot-starter:3.2.0
```

Or via directory structure (zero-config):

```
service/
└── src/main/java/
    └── Service.java     # Imports from api/src/main/java
```

Kalyx infers the dependency from imports (compiler integration) or requires explicit declaration.

## Artifact Naming

| Subproject Path                     | Default Artifact Name | Coordinates (with root group: com.example) |
| ----------------------------------- | --------------------- | ------------------------------------------ |
| `api`                               | `api`                 | `com.example:api:1.0.0`                    |
| `service`                           | `service`             | `com.example:service:1.0.0`                |
| `util/logging`                      | `logging`             | `com.example:logging:1.0.0`                |
| `util/logging` (with name override) | `util-logging`        | `com.example:util-logging:1.0.0`           |

## Multi-Module Output

A subproject can produce multiple artifacts:

```yaml
# api/kalyx.yaml
project:
  name: my-app-api

modules:
  - name: api # Main API
  - name: api-test # Test fixtures
    sources: src/testFixtures/java
  - name: api-processor # Annotation processor
    sources: src/processor/java
```

Produces:

- `com.example:my-app-api:1.0.0`
- `com.example:my-app-api-test:1.0.0`
- `com.example:my-app-api-processor:1.0.0`

## Maven Compatibility Shim

The Java ecosystem has thousands of tools that understand `pom.xml`. Kalyx provides bidirectional compatibility:

### Reading pom.xml (Migration)

For gradual migration, Kalyx can read `pom.xml` as input:

```
legacy-module/
├── pom.xml              # Read by Kalyx
└── src/main/java/       # Standard layout
```

```yaml
# root kalyx.yaml
subprojects:
  include:
    - legacy-module:
        format: maven # Treat pom.xml as authoritative for this subproject
```

Kalyx reads:

- `<dependencies>` and `<dependencyManagement>`
- `<groupId>`, `<artifactId>`, `<version>`
- `<properties>` for variable substitution
- `<repositories>` and `<pluginRepositories>`

Does NOT support:

- Maven plugins (`<build><plugins>`)
- Complex POM inheritance (parent POM resolution)
- Profile activation logic

### Generating pom.xml (Ecosystem Interop)

Kalyx generates `pom.xml` for tools that require it:

```bash
$ klx pom-generate
# Generates pom.xml from kalyx.yaml for IDE/tools
```

```
api/
├── kalyx.yaml           # Source of truth
├── pom.xml              # Generated (can be gitignored)
└── src/main/java/
```

Tools that work with generated `pom.xml`:

- IDEs (IntelliJ, Eclipse) for dependency resolution
- Dependabot, Renovate for automated dependency updates
- OWASP Dependency Check for security scanning
- SonarQube for analysis
- GitHub's dependency graph

**Note on Renovate/Dependabot:** These tools understand Maven `pom.xml` natively. By generating `pom.xml`, Kalyx projects get automated dependency management without waiting for Kalyx-specific support in those tools. Renovate will update versions in `pom.xml`; `klx pom-sync` (or similar) can backport those changes to `kalyx.yaml` if needed.

The generated `pom.xml` is a **derived file** - `kalyx.yaml` remains the source of truth. CI builds use `klx`, not Maven.

## Comparison

| Feature            | Maven            | Gradle            | Kalyx                     |
| ------------------ | ---------------- | ----------------- | ------------------------- |
| Root required      | `pom.xml`        | `settings.gradle` | `kalyx.yaml`              |
| Subproject config  | `pom.xml`        | `build.gradle`    | Optional (zero-config)    |
| Inclusion          | `<modules>`      | `include()`       | `subprojects.include`     |
| Cross-project deps | `<dependency>`   | `project(':api')` | `project:api` or inferred |
| Multi-artifact     | Separate modules | Source sets       | `modules:` list           |

## Directory Layout Examples

### Flat Layout (Gradle-style)

```
my-app/
├── kalyx.yaml
├── api/
│   └── src/main/java/
├── service/
│   └── src/main/java/
└── util/
    └── src/main/java/
```

### Nested Layout (Maven-style)

```
my-app/
├── kalyx.yaml
└── modules/
    ├── api/
    │   └── src/main/java/
    └── service/
        └── src/main/java/
```

### Mixed (Migration)

```
my-app/
├── kalyx.yaml              # Root
├── new-api/                # Kalyx native
│   └── src/main/java/
└── legacy-service/         # Maven compatibility
    ├── pom.xml
    └── src/main/java/
```

## Questions

1. Should subproject auto-discovery be the default, or explicit inclusion only?
2. How do we handle circular dependencies between subprojects?
3. Should we support `exclude` for subprojects (e.g., skip `examples/`)?
4. Is the `pom.xml` shim actually useful, or should migration be "rewrite to YAML"?
