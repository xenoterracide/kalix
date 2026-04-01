# Versions and Resolution Strategies

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## Repository Support

| Repository Type        | Priority     | Notes                            |
| ---------------------- | ------------ | -------------------------------- |
| Maven                  | **Required** | No alternative in Java ecosystem |
| Gradle Module Metadata | Nice to have | Enhanced dependency information  |

Maven repository support is non-negotiable. Gradle metadata can supplement but is optional.

## JPMS as Source of Truth (Ideal vs Reality)

### The Dream

Use `module-info.java` as the canonical dependency declaration:

```java
module com.example.app {
  requires spring.core;
  requires org.slf4j;
}
```

Kalyx maps module names to Maven coordinates, resolving versions via lock file.

### The Reality

JPMS has **no first-class version support**:

```java
module spring.core {
// No version declared here!
  requires transitive spring.jcl;
  exports org.springframework.core;
}
```

Even if a module declares a version via `ModuleDescriptor.Version`, it's rarely used in practice.

### The Compromise

Kalyx uses **hybrid declaration**:

```yaml
# kalyx.yaml
dependencies:
  # Maven coordinates with JPMS module name (optional)
  - org.springframework:spring-core:6.1.0:
      module: spring.core # For verification

  # Or: just coordinates, module name discovered from JAR
  - org.slf4j:slf4j-api:2.0.9
```

Benefits:

- Coordinates are required (versions must come from somewhere)
- Module name can be used for verification (`module-info.class` matches declared)
- Future: if JPMS adds versions, we can migrate

## Version Syntax Support

Kalyx supports three syntax styles for compatibility and ergonomics:

### 1. Maven Style (Archaic but Required)

```yaml
dependencies:
  # Range: 8.0.0 to infinity
  - org.example:lib:"[8.0.0,)"

  # Range: 8.0.0 inclusive to 9.0.0 exclusive
  - org.example:lib:"[8.0.0,9.0.0)"

  # Exact version
  - org.example:lib:8.0.0
```

Maven semantics:

- `[` = inclusive, `(` = exclusive
- `)` = exclusive, `]` = inclusive
- `,` separates lower and upper bounds
- Empty = unbounded

### 2. Semver/Node Style (Preferred Ergonomics)

```yaml
dependencies:
  # Any 8.x.x (matches 8.0.0 through 8.99.99)
  - org.example:lib:8.x

  # Any 8.0.x (patch only: 8.0.0 through 8.0.99)
  - org.example:lib:8.0.x

  # Any 8.x.x or 9.x.x (major version flexibility)
  - org.example:lib:"8.x || 9.x"

  # At least 8.0.0, less than 9.0.0
  - org.example:lib:"^8.0.0"
```

**Important:** Bare numbers are **literal versions only**:

```yaml
# 8 = EXACTLY version "8" (the literal string)
# Does NOT match 8.0.0, does NOT match 8.5.2
# Only matches an artifact published with version = "8"
- org.example:lib:8
```

This is intentionally strict:

- `8` ≠ `8.0.0` (different strings)
- `8` ≠ `8.0` (different strings)
- Only `8` matches `8`

Use wildcards for flexibility:

- `8.x` matches `8.0.0`, `8.5.2`, `8.99.99`
- `8.0.x` matches `8.0.0`, `8.0.5`, `8.0.99`

This violates "Do What I Mean" but provides **absolute clarity**: you get exactly what you asked for, no surprises.

### Syntax Precedence

Kalyx detects syntax style and normalizes internally:

| Pattern    | Detected As     | Normalized To    |
| ---------- | --------------- | ---------------- |
| `[8.0.0,)` | Maven range     | Internal range   |
| `8.x`      | Semver wildcard | `>=8.0.0 <9.0.0` |
| `8.+`      | Ivy wildcard    | `>=8.0.0 <9.0.0` |
| `^8.0.0`   | Semver caret    | `>=8.0.0 <9.0.0` |
| `~8.0.0`   | Semver tilde    | `>=8.0.0 <8.1.0` |
| `8.0.0`    | Exact version   | Exact `8.0.0`    |
| `8`        | Literal exact   | Exact string `8` |

**Ivy Compatibility:** `8.+` is accepted as a synonym for `8.x`. `8+` (without dot) is **unsupported** (likely a typo).

## Resolution Strategies

Different version syntaxes need different resolution strategies:

| Strategy   | Source     | Behavior                                |
| ---------- | ---------- | --------------------------------------- |
| **Maven**  | `[8.0.0,)` | Nearest definition, soft versions       |
| **Semver** | `8.x`      | Latest matching, reproducible with lock |

Kalyx uses **unified resolution**:

