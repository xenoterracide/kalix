# Kalyx Vision & Philosophy

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
SPDX-FileCopyrightText: 2026 Kalyx Contributors

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## What is Kalyx?

A build system and dependency manager for the Java ecosystem.

## Core Principles

### Does What I Mean (DWIM)

Simple things should be simple. No ceremony for common tasks.

```bash
# Run a tool on one file
klx tool com.puppycrawl.tools:checkstyle:10.12.0 MyFile.java

# Add a dependency
klx add org.slf4j:slf4j-api:2.0.9

# Build the project
klx build
```

No `pom.xml`. No `build.gradle`. No plugins to configure for basic tasks.

### Secure by Default

Security is not opt-in. It's the foundation.

- **Immutable versions** - Once published, never modified
- **Lock files mandatory** - Reproducible, verifiable builds
- **Supply chain verification** - SHA + PGP signatures checked
- **Credential isolation** - Never in environment variables or plaintext files
- **DNS-verified publishing** - Domain ownership proven via TXT records

```yaml
# Credentials in keychain, not gradle.properties
credentials:
  supplier: keychain
```

### CI and Version Control Optimized

Built for how we work in 2026.

- **Ephemeral CI** - No `clean` needed, fresh containers
- **Reproducible builds** - Same commit = same bits
- **Git-native** - Timestamps from commits, versions from tags
- **Layered config** - Share team/company standards via git submodules
- **Zero-config subprojects** - Just create a directory
- **Git hooks that don't suck** - Fast native binary, `klx tool` for one-liners
- **Cache-friendly** - Unlike `npm ci`, we don't spoil caches

```yaml
# .config/kalyx/10-company.yaml - shared via git submodule
project:
  group: com.mycompany

repositories:
  - https://nexus.mycompany.com/
```

#### Git Hooks Done Right

Compare:

```bash
#!/bin/sh
# .git/hooks/pre-commit with Gradle - AWFUL
./gradlew ktlintCheck --daemon  # Slow, wrapper hell, Windows vs Unix
```

```bash
#!/bin/sh
# .git/hooks/pre-commit with Kalyx - SIMPLE
klx tool com.pinterest.ktlint:ktlint-cli:1.2.0 --git-pre-commit-hook
```

**Why it's better:**

- `klx` uses **Project Leyden** for fast startup (AOT-optimized JVM, not native binary)
- **Lazy loading** - only load plugins/classes needed for the current command
- No wrapper scripts (`./gradlew` vs `gradlew.bat` vs `mvnw`)
- `klx tool` downloads and caches automatically
- Works identically on every developer's machine

#### No npm ci Cache Spoiling

`npm ci` deletes `node_modules` and reinstalls everything. This is slow and wasteful.

Kalyx:

- Lock file guarantees reproducibility
- Cache is never invalidated unless dependencies actually change
- Incremental downloads (only changed artifacts)
- No "clean install" anti-pattern needed

## Technology Choices

### Project Leyden + jlink (Not GraalVM Native)

Kalyx targets **Project Leyden** for fast startup, not GraalVM native image:

- **Keep the JVM** - JIT optimization, full tooling, debugging, profiling
- **AOT caching** - Pre-initialized, pre-resolved classes for fast startup
- **No native limitations** - Full reflection, JNI, dynamic classloading work normally

### jlink Custom Runtime

Because Kalyx is **full JPMS**, we can use `jlink` to create minimal runtime images:

```bash
# Custom runtime with only needed modules
jlink --module-path kalyx.jar:$JAVA_HOME/jmods \
      --add-modules io.kalyx.cli,io.kalyx.resolution \
      --output klx-runtime
```

Result:

- Smaller distribution (~50MB vs ~200MB full JDK)
- Faster startup (fewer modules to initialize)
- Still a real JVM with full capabilities

### Leyden Workflow-Specific Optimizations

Leyden AOT caching can be specialized per workflow:

```bash
# AOT-optimized for testing (no checkstyle, no publishing code loaded)
klx test
# Uses: ~/.kalyx/leyden/klx-test.cds

# AOT-optimized for tool execution (no compilation infrastructure)
klx tool com.pinterest.ktlint:ktlint-cli:1.2.0
# Uses: ~/.kalyx/leyden/klx-tool.cds
```

Each command gets its own **AOT cache profile** with only the classes needed for that workflow. No wasted time loading checkstyle code when running tests.

### No Classpath Hell

Gradle and Maven dump all plugins and dependencies onto one giant classpath. Kalyx uses JPMS modules:

```groovy
// Gradle - everything on classpath, conflicts everywhere
buildscript { dependencies { classpath 'plugin-a:1.0' } }  // Uses guava 28
plugins { id 'plugin-b' version '2.0' }                   // Uses guava 30
// Conflict! Which guava wins?
```

```yaml
# Kalyx - isolated modules
plugins:
  - io.kalyx.java:2.0.0 # module io.kalyx.java, uses guava 28 internally
  - io.kalyx.checkstyle:2.0.0 # module io.kalyx.checkstyle, uses guava 30 internally
# No conflict - each plugin has its own module layer
```

**Potential: JPMS-Required Ecosystem**

Kalyx plugins may be required to be JPMS-compatible:

| Requirement                  | Meaning                        |
| ---------------------------- | ------------------------------ |
| All plugins are JPMS modules | `module-info.java` required    |
| Strong encapsulation         | Internal packages truly hidden |
| Explicit dependencies        | No accidental transitive deps  |
| Module layers per plugin     | True isolation between plugins |

This prevents the "plugin pulled in Guava 28 breaking my Guava 30" nightmare that plagues Gradle.

### Lazy Loading

Kalyx only loads what it needs:

```bash
# Only loads the tool resolution plugin
klx tool com.example:tool:1.0.0

# Doesn't load Maven publishing, container plugins, etc.
```

```bash
# Only loads compilation and testing infrastructure
klx test

# Doesn't load publishing, security scanning, etc.
```

Gradle loads the entire build script and all plugins on every invocation. Kalyx defers loading until actually required.

## Configuration Format

Kalyx uses YAML for project configuration (supports anchors for DRY configs):

```yaml
# kalyx.yaml
project:
  name: my-app
  version: 1.0.0

dependencies:
  compile:
    - org.slf4j:slf4j-api:2.0.9
```

## Command Line Interface

The command-line tool is `klx`:

```bash
klx build
klx test
klx run
```

## License

See project root for licensing details.
