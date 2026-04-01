# Spring App Commons: Gradle vs Kalyx Comparison

<!--
SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

## File Count

| Gradle Files                                    | Kalyx Files                            |
| ----------------------------------------------- | -------------------------------------- |
| `build.gradle.kts` (root)                       | `kalyx.yaml` (root)                    |
| `settings.gradle.kts`                           | (merged into root)                     |
| `gradle/libs.versions.toml`                     | (merged into root)                     |
| `buildSrc/build.gradle.kts`                     | (no equivalent - plugins are external) |
| `buildSrc/settings.gradle.kts`                  | (no equivalent)                        |
| `buildSrc/src/main/kotlin/our.*.gradle.kts` x 5 | (no equivalent)                        |
| `module/*/build.gradle.kts` x 8                 | `module/*/kalyx.yaml` x 8              |
| **Total: 17 files**                             | **Total: 9 files**                     |

## Lines of Code

### Root Configuration

**Gradle (build.gradle.kts + settings.gradle.kts): ~170 lines**

- settings.gradle.kts: 98 lines
- build.gradle.kts: 74 lines

**Kalyx (kalyx.yaml): ~70 lines**

### Module Configuration (commons-model example)

**Gradle (build.gradle.kts): 37 lines**

```kotlin
testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      dependencies {
        implementation(libs.java.tools)
        implementation(sbd4.jakarta.persistence.api)
        implementation(sbd4.spring.beans)
        implementation(sbd4.spring.boot.data.jpa.test)
        runtimeOnly(projects.testAppCore)
        runtimeOnly(sbd4.h2)
        runtimeOnly(sbd4.spring.boot.starter.data.jpa)
        runtimeOnly.bundle(libs.bundles.jakarta.transaction)
      }
    }
    withType<JvmTestSuite>().configureEach {
      dependencies {
        implementation.bundle(sbd4.bundles.test.impl)
        runtimeOnly.bundle(sbd4.bundles.test.runtime)
      }
    }
  }
}
```

**Kalyx (kalyx.yaml): ~25 lines**

```yaml
test:
  dependencies:
    implementation:
      - com.xenoterracide:tools:0.x
      - jakarta.persistence:jakarta.persistence-api:${jakarta}
      - org.springframework:spring-beans:${spring-boot}
      - org.springframework.boot:spring-boot-data-jpa-test:${spring-boot}
    runtimeOnly:
      - project:test-app-core
      - com.h2database:h2:2.x
      - org.springframework.boot:spring-boot-starter-data-jpa:${spring-boot}
```

## Complexity Comparison

### Convention Plugins (buildSrc)

**Gradle:**

- 5 convention plugin files
- Custom plugin logic for publishing, checkstyle, spotbugs, errorprone, coverage
- Inter-plugin dependencies and ordering
- Workarounds for Gradle bugs (plainJavadocJar/javadocJar conflict)

**Kalyx:**

- First-party plugins from `io.kalyx.*`
- No custom buildSrc needed
- Version updates via plugin version, not buildSrc changes

### Version Management

**Gradle:**

- `gradle/libs.versions.toml` - 77 lines
- External version catalog `com.xenoterracide.gradle.vc:version-catalog-spring-boot`
- Bundle definitions scattered
- `sbd4` catalog alias in settings.gradle.kts

**Kalyx:**

- Inline `versions:` section in root kalyx.yaml
- No external catalog dependency
- Bundle expansion automatic or explicit in dependencies

### Test Suites

**Gradle:**

```kotlin
testing {
  suites {
    val test by getting(JvmTestSuite::class) { ... }
    val testWhitebox by getting(JvmTestSuite::class) { ... }
    withType<JvmTestSuite>().configureEach { ... }
  }
}
```

**Kalyx:**

```yaml
test:
  # Default test suite
  dependencies: ...
  suites:
    whitebox: # Additional suite
      dependencies: ...
```

### Dependency Locking

**Gradle:**

```kotlin
// In every single build.gradle.kts
buildscript { dependencyLocking { lockAllConfigurations() } }
dependencyLocking { lockAllConfigurations() }
```

**Kalyx:**

```yaml
# In root only - inherited by all subprojects
# Lock files always enabled, always immutable
```

## What Kalyx Removes

1. **buildSrc/** - Custom convention plugins → First-party plugins
2. **gradle/libs.versions.toml** → Inline versions in kalyx.yaml
3. **settings.gradle.kts complexity** → Auto-discovery, no component rules
4. **Dependency locking boilerplate** → Always on, inherited
5. **Task configuration** → Sensible defaults, minimal override
6. **Plugin ordering workarounds** → Explicit dependency model

## What Kalyx Adds

1. **Layered configuration** - Share standards via `.config/kalyx/`
2. **First-class scripts** - `klx tool` for ad-hoc tasks
3. **Immutable versions** - Security by default
4. **Simpler test suites** - YAML instead of Kotlin DSL
5. **Clearer inheritance** - Subprojects inherit from root explicitly

## Migration Path

High-value targets for migration:

1. **Dependency declarations** - Straightforward YAML conversion
2. **Test suite configuration** - Much simpler in Kalyx
3. **Version catalog** - Consolidate into root
4. **Convention plugins** - Replace with first-party plugins

Challenging areas:

1. **Custom build logic** - May need custom plugins
2. **Complex dependency rules** - Component metadata rules in Gradle
3. **Integration with external plugins** - Need Kalyx equivalents
