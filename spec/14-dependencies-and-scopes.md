# Dependencies and Scopes

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft
> **Note:** Core dependency management specification

## Philosophy: Maven Syntax, Gradle Correctness

Kalyx uses **Maven 4 polyglot YAML syntax** (`pom.yml`) for familiarity, with **Gradle's correct classpath modeling** internally.

## Maven Scopes (Copy-Paste Compatible)

```yaml
# Maven 4 polyglot YAML syntax (pom.yml)
# group:artifact:version@scope shorthand
dependencies:
  - org.slf4j:slf4j-api:2.0.0@compile
  - javax.servlet:servlet-api:2.5@provided
  - org.postgresql:postgresql:42.7.0@runtime
  - org.junit:junit:5.11.0@test
  - org.springframework.boot:spring-boot-dependencies:3.2.0@bom
```

**Note:** This is actual Maven 4 polyglot YAML syntax. A valid `pom.yml` from Maven 4 should work in Kalyx with minimal changes.

## Scope Mapping

| Maven Scope         | Available At           | Transitive | Gradle Equivalent    |
| ------------------- | ---------------------- | ---------- | -------------------- |
| `compile` (default) | compile + runtime      | Yes        | `implementation`     |
| `provided`          | compile only           | No         | `compileOnly`        |
| `runtime`           | runtime only           | Yes        | `runtimeOnly`        |
| `test`              | test compile + runtime | No         | `testImplementation` |
| `bom`               | version management     | N/A        | `platform`           |

**Classpath configurations** (derived from scope):

- `compileClasspath` = `compile` + `provided`
- `runtimeClasspath` = `compile` + `runtime`
- `testCompileClasspath` = `compile` + `provided` + `test`
- `testRuntimeClasspath` = `compile` + `runtime` + `test`

## Extended Scopes

Beyond Maven's basic scopes, Kalyx adds:

| Scope                 | Available At     | Transitive | Use Case               |
| --------------------- | ---------------- | ---------- | ---------------------- |
| `development`         | local run + test | No         | DevTools, debug agents |
| `annotationProcessor` | compile only     | No         | Code generation        |

```yaml
dependencies:
  - org.springframework.boot:spring-boot-devtools:3.2.0@development
  - org.immutables:value:2.10.0@annotationProcessor
```

## BOM Support

### Importing a BOM

```yaml
dependencies:
  - org.springframework.boot:spring-boot-dependencies:3.2.0@bom
```

**Why `@bom`?** Because everyone calls it a BOM. Maven uses `@import` for historical reasons, Gradle uses `platform`, but "BOM" is the universal term.

### Local BOM Definition

```yaml
# my-company-bom/kalyx.yaml
project:
  name: my-company-bom
  version: 1.0.0
  type: bom

dependencies:
  compile:
    - org.postgresql:postgresql:42.7.0
    - org.slf4j:slf4j-api:2.0.0
```

Used by other projects:

```yaml
dependencies:
  - com.mycompany:my-company-bom:1.0.0@bom

  compile:
    # Version inherited from BOM
    - org.postgresql:postgresql
    - org.slf4j:slf4j-api
```

### Version Overrides

```yaml
dependencies:
  - org.springframework.boot:spring-boot-dependencies:3.2.0@bom

  compile:
    # Use BOM version
    - org.springframework.boot:spring-boot-starter

    # Override specific version from BOM
    - org.postgresql:postgresql:43.0.0
```

## Open Design Questions

> **🚧 Design TBD:** Exact scope naming is still being determined.
>
> Maven terms (`@compile`, `@provided`) are familiar but misleading:
>
> - `@compile` sounds like "compile only" but means "compile + runtime"
>
> Gradle terms (`@implementation`, `@api`) are accurate but verbose.
>
> Decision deferred to elaboration phase after multi-module builds reveal real needs.

## Related

- [06-locks-and-resolution.md](06-locks-and-resolution.md) - Lock file behavior
- [07-versions-and-resolutions.md](07-versions-and-resolutions.md) - Version syntax
- [13-real-world-requirements.md](13-real-world-requirements.md) - Pain points that drove this design