1. Parse version constraint (any syntax)
2. Normalize to internal range representation
3. Query Maven repositories
4. Apply conflict resolution (newest wins, unless forced)
5. Lock exact versions in `kalyx.lock`

## Lock File Versions

Regardless of declared syntax, lock file always contains exact versions:

```yaml
# kalyx.yaml
dependencies:
  - org.example:lib:8.x # Semver style
```

```json
// kalyx.lock
{
  "resolved": [
    {
      "group": "org.example",
      "artifact": "lib",
      "version": "8.5.2", // Exact resolved version
      "declared": "8.x" // Original constraint
    }
  ]
}
```

## Toolchains: First-Class Consideration

Toolchain support is designed in from the start, not bolted on later.

```yaml
# kalyx.yaml
toolchain:
  languageVersion: 21
  vendor: temurin # or amazon, oracle, microsoft, etc.

  # Different toolchain for compilation vs runtime
  compile:
    languageVersion: 21

  test:
    languageVersion: 21

  runtime:
    languageVersion: 21
```

### Auto-Detection

```bash
$ java -version
openjdk version "21.0.2" 2024-01-16

$ klx build
# Detects: Java 21 from PATH
# Uses: System toolchain (no download)
```

### Download on Demand

```yaml
# kalyx.yaml - explicit version
toolchain:
  languageVersion: 21
  download: true # Download if not present
```

```bash
$ klx build
Toolchain '21-temurin' not found locally.
Downloading from https://api.adoptium.net/...
Installed: ~/.kalyx/toolchains/21-temurin/
Building...
```

### Per-Subproject Toolchains

```yaml
# legacy-subproject/kalyx.yaml
toolchain:
  languageVersion: 17 # This subproject builds with Java 17
```

```yaml
# root kalyx.yaml
toolchain:
  languageVersion: 21 # Default for other subprojects
```

## Configuration Ergonomics

### Concise Form

```yaml
dependencies:
  - org.slf4j:slf4j-api:2.0.x
  - org.springframework.boot:spring-boot-starter:3.2.x
```

### Verbose Form

```yaml
dependencies:
  - group: org.slf4j
    artifact: slf4j-api
    version: 2.0.x
    scope: compile
    optional: false
    exclusions:
      - group: *
        artifact: some-internal-lib
```

### Mapping Declarations

For multiple dependencies with same version:

```yaml
versions:
  spring-boot: 3.2.x

dependencies:
  - org.springframework.boot:spring-boot-starter:$spring-boot
  - org.springframework.boot:spring-boot-starter-web:$spring-boot
```

## Publishing with Ranges (Resolving the "Gradle Latest" Problem)

**The Problem:** Gradle's default resolution picks the newest version that satisfies constraints. This means if your library supports `[8.0.0,10.0.0)` but a new version 9.5.0 breaks things, consumers using Gradle automatically get the broken version.

### Kalyx Solution: Lock for Builds, Publish with Ranges

```yaml
# kalyx.yaml - what you develop/test against
# Locked to exact versions for reproducible builds
dependencies:
  compile:
    - org.springframework:spring-core:8.5.2 # Exact, locked
```

```xml
<!-- Published pom.xml - what you tell consumers you support -->
<dependency>
  <groupId>com.example</groupId>
  <artifactId>my-lib</artifactId>
  <version>1.0.0</version>
  <!-- We tested against 8.x, publish that range -->
</dependency>
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-core</artifactId>
  <!-- Published as range so consumers know what we support -->
  <version>[8.0.0,9.0.0)</version>
</dependency>
```

This way:

- **Your builds** are reproducible (locked to exact versions)
- **Consumers know** what versions you actually tested against
- **Version conflicts** are explicit (consumer sees `[8.0.0,9.0.0)` vs their constraints)

### Handling Known Bad Versions

Maven's range syntax doesn't support exclusions. Options:

**Option 1: Narrow the range**

```xml
<!-- Skip 8.5.1 due to known bug -->
<version>[8.0.0,8.5.0),[8.5.2,9.0.0)</version>
```

**Option 2: Dependency management override**

```xml
<!-- In consumer's pom.xml -->
<dependencyManagement>
  <dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>8.5.2</version>
    <!-- Override the problematic version -->
  </dependency>
</dependencyManagement>
```

**Option 3: Documentation**
Release notes document: "Known incompatible with 8.5.1 due to CVE-2026-1234"

## Snapshots and Pre-releases

### Gradle's Mistake

Gradle allows:

```groovy
implementation 'com.example:lib:1.0-SNAPSHOT'
implementation 'com.example:lib:1.0-rc1'
```

This causes:

- Non-reproducible builds (SNAPSHOT changes daily)
- Dependency confusion (is 1.0-rc1 before or after 1.0?)
- Cache invalidation hell

### Kalyx: No Snapshots By Default

