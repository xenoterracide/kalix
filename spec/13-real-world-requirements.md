# Real-World Requirements

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## Must-Solve Pain Points

### 1. Error-Prone: Readable Compiler Arguments

**Maven Problem:** Error-prone arguments must be on one line, unreadable:

```xml
<compilerArgs>
  <arg>-Xplugin:ErrorProne -Xep:NullAway:ERROR -XepOpt:NullAway:AnnotatedPackages=com.example ...</arg>
</compilerArgs>
```

**Gradle Solution:** Works fine with multi-line strings

**Kalix Solution:** YAML supports multi-line strings naturally:

```yaml
# kalix.yaml
plugins:
  - io.kalix.java:2.0.0
  - io.kalix.errorprone:2.0.0

java:
  compiler:
    errorprone:
      enabled: true
      options:
        NullAway: ERROR
        NullAway:AnnotatedPackages: com.example
        # Or using YAML multi-line for complex cases
        rawOptions: |
          -Xep:MissingOverride:ERROR
          -Xep:DeadException:ERROR
          -Xep:UnusedVariable:WARN
```

Or external file:

```yaml
java:
  compiler:
    errorprone:
      optionsFile: errorprone-config.txt
```

```
# errorprone-config.txt
-Xep:NullAway:ERROR
-XepOpt:NullAway:AnnotatedPackages=com.example
-Xep:MissingOverride:ERROR
```

### 2. Annotation Processors and Javadoc

**Gradle Problem:** Javadoc doesn't understand annotation processor output. If you write Javadoc on an Immutables interface:

```java
/** My documented interface */
@Value.Immutable
public interface MyInterface {}
```

Gradle's `javadoc` task runs before the processor generates `ImmutableMyInterface`, so Javadoc is incomplete.

**Kalix Solution:** Explicit task dependencies and processor-aware source sets:

```yaml
plugins:
  - io.kalix.java:2.0.0
  - io.kalix.immutables:2.0.0

java:
  sourceSets:
    main:
      annotationProcessors:
        - org.immutables:value:2.10.0

    # Generated code is a separate source set with proper dependencies
    generated:
      extends: main
      sourceDir: build/generated/sources/annotationProcessor/java/main
      tasks:
        - javadoc # Javadoc task depends on generated source set
```

```yaml
# In generated source set config
sourceSets:
  generated:
    # Javadoc task explicitly depends on annotation processing
    javadoc:
      dependsOn: compileJava # Which triggers annotation processors
      sourceDirs:
        - src/main/java
        - build/generated/sources/annotationProcessor/java/main
```

Key: Tasks declare their inputs explicitly. Javadoc sees the generated sources as inputs.

### 3. Checkstyle Per Source Set

**Maven Problem:** One checkstyle config for entire project.

**Gradle Solution:** Can configure per source set.

**Kalix Solution:** Tools configured per source set:

```yaml
plugins:
  - io.kalix.java:2.0.0
  - io.kalix.checkstyle:2.0.0

java:
  sourceSets:
    main:
      checkstyle:
        config: config/checkstyle-main.xml

    test:
      checkstyle:
        config: config/checkstyle-test.xml # Relaxed rules for tests

    integrationTest:
      checkstyle:
        config: config/checkstyle-test.xml
```

Or at tool level:

```yaml
tools:
  checkstyle-main:
    uses: com.puppycrawl.tools:checkstyle:10.12.0
    plugin: java-exec

  checkstyle-test:
    uses: com.puppycrawl.tools:checkstyle:10.12.0
    plugin: java-exec

scripts:
  checkstyle:
    run:
      checkstyle-main: -c config/checkstyle-main.xml src/main/java
      checkstyle-test: -c config/checkstyle-test.xml src/test/java
```

### 4. Multiple Test Suites

**Current State:** Most projects need 1-4 test types:

- **Unit tests** (fast, isolated)
- **White-box** (unit with internals visible)
- **Integration** (database, external services)
- **E2E** (full system)

**Kalix Solution:** First-class test source sets:

```yaml
plugins:
  - io.kalix.java:2.0.0
  - io.kalix.junit:2.0.0

java:
  sourceSets:
    # Default unit tests
    test:
      junit:
        includeTags: [unit, fast]

    # Additional test suites
    integrationTest:
      extends: test
      sourceDir: src/integrationTest/java
      resourcesDir: src/integrationTest/resources
      junit:
        includeTags: [integration]
      dependencies:
        - org.testcontainers:testcontainers:1.19.0

    e2eTest:
      sourceDir: src/e2eTest/java
      junit:
        includeTags: [e2e]
      # Can run against the built application
      dependsOn: bootJar
```

