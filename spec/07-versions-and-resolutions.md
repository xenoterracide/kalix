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

Kalix maps module names to Maven coordinates, resolving versions via lock file.

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

Kalix uses **hybrid declaration**:

```yaml
# kalix.yaml
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

Kalix supports three syntax styles for compatibility and ergonomics:

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

### 2. Gradle/Ivy Style (Regex-like)

```yaml
dependencies:
  # 8.0.0 or higher, same major
  - org.example:lib:8.+

  # 8.1.0 or higher, same major.minor
  - org.example:lib:8.1.+

  # Latest 8.x.x
  - org.example:lib:8.+
```

Ivy `+` semantics:

- `8.+` = `8.*` in regex terms
- `8.1.+` = `8.1.*` in regex terms
- **Warning**: `8.+` matches `8.99.99` which may include breaking changes

### 3. Semver/Node Style (Preferred Ergonomics)

```yaml
dependencies:
  # Any 8.x.x (compatible with 8.0.0)
  - org.example:lib:8.x

  # Any 8.0.x (patch only)
  - org.example:lib:8.0.x

  # Any 8.x.x or 9.x.x (major version flexibility)
  - org.example:lib:"8.x || 9.x"

  # At least 8.0.0, less than 9.0.0
  - org.example:lib:"^8.0.0"
```

Node/Semver semantics:

- `x` = wildcard (any value)
- `^8.0.0` = `>=8.0.0 <9.0.0` (compatible with)
- `~8.0.0` = `>=8.0.0 <8.1.0` (approximately)
- `||` = OR ranges

### Syntax Precedence

Kalix detects syntax style and normalizes internally:

| Pattern    | Detected As  | Normalized To    |
| ---------- | ------------ | ---------------- |
| `[8.0.0,)` | Maven range  | Internal range   |
| `8.+`      | Ivy pattern  | Regex `8\\..*`   |
| `8.x`      | Semver       | `>=8.0.0 <9.0.0` |
| `^8.0.0`   | Semver caret | `>=8.0.0 <9.0.0` |
| `8.0.0`    | Exact        | Exact `8.0.0`    |

## Resolution Strategies

Different version syntaxes need different resolution strategies:

| Strategy   | Source     | Behavior                                |
| ---------- | ---------- | --------------------------------------- |
| **Maven**  | `[8.0.0,)` | Nearest definition, soft versions       |
| **Ivy**    | `8.+`      | Dynamic revision, cache TTL             |
| **Semver** | `8.x`      | Latest matching, reproducible with lock |

Kalix uses **unified resolution**:

1. Parse version constraint (any syntax)
2. Normalize to internal range representation
3. Query Maven repositories
4. Apply conflict resolution (newest wins, unless forced)
5. Lock exact versions in `kalix.lock`

## Lock File Versions

Regardless of declared syntax, lock file always contains exact versions:

```yaml
# kalix.yaml
dependencies:
  - org.example:lib:8.x # Semver style
```

```json
// kalix.lock
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
# kalix.yaml
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
# kalix.yaml - explicit version
toolchain:
  languageVersion: 21
  download: true # Download if not present
```

```bash
$ klx build
Toolchain '21-temurin' not found locally.
Downloading from https://api.adoptium.net/...
Installed: ~/.kalix/toolchains/21-temurin/
Building...
```

### Per-Subproject Toolchains

```yaml
# legacy-subproject/kalix.yaml
toolchain:
  languageVersion: 17 # This subproject builds with Java 17
```

```yaml
# root kalix.yaml
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

### Kalix: No Snapshots By Default

Kalix **does not resolve snapshots or pre-releases** by default:

```yaml
# kalix.yaml
dependencies:
  - com.example:lib:1.0-SNAPSHOT
```

```bash
$ klx build
Error: Snapshots are not supported in default resolution mode.
  com.example:lib:1.0-SNAPSHOT

To use snapshots, enable explicitly:
  kalix.yaml:
    resolution:
      allowSnapshots: true
      allowPrereleases: true
```

### Why Consider No Snapshot Support At All

Maven snapshots are a failed experiment:

- They encourage "works on my machine" CI failures
- They bypass the lock file contract
- They make bisecting bugs impossible (old SNAPSHOT is gone)

**Potential decision:** Kalix may not support Maven `-SNAPSHOT` versions at all. Use:

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

Kalix distinguishes between **vulnerabilities** (unintentional bugs) and **malware** (intentional backdoors):

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
    reportedBy: security@kalix.dev
    reportedAt: 2026-03-28T14:30:00Z
```

```bash
$ klx build
Error: Dependency com.evil:backdoor:1.0.0 is BLOCKED
  Severity: MALWARE
  Reason: Confirmed supply chain attack - backdoor payload
  Reported: security@kalix.dev

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

Kalix will **cowardly refuse** to download malware - the system defaults to protecting users over preserving immutability when the artifact is confirmed malicious.

### Comparison

| Feature            | Maven            | Gradle           | npm              | Kalix                   |
| ------------------ | ---------------- | ---------------- | ---------------- | ----------------------- |
| Snapshots          | Native           | Supported        | (nightly)        | **Disabled by default** |
| Prereleases        | Manual naming    | Supported        | Native           | **Disabled by default** |
| Immutable versions | ❌ Can re-deploy | ❌ Can re-deploy | ❌ Can unpublish | **Immutable forever**   |
| Delete versions    | ❌               | ❌               | `npm unpublish`  | **Not supported**       |
| Security yank      | ❌               | ❌               | `npm deprecate`  | **Advisory system**     |