Kalyx **does not resolve snapshots or pre-releases** by default:

```yaml
# kalyx.yaml
dependencies:
  - com.example:lib:1.0-SNAPSHOT
```

```bash
$ klx build
Error: Snapshots are not supported in default resolution mode.
  com.example:lib:1.0-SNAPSHOT

To use snapshots, enable explicitly:
  kalyx.yaml:
    resolution:
      allowSnapshots: true
      allowPrereleases: true
```

### Why Consider No Snapshot Support At All

Maven snapshots are a failed experiment:

- They encourage "works on my machine" CI failures
- They bypass the lock file contract
- They make bisecting bugs impossible (old SNAPSHOT is gone)

**Potential decision:** Kalyx may not support Maven `-SNAPSHOT` versions at all. Use:

- **Versioned releases:** `1.0.0-alpha1`, `1.0.0-beta2`, `1.0.0-rc1`
- **Local overrides:** `klx install` from source for development
- **Branch-based resolution:** `klx add com.example:lib:branch-main` (resolves to commit SHA)

## Version Immutability

### Versions Are Forever

Once published, a version **cannot be modified or deleted**:

| Action                  | Result                                                    |
| ----------------------- | --------------------------------------------------------- |
| Re-upload same JAR      | Success (idempotent, SHA matches)                         |
| Re-upload different JAR | **Error** - version already exists with different content |
| Delete version          | **Not supported**                                         |
| "Yank"/suppress version | Supported - marked as vulnerable/bad                      |

## Version Testing Strategies (CPAN Testers Style)

Library authors need to test against multiple dependency versions to ensure compatibility. Kalyx provides **version testing strategies** for CI matrix builds.

### Built-in Strategies

| Strategy         | Behavior                               | Use Case                            |
| ---------------- | -------------------------------------- | ----------------------------------- |
| `lock` (default) | Use `kalyx.lock` exactly               | Reproducible builds                 |
| `latest`         | Latest version matching declared range | Verify still works with newest deps |
| `random`         | Random version from acceptable range   | Probabilistic coverage              |
| `snapshot`       | Include SNAPSHOT versions              | Test against bleeding edge          |
| `prerelease`     | Include alpha/beta/RC versions         | Early compatibility testing         |
| `next`           | Next major version (8.x → 9.x)         | Future compatibility                |

### Usage

```bash
# Default: use locked versions
klx test

# Test with latest versions in declared ranges
klx test --version-strategy latest

# Random version selection for probabilistic testing
klx test --version-strategy random

# Include SNAPSHOT builds
klx test --version-strategy snapshot

# Test against next major version
# If you declared 8.x, this tests against 9.x
klx test --version-strategy next

# Combine strategies
klx test --version-strategy "latest,snapshot"
```

### Explicit Version Overrides

Override specific dependencies for targeted testing:

```bash
# Force specific version for one dependency
klx test --version-override org.springframework:spring-core:6.2.0-RC1

# Multiple overrides
klx test \
  --version-override org.springframework:spring-core:6.2.0-RC1 \
  --version-override org.slf4j:slf4j-api:2.1.0-SNAPSHOT
```

### CI Matrix Example

```yaml
# .github/workflows/matrix.yml
strategy:
  matrix:
    version-strategy: [lock, latest, random, next]
    include:
      # Weekly snapshot test
      - version-strategy: snapshot
        cron: "0 0 * * 0"

steps:
  - run: klx test --version-strategy ${{ matrix.version-strategy }}
```

### Reproducibility on Failure

When a non-locked strategy fails, Kalyx generates a **reproduction lock file**:

```bash
$ klx test --version-strategy random
FAILURE: Test failed with random version selection

Generated reproduction lock file:
  kalyx-random-failure-20260328-153022.lock

Reproduce this exact failure:
  klx test --lock-file kalyx-random-failure-20260328-153022.lock
```

The main `kalyx.lock` is **never modified** by version strategies.

### Custom Lock Files

Use `--lock-file` to test against specific lock files:

```bash
# Use specific lock file
klx build --lock-file production.lock

# CI saves failing states
klx test --version-strategy random || \
  mv kalyx-random-failure-*.lock build-reports/

# Reproduce a reported failure
klx test --lock-file kalyx-random-failure-20260328-153022.lock
```

This enables:

- **CI artifacts**: Save failing lock files for debugging
- **Bug reports**: Attach lock file to reproduce exactly
- **Bisecting**: Test against multiple historical lock files
- **Staging**: Test against production lock file before deploy

### Strategy Details

#### `random` Strategy

Picks a random version from the acceptable range:

```yaml
# kalyx.yaml
dependencies:
  - org.example:lib:8.x # Could resolve to 8.0.0, 8.5.2, 8.99.99...
```

Each run gets a different random version within bounds. Run multiple times for coverage.