Running:

```bash
# Run all tests
klx test

# Run specific suite
klx test integrationTest
klx test e2eTest

# Run multiple
klx test test integrationTest

# Tags within a suite
klx test --tags "slow" integrationTest
```

### 5. Test Fixtures

**Problem:** Test fixtures (shared test utilities) need to be:

- In the same subproject (for maintenance)
- Separate artifacts (for other projects to depend on)

**Kalix Solution:** Test fixtures as explicit module output:

```yaml
# api/kalix.yaml
plugins:
  - io.kalix.java:2.0.0

java:
  sourceSets:
    main:
      # Regular production code

    testFixtures:
      # Shared test utilities
      sourceDir: src/testFixtures/java
      # Becomes separate artifact: api-test-fixtures.jar
      publish: true

    test:
      # Depends on testFixtures automatically
      dependsOn: [testFixtures]
```

Another subproject using the fixtures:

```yaml
# service/kalix.yaml
dependencies:
  compile:
    - project:api
  testCompile:
    - project:api:
        classifier: test-fixtures # Or module: testFixtures
```

Or with explicit module syntax:

```yaml
dependencies:
  testCompile:
    - project:api:testFixtures # Depends on api's testFixtures source set
```

## Implementation Notes

### Source Sets Are First-Class

Source sets define:

- Source directories
- Resource directories
- Dependencies
- Attached tools (checkstyle, javadoc, etc.)
- Output artifacts

```yaml
java:
  sourceSets:
    main:
      sourceDir: src/main/java
      resourcesDir: src/main/resources
      output: jar # Produces main artifact

    testFixtures:
      sourceDir: src/testFixtures/java
      output: jar
      classifier: test-fixtures # api-test-fixtures.jar

    test:
      sourceDir: src/test/java
      output: none # Test classes not published
      dependsOn: [testFixtures] # Can use fixtures
```

### Task Dependencies Follow Data Flow

Tasks automatically depend on what produces their inputs:

```
javadoc
├── inputs: main.java.sources + main.annotationProcessor.outputs
├── dependsOn: compileJava (which triggers annotation processors)
└── outputs: build/docs/javadoc
```

No manual `dependsOn` needed for standard cases.

## Comparison

| Requirement               | Maven            | Gradle               | Kalix                  |
| ------------------------- | ---------------- | -------------------- | ---------------------- |
| Long compiler args        | ❌ Painful XML   | ✅ Works             | ✅ Native YAML support |
| Annotation proc + Javadoc | ❌ Manual config | ⚠️ Works but complex | ✅ Source set aware    |
| Checkstyle per source set | ❌ No            | ✅ Yes               | ✅ Native support      |
| Multiple test suites      | ❌ profiles hack | ⚠️ Source sets       | ✅ First-class         |
| Test fixtures             | ❌ No            | ✅ Yes (plugin)      | ✅ Native              |
| Optional/dev dependencies | ✅ `optional`    | ⚠️ `feature`         | ✅ Unified approach    |

## 6. Optional Dependencies and Capabilities

**The Problem:** Spring Boot DevTools should only be on classpath during development, not in production. This requires:

