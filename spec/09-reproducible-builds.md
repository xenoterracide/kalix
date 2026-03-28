# Reproducible Builds

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## Core Principle

Kalix builds are **reproducible by default**. Running `klx build` 482 times with the same inputs produces bit-for-bit identical output.

```bash
$ klx build
$ sha256sum build/libs/my-app-1.0.0.jar
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855  build/libs/my-app-1.0.0.jar

$ klx build
$ sha256sum build/libs/my-app-1.0.0.jar
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855  build/libs/my-app-1.0.0.jar

# Identical hash
```

## Inputs That Must Be Identical

| Input              | Source of Variability          | Kalix Solution       |
| ------------------ | ------------------------------ | -------------------- |
| Source code        | Timestamps, line endings       | Normalized input     |
| Dependencies       | SNAPSHOT versions              | Banned by default    |
| Build tool version | Different `klx` versions       | Version in lock file |
| JDK                | Different JDK vendors/versions | Toolchain pinning    |
| Environment        | Build path, username           | Stripped from output |

## What Makes Builds Non-Reproducible

### Timestamps

ZIP/JAR format unfortunately has timestamp fields baked into the spec. They are **not required to be accurate** - we just need them to be **reproducible**.

**Kalix solution:** Plugin-based timestamp resolution.

The core asks plugins: "What's the reproducible timestamp for this build?"

```
# Kalix JAR entry (via git plugin)
Local file header:
  last mod file time: 10:30:00   # From git commit: 2026-03-28 10:30:00 UTC
  last mod file date: 2026-03-28 # From git commit
```

**Plugin interface for VCS/timestamp providers:**

| Plugin           | Source                 | Timestamp              |
| ---------------- | ---------------------- | ---------------------- |
| `git` (built-in) | Git commit author date | `2026-03-28T10:30:00Z` |
| `hg`             | Mercurial commit       | Same pattern           |
| `epoch`          | Fixed constant         | `1980-01-01T00:00:00Z` |
| `fs`             | File modification      | Source file mtime      |

ZIP entry timestamps come from the active VCS plugin. No git in the core - the git plugin provides `ReproducibleTimestampProvider`.

### File Ordering

ZIP/JAR entry order depends on filesystem iteration order.

**Kalix solution:** All archive entries sorted alphabetically.

### Build Path

Absolute paths in debug info, source maps, etc.

**Kalix solution:** All paths relative to project root in output.

### Non-Deterministic Operations

- Hash iteration order (caught us in Gradle)
- Random UUIDs (unless seeded)
- Parallel stream non-determinism

**Kalix solution:**

- Ordered data structures by default
- Deterministic UUID generation when needed
- Documented parallelism constraints

## Reproducibility Verification

```bash
# Verify build is reproducible
$ klx build --verify
Building (pass 1)...
Building (pass 2)...
Comparing...
✓ Builds are reproducible
  SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855

# Or check without rebuilding
$ klx build --verify-existing
Comparing last build against canonical hash...
✓ Build matches expected hash
```

## CI/CD Integration

```yaml
# GitHub Actions
- run: klx build --verify
  # Fails if build is not reproducible
```

Reproducible builds enable:

- **Verification** - Anyone can rebuild and verify artifact matches
- **Caching** - Build outputs cached by content hash
- **Debugging** - Old builds behave identically

## Configuration

```yaml
# kalix.yaml
reproducible:
  enabled: true # Default
  buildInfo: false # Exclude volatile build metadata
```

Non-reproducible features opt-in:

```yaml
reproducible:
  enabled: false # Include non-deterministic metadata
```

Note: There is no option to "record timestamps" - timestamps in JARs are set to epoch (fixed constant), not meaningful dates.

## Comparison

| Tool      | Reproducible By Default | Notes                                |
| --------- | ----------------------- | ------------------------------------ |
| Maven     | ❌ No                   | Requires plugins, timestamps in JARs |
| Gradle    | ❌ No                   | Requires configuration, often breaks |
| Bazel     | ✅ Yes                  | Sandboxed, deterministic             |
| Nix       | ✅ Yes                  | Content-addressed                    |
| **Kalix** | **✅ Yes**              | **Zero config, guaranteed**          |

## Debugging Non-Reproducibility

```bash
$ klx build --verify
✗ Builds differ

Diff:
  META-INF/MANIFEST.MF
    - Build-Time: 2026-03-28T14:30:00Z
    + Build-Time: 2026-03-28T14:30:01Z

Suggestion: Disable build timestamps in kalix.yaml
  reproducible:
    buildInfo: false
```

## Future: Independent Verification

Third parties can verify Kalix artifacts:

```bash
# Anyone can run this
$ klx verify org.example:my-app:1.0.0
Downloading sources...
Downloading dependencies...
Building...
Comparing against published artifact...
✓ Reproduced exactly
  Published: e3b0c44298...
  Built:     e3b0c44298...
```

This enables decentralized trust - don't trust Kalix hosting, verify yourself.