#### `next` Strategy

Tests compatibility with next major version:

```yaml
# Your declared dependency
dependencies:
  - org.springframework:spring-core:8.x
```

```bash
$ klx test --version-strategy next
# Resolves to latest 9.x.x to verify future compatibility
```

This is particularly useful for library authors who want to ensure their code works with upcoming major versions of their dependencies.

#### `snapshot` Strategy

Allows resolution of Maven SNAPSHOT versions:

```bash
$ klx test --version-strategy snapshot
Resolving SNAPSHOT: org.example:lib:1.0-SNAPSHOT
Downloaded: lib-1.0-20260328.143000-47.jar
```

**Warning:** SNAPSHOTs change between runs. Only use in CI, never for releases.

### Comparison: CPAN Testers

| Feature              | CPAN Testers     | Kalyx                  |
| -------------------- | ---------------- | ---------------------- |
| Matrix testing       | ✓ Yes            | ✓ `--version-strategy` |
| Random versions      | ✓ Yes            | ✓ `random` strategy    |
| Report failures      | ✓ Public reports | ✓ JSON dump            |
| Reproduce failures   | Via reports      | `--version-override`   |
| Snapshot testing     | N/A              | ✓ `snapshot` strategy  |
| Next version testing | N/A              | ✓ `next` strategy      |

### Idempotent Publishing

```bash
# First publish
$ klx publish
Published: com.example:lib:1.0.0

# Re-publish identical content
$ klx publish
Published: com.example:lib:1.0.0 (idempotent - already exists)

# Re-publish different content
$ klx publish
Error: Version 1.0.0 already exists with different SHA-256
  Existing: a1b2c3d4...
  Upload:   w9x8y7z6...

Versions are immutable. Use a new version number.
```

### Security Response: Vulnerabilities vs Malware

Kalyx distinguishes between **vulnerabilities** (unintentional bugs) and **malware** (intentional backdoors):

| Type          | Severity                            | Download Behavior     |
| ------------- | ----------------------------------- | --------------------- |
| Vulnerability | `low`, `medium`, `high`, `critical` | Warning, can override |
| Malware       | `malware`                           | **Blocked entirely**  |

#### Vulnerability Advisory

```yaml
# Published to repository metadata
advisories:
  com.example:lib:1.0.0:
    severity: critical
    reason: "CVE-2026-1234 - RCE vulnerability"
    supersededBy: 1.0.1
```

```bash
$ klx build
Warning: Dependency com.example:lib:1.0.0 has security advisory
  Severity: CRITICAL
  CVE-2026-1234 - RCE vulnerability
  Upgrade to: 1.0.1

Use --ignore-advisories to proceed (not recommended).
```

#### Malware Block

```yaml
advisories:
  com.evil:backdoor:1.0.0:
    severity: malware
    reason: "Confirmed supply chain attack - backdoor payload"
    reportedBy: security@kalyx.dev
    reportedAt: 2026-03-28T14:30:00Z
```

```bash
$ klx build
Error: Dependency com.evil:backdoor:1.0.0 is BLOCKED
  Severity: MALWARE
  Reason: Confirmed supply chain attack - backdoor payload
  Reported: security@kalyx.dev

This artifact has been identified as malware.
Remove it from your dependencies immediately.

Use --ignore-malware ONLY if you understand the risk.
```

### Deletion Policy (Under Consideration)

| Scenario                  | Immutable | Deletable           |
| ------------------------- | --------- | ------------------- |
| Normal release            | ✓ Yes     | ✗ No                |
| Vulnerable release        | ✓ Yes     | ✗ No (use advisory) |
| Malware/confirmed exploit | ? Maybe   | ? Maybe             |
| Legal/copyright violation | ? Maybe   | ? Maybe             |

**Open question:** Should actual malware be:

- A) Blocked but preserved (for forensic analysis)
- B) Deleted entirely (removed from repository)
- C) Both (moved to quarantine, removed from main)

Kalyx will **cowardly refuse** to download malware - the system defaults to protecting users over preserving immutability when the artifact is confirmed malicious.

### Comparison

| Feature            | Maven            | Gradle           | npm              | Kalyx                   |
| ------------------ | ---------------- | ---------------- | ---------------- | ----------------------- |
| Snapshots          | Native           | Supported        | (nightly)        | **Disabled by default** |
| Prereleases        | Manual naming    | Supported        | Native           | **Disabled by default** |
| Immutable versions | ❌ Can re-deploy | ❌ Can re-deploy | ❌ Can unpublish | **Immutable forever**   |
| Delete versions    | ❌               | ❌               | `npm unpublish`  | **Not supported**       |
| Security yank      | ❌               | ❌               | `npm deprecate`  | **Advisory system**     |