- Optional dependencies (don't transitively pull in)
- Development-only dependencies (not in production)
- Capability declarations (what features this artifact provides)

### Maven's Approach

```xml
<!-- Only for development -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <scope>provided</scope>
  <optional>true</optional>
</dependency>
```

Problems:

- `optional` is just a boolean - no semantics
- `provided` means different things in different contexts
- No way to express "this is a development-only feature"

### Gradle's Approach: "Feature Variants" Magic

Gradle lacks `optional` (disturbing). Has a feature called **feature variants**:

```groovy
// build.gradle
java {
  registerFeature('development') {
    usingSourceSet(sourceSets.main)
  }
}

dependencies {
  developmentImplementation 'org.springframework.boot:spring-boot-devtools'
}
```

What this "feature variant" magic does:

- Creates a configuration called `development`
- Creates a source set called `development` (even if you don't want one!)
- Creates publishing variants
- Creates artifact variants
- All tied together with implicit coupling

Problems:

- **Too magical** - Creates 5 things when you wanted 1
- **Forced coupling** - Can't have `development` scope without `development` source set
- **No simple "optional"** - Everything must be a "feature variant"
- **Complex debugging** - Which feature variant created which configuration?

### Kalix Approach: Maven 4 Polyglot + Gradle Thoroughness

Kalix uses Maven 4's polyglot YAML syntax (`pom.yml`) for compatibility, with Gradle's correct classpath modeling internally.

> **🚧 Design TBD:** Exact scope/role naming is still being determined. Options:
>
> - Maven terms (`@compile`, `@provided`, `@runtime`) - familiar but misleading
> - Gradle terms (`@implementation`, `@api`, `@compileOnly`) - accurate but verbose
> - JPMS-inspired (`@requires`, `@requires static`, `@uses`) - theoretically pure
>
> Decision deferred to elaboration phase after multi-module builds reveal real needs.

#### Standard Maven Scopes (Maven 4 pom.yml Compatible)

```yaml
# Maven 4 polyglot YAML syntax (pom.yml)
# group:artifact:version@scope shorthand
dependencies:
  - org.slf4j:slf4j-api:2.0.0@compile
  - javax.servlet:servlet-api:2.5@provided
  - org.postgresql:postgresql:42.7.0@runtime
  - org.junit:junit:5.11.0@test
  - org.springframework.boot:spring-boot-dependencies:3.2.0@import
```

**Note:** This is actual Maven 4 polyglot YAML syntax. A valid `pom.yml` from Maven 4 should work in Kalix with minimal changes.

**How Maven scopes map to Kalix:**

| Maven Scope         | Kalix Role | Available At           | Transitive |
| ------------------- | ---------- | ---------------------- | ---------- |
| `compile` (default) | `compile`  | compile + runtime      | Yes        |
| `provided`          | `provided` | compile only           | No         |
| `runtime`           | `runtime`  | runtime only           | Yes        |
| `test`              | `test`     | test compile + runtime | No         |
| `import`            | `import`   | BOM management         | N/A        |

**Gradle-style configurations** (derived from role):

- `compileClasspath` = `compile` + `provided`
- `runtimeClasspath` = `compile` + `runtime`
- `testCompileClasspath` = `compile` + `provided` + `test`
- `testRuntimeClasspath` = `compile` + `runtime` + `test`

#### Extended Roles for Modern Workflows

Beyond Maven's basic scopes, Kalix adds:

```yaml
dependencies:
  # Development-only (not in production)
  # Similar to Gradle's developmentOnly
  - org.springframework.boot:spring-boot-devtools:3.2.0@development

  # Optional dependency
  # Consumers don't get this unless they explicitly depend on it
  - com.example:optional-feature:1.0.0@compileOptional

  # Annotation processors
  - org.immutables:value:2.10.0@annotationProcessor
```

| Extended Role         | Available At      | Transitive | Use Case                   |
| --------------------- | ----------------- | ---------- | -------------------------- |
| `development`         | local run + test  | No         | DevTools, debugging agents |
| `compileOptional`     | compile + runtime | Optional   | Optional features          |
| `annotationProcessor` | compile only (AP) | No         | Code generation            |

#### Composite Role Syntax

Combine role + optional flag:

```yaml
# compile + optional = compileOptional
dependencies:
  - com.example:lib:1.0.0@compile+optional
  # Same as:
  - com.example:lib:1.0.0@compileOptional
```

#### Map-Based (Verbose) Alternative

For complex exclusions or when readability matters:

```yaml
dependencies:
  compile:
    - org.springframework.boot:spring-boot-starter-web:3.2.0:
        exclude:
          - org.springframework.boot:spring-boot-starter-logging

  provided:
    - javax.servlet:javax.servlet-api:4.0.1

  test:
    - org.junit.jupiter:junit-jupiter:5.11.0
```

#### The Design Tension

**Maven's model** is simple (5 scopes) but misses important distinctions:

- Can't express "test-only runtime dependency"
- No first-class annotation processor scope
- No development-only concept

**Gradle's model** is thorough (compileClasspath, runtimeClasspath, testCompileClasspath, etc.) but complex:

- Configurations are implementation details leaking out
- Feature variants create implicit configurations
- Hard to know which configuration to use

**Kalix approach:**

- **Simple case:** Maven scopes (`@compile`, `@test`, etc.) - copy from any pom.xml
- **Complex case:** Explicit classpath configuration when needed
- **Role expansion:** Roles expand to correct Gradle-style configurations internally

**Example expansion:**

```yaml
# User writes (simple):
dependencies:
  - org.postgresql:postgresql:42.7.0@runtime

# Kalix expands to (thorough):
# - NOT available at compile time
# - Available at runtime
# - Transitive to consumers
# - Part of runtimeClasspath, testRuntimeClasspath
```

Does this actually work better? We'll see. This is marked as experimental.

### BOM/Platform Support

Kalix supports Maven BOM (Bill of Materials) and Gradle Platform concepts for dependency version management:

#### Importing a BOM

```yaml
# Maven-style BOM import
dependencies:
  - org.springframework.boot:spring-boot-dependencies:3.2.0@import
```

BOM dependencies:

- Provide version numbers for transitive dependencies
- Not included in classpath themselves
- Versions can be overridden explicitly

#### Platform Dependencies (Gradle-style)

```yaml
# Gradle-style platform
dependencies:
  compile:
    - platform: org.springframework.boot:spring-boot-dependencies:3.2.0
```

#### Local BOM Definition

Define your own BOM for internal shared versions:

```yaml
# my-company-bom/kalix.yaml
project:
  name: my-company-bom
  version: 1.0.0
  type: bom # Mark as BOM, not library

dependencies:
  compile:
    # Versions managed by this BOM
    - org.postgresql:postgresql:42.7.0
    - org.slf4j:slf4j-api:2.0.0
    - com.fasterxml.jackson.core:jackson-databind:2.15.0
```

Used by other projects:

```yaml
# service/kalix.yaml
dependencies:
  - com.mycompany:my-company-bom:1.0.0@import

  compile:
    # Version inherited from BOM
    - org.postgresql:postgresql
    - org.slf4j:slf4j-api
```

#### Version Overrides

```yaml
dependencies:
  - org.springframework.boot:spring-boot-dependencies:3.2.0@import

  compile:
    # Use BOM version
    - org.springframework.boot:spring-boot-starter

    # Override specific version from BOM
    - org.postgresql:postgresql:43.0.0
```

### Capability Declarations

What if multiple libraries provide the same capability (e.g., logging)?

```yaml
dependencies:
  compile:
    - org.springframework.boot:spring-boot-starter-web:3.2.0:
        # Exclude default logging
        exclude:
          - org.springframework.boot:spring-boot-starter-logging

    # Provide logging capability via log4j
    - org.springframework.boot:spring-boot-starter-log4j2:3.2.0:
        provides: logging # Declares capability
```

Or at project level:

```yaml
project:
  capabilities:
    provides:
      - logging # This project provides logging
    requires:
      - jdbc # This project needs jdbc capability

dependencies:
  compile:
    # Will use any dependency that provides 'logging' capability
    - capability:logging:
        default: org.springframework.boot:spring-boot-starter-log4j2:3.2.0
```

### DevTools Example

```yaml
# kalix.yaml
plugins:
  - io.kalix.spring-boot:2.0.0

dependencies:
  compile:
    - org.springframework.boot:spring-boot-starter-web:3.2.0

  development:
    - org.springframework.boot:spring-boot-devtools:3.2.0:
        # DevTools is:
        # - On classpath during development (klx run, klx test)
        # - NOT in production JAR
        # - NOT transitive to consumers
        scope: [development, non-transitive]
```

```bash
# Development - devtools active
klx run

# Production build - devtools excluded
klx build --profile production
# Or: klx jar (production by default for packaging)
```

### Open Questions

1. Should `development` be a first-class scope or a profile?
2. How do we express "this is optional unless you ask for it"?
3. Should capabilities be inferred from JPMS `provides` clauses?
4. How to handle Gradle's "api" vs "implementation" distinction?

## 7. Error Messages That Don't Suck

Gradle's error messages are notoriously bad. But the problem isn't bad error message writers - it's that Gradle is so convoluted that errors are inherently hard to explain.

**Gradle complexity:**

- Dynamic Groovy/Kotlin DSL - errors happen at runtime
- Plugin interactions - which plugin added this task?
- Configuration phases - errors in `afterEvaluate` with no context
- Implicit dependencies - why did this task run?

**Kalix simplicity enables good errors:**

- Static YAML - errors at parse time with line numbers
- Explicit configuration - no "where did this come from"
- Single command - no task chaining confusion
- Clear plugin boundaries - errors attributed to specific plugins

### Example: Missing Dependency

**Gradle:**

```
> Could not resolve all dependencies for configuration ':compileClasspath'.
  > Could not find com.example:lib:1.0.0.
    Required by:
        project :
        project : > org.other:other-lib:2.0.0
```

(Which one required it? Why? Good luck.)

**Kalix:**

```
Error: Dependency resolution failed
  File: kalix.yaml, line 12

  dependencies:
    compile:
      - com.example:lib:1.0.0  # <-- Not found

  Searched repositories:
    - https://repo1.maven.org/maven2
    - https://nexus.company.com/

  Suggestion: Check the version number or add the correct repository.
```

### Error Design Principles

1. **Point to the exact file and line**
2. **Show the relevant config in context**
3. **Explain what was tried**
4. **Suggest how to fix it**
5. **No stack traces for user errors**
