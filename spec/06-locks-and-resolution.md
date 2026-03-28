# Lock Files and Resolution

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## Lock Files Are Mandatory

Kalix **always** uses a lock file. There is no mode without locking.

```
my-project/
├── kalix.yaml          # Declares dependencies (loose versions allowed)
├── kalix.lock          # Exact resolved versions (JSON, committed)
└── src/
```

## Lock File Immutability

**Lock files are immutable by default.** This is the opposite of tools like Yarn where `yarn install` modifies locks and `--immutable` is opt-in.

| Command              | Behavior                                               |
| -------------------- | ------------------------------------------------------ |
| `klx build`          | Uses `kalix.lock` exactly. Fails if lock is stale.     |
| `klx test`           | Uses `kalix.lock` exactly. Fails if lock is stale.     |
| `klx run`            | Uses `kalix.lock` exactly. Fails if lock is stale.     |
| `klx update`         | Updates `kalix.yaml` deps and regenerates `kalix.lock` |
| `klx update --check` | Exits non-zero if `kalix.lock` is out of date          |

### Stale Lock Detection

```bash
$ cat kalix.yaml
dependencies:
  - org.slf4j:slf4j-api:2.0.9

$ cat kalix.lock
{
  "dependencies": [
    {"group": "org.slf4j", "artifact": "slf4j-api", "version": "2.0.7"}
  ]
}

$ klx build
Error: kalix.lock is stale. Declared: slf4j-api:2.0.9, Locked: 2.0.7
Run: klx update
```

## Zero Command

**There is no `klx install`.** Dependencies are downloaded automatically as needed.

```bash
# Fresh clone, no dependencies downloaded
$ klx build
Resolving from kalix.lock...
Downloading org.slf4j:slf4j-api:2.0.9...
Downloading ch.qos.logback:logback-classic:1.4.14...
Compiling...
```

The `klx` CLI:

1. Reads `kalix.lock`
2. Checks local cache
3. Downloads missing artifacts automatically
4. Builds

### Cache Location

```
~/.kalix/cache/
├── v1/
│   ├── https/repo1.maven.org/maven2/
│   │   └── org/slf4j/slf4j-api/2.0.9/
│   │       ├── slf4j-api-2.0.9.jar
│   │       ├── slf4j-api-2.0.9.pom
│   │       └── slf4j-api-2.0.9.jar.sha256
```

## Lock File Format

JSON for simplicity and tooling. Includes cryptographic verification data:

```json
{
  "version": 1,
  "generatedAt": "2026-03-28T14:30:00Z",
  "dependencies": [
    {
      "group": "org.slf4j",
      "artifact": "slf4j-api",
      "version": "2.0.9",
      "sha256": "c4f...",
      "scope": "compile"
    }
  ],
  "resolved": [
    {
      "group": "org.slf4j",
      "artifact": "slf4j-api",
      "version": "2.0.9",
      "sha256": "a1b2c3d4...",
      "sha512": "e5f6g7h8...",
      "pomSha256": "i9j0k1l2...",
      "signature": {
        "keyId": "0x1234567890ABCDEF",
        "algorithm": "SHA256WITHRSA",
        "signature": "base64signature..."
      },
      "dependencies": []
    }
  ]
}
```

## Supply Chain Security

Kalix validates artifacts at download time to prevent supply chain attacks:

### Verification Steps

When downloading an artifact:

1. **Download** JAR, POM, and `.asc` signature file from repository
2. **SHA verification** - Verify JAR/POM SHA-256/SHA-512 matches lock file
3. **Signature verification** - Verify PGP signature against known key
4. **Key validation** - Verify signing key ID matches lock file
5. **Save to cache** - Only after all checks pass

```
~/.kalix/cache/
├── v1/
│   └── https/repo1.maven.org/maven2/
│       └── org/slf4j/slf4j-api/2.0.9/
│           ├── slf4j-api-2.0.9.jar
│           ├── slf4j-api-2.0.9.jar.sha256        # Verified
│           ├── slf4j-api-2.0.9.pom
│           ├── slf4j-api-2.0.9.pom.sha256        # Verified
│           └── slf4j-api-2.0.9.jar.asc           # Signature verified
```

### Unsigned Artifacts

If an artifact has no signature in the repository:

| Mode                | Behavior                                      |
| ------------------- | --------------------------------------------- |
| Default             | Warning logged, SHA-256 still required        |
| Strict (`--strict`) | Build fails if any artifact is unsigned       |
| Trust-on-first-use  | Require signature for updates, allow existing |

```yaml
# kalix.yaml
security:
  requireSignatures: true # Fail build on unsigned artifacts
  allowedKeys: # Restrict to specific signing keys
    - "0x1234567890ABCDEF" # Project maintainer
    - "0xFEDCBA0987654321" # Organization key
```

### Key Management

Kalix maintains a keyring of trusted signing keys:

```
~/.kalix/keys/
├── trusted/
│   ├── 0x1234567890ABCDEF.asc   # Maven Central signing key
│   └── 0xFEDCBA0987654321.asc   # Organization key
└── revoked/                     # Known compromised keys
```

Keys are auto-imported from:

- Maven Central's KEYS file
- SKS keyservers (if available)
- User manual import: `klx key import maintainer@example.com`

### Verification Failure

If verification fails, the build stops with a clear error:

```bash
$ klx build
Error: Supply chain verification failed for org.slf4j:slf4j-api:2.0.9
  SHA-256 mismatch: expected a1b2..., got x9y8...
  Possible causes:
    - Artifact was modified after lock file creation
    - Repository compromise
    - Network corruption (retry may help)

Run 'klx verify --repair' to re-resolve and update lock file.
```

### Lock File Updates

When `klx update` generates a new lock file, it captures:

- Current artifact SHA hashes
- Current signing key IDs
- Signatures for validation

Users review changes in version control:

```diff
+      "sha256": "newhash...",
+      "signature": {
+        "keyId": "0x1234567890ABCDEF",
```

````

## Updating Dependencies

```bash
# Update all to latest matching constraints
$ klx update

# Update specific dependency
$ klx update org.slf4j:slf4j-api

# Check if update available without modifying
$ klx update --dry-run

# Update and show what changed
$ klx update --interactive
````

## Comparison

| Tool      | Default Lock Behavior                 | Install Command         |
| --------- | ------------------------------------- | ----------------------- |
| npm       | Modifies                              | `npm install`           |
| yarn 1.x  | Modifies                              | `yarn install`          |
| yarn 2.x+ | Modifies (`--immutable` opt-in)       | `yarn install`          |
| pnpm      | Modifies (`--frozen-lockfile` opt-in) | `pnpm install`          |
| Maven     | No lock file                          | N/A                     |
| Gradle    | No lock file                          | N/A                     |
| **Kalix** | **Immutable by default**              | **None (zero command)** |

## CI/CD

```yaml
# GitHub Actions
- run: klx build
  # Fails if kalix.lock is stale or missing
  # Downloads deps automatically
```

No separate install step. No `--immutable` flag to remember.

## Version Constraints

In `kalix.yaml` (declarative, loose):

```yaml
dependencies:
  # Exact version
  - org.slf4j:slf4j-api:2.0.9

  # Range (resolved to specific version in lock)
  - org.slf4j:slf4j-api:2.0.x
  - org.slf4j:slf4j-api:">=2.0.0 <3.0.0"
```

In `kalix.lock` (resolved, exact):

```json
{ "version": "2.0.9" }
```
