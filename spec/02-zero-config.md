# Zero-Config (or Near Zero) Builds

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## The Goal

For containerized enterprise apps that don't publish to Maven: how close to zero-config can we get?

## What Can We Assume?

| Aspect              | Assumption                    |
| ------------------- | ----------------------------- |
| Deployment          | Container (Docker/OCI)        |
| Artifact publishing | Not needed                    |
| Coordinates (GAV)   | Not needed                    |
| Signing             | Not needed                    |
| Java version        | From container base image     |
| Source layout       | Convention over configuration |

## True Zero-Config

```bash
$ ls
src/
  main/
    java/
      com/example/App.java

$ klx build
# builds ./build/libs/app.jar

$ klx container-build
# builds container image
```

## What Needs Configuration?

| Element                | Zero-Config Approach                                               | When Explicit Config Needed         |
| ---------------------- | ------------------------------------------------------------------ | ----------------------------------- |
| **Source directories** | Convention: `src/main/java`, `src/test/java`                       | Non-standard layouts                |
| **Main class**         | Auto-detect: single class with `public static void main(String[])` | Multiple mains, explicit selection  |
| **Java version**       | From `JAVA_HOME` or container                                      | Cross-compilation, specific targets |
| **Dependencies**       | `klx add org.slf4j:slf4j-api:2.0.9` generates lock file            | N/A (must declare somehow)          |
| **Output name**        | Derived from directory name                                        | Explicit branding                   |

## The Minimal Config

```yaml
# kalix.yaml - only what's non-obvious
dependencies:
  - org.slf4j:slf4j-api:2.0.9
  - ch.qos.logback:logback-classic:1.4.14
```

Or even: dependencies declared via CLI only, stored in `kalix.lock`:

```bash
klx add org.slf4j:slf4j-api:2.0.9
klx add --test org.junit.jupiter:junit-jupiter:5.11.0
```

## Auto-Detection Rules

### Finding the Main Class

1. Single class with `public static void main(String[])` → use it
2. Multiple classes with `main` → error: "Multiple main classes found. Specify with: `kalix.main = com.example.App`"
3. No `main` found → assume library (no executable JAR)

### Source Layout Detection

Check in order:

1. Standard Maven layout (`src/main/java`) → use it
2. Flat layout (`src/*.java`) → use it
3. Custom → require explicit `sources:` config

### Java Version

1. `JAVA_HOME` environment variable
2. Container image JDK version
3. Latest LTS as fallback

## What Breaks Zero-Config?

| Scenario                         | Solution                                         |
| -------------------------------- | ------------------------------------------------ |
| Multi-module workspace           | `modules:` section or workspace file             |
| Annotation processors            | Auto-detected from classpath, or explicit config |
| Native image (GraalVM)           | `klx native-build` with detected config          |
| Code generation (protobuf, etc.) | Plugin/DSL config required                       |
| Custom packaging (WAR, EAR)      | Explicit `packaging:` config                     |

## The "Enterprise Container" Sweet Spot

```yaml
# kalix.yaml - enterprise microservice
# Everything else is convention or auto-detected

dependencies:
  - org.springframework.boot:spring-boot-starter-web:3.2.0

container:
  base: eclipse-temurin:21-jre-alpine
  port: 8080
```

```bash
$ klx build              # builds jar
$ klx container-build    # builds container image
$ klx deploy --dry-run   # show K8s manifest
```

## Questions

1. Is `kalix.yaml` even needed if we can infer dependencies from imports? (Would require compiler integration)
2. Should `klx add` modify `kalix.yaml` or a separate lockfile?
3. How do we handle version conflicts without explicit resolution strategy?
