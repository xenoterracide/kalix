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

### Kalix Approach: Dependency Roles + Maven 4 Shorthand

Kalix supports both **map-based** (explicit) and **string-based** (shorthand) dependency declarations:

#### Map-Based (Explicit)

```yaml
# kalix.yaml
dependencies:
  # Regular compile dependency
  compile:
    - org.springframework.boot:spring-boot-starter-web:3.2.0

  # Development-only (like devtools)
  development:
    - org.springframework.boot:spring-boot-devtools:3.2.0

  # Optional (consumers don't get this transitively)
  compileOptional:
    - com.example:optional-lib:1.0.0

  # Test-only
  test:
    - org.junit.jupiter:junit-jupiter:5.11.0
```

#### String-Based Shorthand (Maven 4 Style)

```yaml
# Maven 4 shorthand: group:artifact:version@role
# Role is roughly equivalent to scope/sourceSet

dependencies:
  # Flat list with @role suffix
  - org.springframework.boot:spring-boot-starter-web:3.2.0@compile
  - org.springframework.boot:spring-boot-devtools:3.2.0@development
  - com.example:optional-lib:1.0.0@compileOptional
  - org.junit.jupiter:junit-jupiter:5.11.0@test
  - org.springframework.boot:spring-boot-dependencies:3.2.0@import
```

**Design principle:** Role determines both the **dependency category** (transitivity, resolution) and the **source sets** that include it.

| Role              | Included In   | Transitive |
| ----------------- | ------------- | ---------- |
| `compile`         | main, test    | Yes        |
| `test`            | test only     | No         |
| `development`     | runtime, test | No         |
| `compileOptional` | main          | Optional   |
| `import`          | N/A (BOM)     | N/A        |

#### Scope vs SourceSet

Scopes and source sets are **separate concerns** but **conveniently overlap**:

```yaml
# Scope determines resolution behavior
dependencies:
  compile:
    - org.slf4j:slf4j-api:2.0.0 # Goes to main classpath

test:
  sourceSets:
    test:
      # Source set determines which compilation unit gets it
      # But scope already told us this is test-only
```

**Special case:** `import` scope is **purely for BOMs** - no source set, just version management.

Does this actually work better? We'll see. The design goals are:

- Roles are just dependency categories, not magically coupled to source sets
- Source sets are explicit when you need them
- No implicit creation of 5 things like Gradle's feature variants

But the proof is in the implementation. This is marked as experimental.

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
