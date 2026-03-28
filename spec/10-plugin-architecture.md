# Plugin Architecture

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## Philosophy: Minimal Core

Kalix has a **minimal core**. Almost everything is a plugin, including first-party features.

This is the lesson from Gradle: when you bake features into the core, you create a monolith that is hard to evolve, hard to replace, and creates unintended coupling.

## What Belongs in Core?

| Category              | Core?  | Examples                                    |
| --------------------- | ------ | ------------------------------------------- |
| Task execution engine | ✅ Yes | Dependency graph, parallel execution        |
| Configuration parsing | ✅ Yes | YAML loading, schema validation             |
| Dependency resolution | ✅ Yes | Maven repository protocol, lock file logic  |
| Artifact caching      | ✅ Yes | Local cache, checksum verification          |
| Build lifecycle       | ✅ Yes | Phases: compile, test, package              |
| **Compilation**       | ❌ No  | Plugin (`java`, `kotlin`)                   |
| **Testing**           | ❌ No  | Plugin (`junit`, `testng`)                  |
| **Packaging**         | ❌ No  | Plugin (`jar`, `war`, `container`)          |
| **Publishing**        | ❌ No  | Plugin (`maven-publish`, `github-packages`) |
| **Version control**   | ❌ No  | Plugin (`git`, `hg`)                        |
| **Security scanning** | ❌ No  | Plugin (`owasp`, `trivy`)                   |
| **Code generation**   | ❌ No  | Plugin (`protobuf`, `jooq`)                 |

## First-Party Plugins

"First-party" means maintained by the Kalix project, not that they're in the core.

```yaml
# kalix.yaml - first-party plugins
plugins:
  - io.kalix.java:2.0.0 # Compilation
  - io.kalix.junit:2.0.0 # Testing
  - io.kalix.jar:2.0.0 # Packaging
  - io.kalix.maven-publish:2.0.0 # Publishing to Maven Central
  - io.kalix.git:2.0.0 # VCS integration
```

These plugins live in separate repositories, have their own release cycles, and could theoretically be replaced by third-party alternatives.

## Why Publication Must Be a Plugin

Gradle proves the failure of the "core includes everything" approach. Gradle has a built-in `maven-publish` plugin, yet **it cannot publish to Maven Central**. The built-in plugin lacks support for:

- Staging repositories
- Close-and-release workflow
- Automatic promotion

So everyone uses third-party plugins like:

- `io.github.gradle-nexus.publish-plugin`
- `com.vanniktech.maven.publish`

This proves that **core bundling doesn't work**. Even with all of Gradle's bloat, they couldn't keep up with Maven Central's requirements.

### Kalix Lesson

Don't pretend we can anticipate every repository's requirements:

| Repository       | Special Requirements        |
| ---------------- | --------------------------- |
| Maven Central    | Staging, signing, promotion |
| GitHub Packages  | Different auth, no staging  |
| AWS CodeArtifact | IAM auth, temporary tokens  |
| Private Nexus    | Custom auth schemes         |

Each needs its own plugin. The core knows nothing about publishing protocols.

Kalix approach:

```yaml
# Publishing is just a plugin
plugins:
  - io.kalix.maven-publish:2.0.0

publishing:
  repositories:
    - name: mavenCentral
      url: https://s01.oss.sonatype.org/
      # Plugin handles authentication, staging, promotion
```

The core knows nothing about:

- Maven staging repositories
- PGP signing for publication
- `maven-metadata.xml` generation
- Repository authentication schemes

The plugin handles all of it. If Maven Central changes their API, update the plugin - not Kalix core.

## Plugin Interface

Plugins implement well-defined interfaces:

```java
// Core defines the contract
public interface CompilerPlugin {
  Task compile(CompileRequest request);
  List<String> getSupportedLanguages();
}

public interface PublisherPlugin {
  Task publish(PublishRequest request);
  List<String> getSupportedProtocols();
}

public interface VcsPlugin {
  String getReproducibleTimestamp();
  String getVersionDescription();
  boolean isWorkspaceDirty();
}
```

## The IntelliJ Problem: Authentication

IntelliJ has no unified authentication system. Every plugin implements its own:

- OAuth flows with different redirect handlers
- Basic auth dialogs with different UIs
- Token storage in different locations
- No shared credential cache

**Result:** Users enter passwords 47 times, credentials scattered insecurely, no single sign-on.

### Kalix: Centralized Credential SPI

Kalix provides a **credential supplier SPI** that plugins use, not implement:

```java
// Core SPI - plugins call this, don't implement it
public interface CredentialSupplier {
  Optional<Credential> get(String service);
  void store(String service, Credential credential);
}

// Plugins request credentials, don't handle them directly
public class MavenPublisher implements PublisherPlugin {

  public Task publish(PublishRequest request) {
    Credential cred = credentialSupplier.get("maven.central");
    // Use cred, never see the password
  }
}
```

### Credential Storage Plugins

| Plugin                | Storage             | Security                      |
| --------------------- | ------------------- | ----------------------------- |
| `keychain` (built-in) | OS keychain         | Encrypted, biometric unlock   |
| `pass`                | GPG-encrypted files | User-controlled               |
| `vault`               | HashiCorp Vault     | Enterprise secrets management |
| `aws-secrets`         | AWS Secrets Manager | IAM-integrated                |
| `azure-keyvault`      | Azure Key Vault     | Entra ID integrated           |
| `gcp-secretmanager`   | GCP Secret Manager  | Cloud IAM                     |

Configuration:

```yaml
# kalix.yaml
credentials:
  supplier: keychain # or vault, pass, etc.

  # Plugin-specific config
  vault:
    address: https://vault.company.com
    path: secret/kalix
```

### Security Requirements

Kalix supports multiple credential storage strategies, from simple to secure:

| Level  | Supplier    | Use Case                             |
| ------ | ----------- | ------------------------------------ |
| Basic  | `plaintext` | Local dev, CI with ephemeral secrets |
| Better | `askpass`   | Interactive password entry           |
| Good   | `keychain`  | OS-integrated secure storage         |
| Best   | `vault`     | Enterprise secrets management        |

**Core principles:**

1. **Pluggable** - Users choose their security level
2. **Default secure** - `askpass` or `keychain` out of the box
3. **No env vars for secrets** - Environment variables leak to subprocesses
4. **No logging** - Core redacts credentials from all output
5. **Audit trail** - When credentials are accessed (not the values)

### Publishing with Credentials

**Option 1: Plaintext (simplest, CI-friendly)**

```yaml
# kalix.yaml
credentials:
  supplier: plaintext

publishing:
  repositories:
    - name: mavenCentral
      url: https://s01.oss.sonatype.org/
      credentials:
        username: ${MAVEN_USER} # 12-factor: non-secret from env
        password: ${MAVEN_PASSWORD} # Secret via env (CI only)
```

**Option 2: askpass (interactive, default for dev)**

```yaml
# kalix.yaml
credentials:
  supplier: askpass # Prompts interactively

publishing:
  repositories:
    - name: mavenCentral
      credentials: maven.central # Prompts: "Password for maven.central:"
```

**Option 3: Keychain (secure, default recommended)**

```yaml
# kalix.yaml
credentials:
  supplier: keychain

publishing:
  repositories:
    - name: mavenCentral
      credentials: maven.central # Looks up in OS keychain
```

```bash
# One-time setup
klx credential add maven.central --username myuser
Password: ********
# Stored in OS keychain, never in files
```

## 12-Factor Configuration

Kalix follows 12-factor app principles for configuration:

### 1. Store Config in Environment

```bash
# Configuration via environment (12-factor)
export KALIX_VAULT_ADDR=https://vault.company.com
export KALIX_LOG_LEVEL=debug

# NOT credentials
# export KALIX_MAVEN_PASSWORD=secret123  # WRONG - use credential supplier
```

### 2. Backing Services as Attached Resources

```yaml
# kalix.yaml - service URLs only, no credentials
repositories:
  - name: company-nexus
    url: ${NEXUS_URL} # 12-factor: URL from env, auth from keychain
```

### 3. Strict Separation of Config from Code

| What            | Where           | Example                            |
| --------------- | --------------- | ---------------------------------- |
| Repository URLs | Env vars / YAML | `https://nexus.company.com`        |
| Build settings  | YAML            | `sourceCompatibility: 21`          |
| Credentials     | Keychain/Vault  | (never in files)                   |
| Feature flags   | Env vars        | `KALIX_EXPERIMENTAL_PARALLEL=true` |

### The Gradle/Maven Problem

Gradle and Maven made secure credential management **impossible by design**:

| Tool   | Credential Storage            | Problem                                    |
| ------ | ----------------------------- | ------------------------------------------ |
| Maven  | `~/.m2/settings.xml`          | Plaintext, single file                     |
| Gradle | `~/.gradle/gradle.properties` | Plaintext, env vars encouraged             |
| Both   | Environment variables         | Leak to subprocess, visible in `/proc`     |
| Both   | No plugin SPI                 | Can't integrate with Vault, keychain, etc. |

**Result:** Enterprise secrets in plaintext files, committed to git, exposed in CI logs.

### Kalix Approach: Pluggable, Progressive Security

Kalix makes security **possible** and **optional but encouraged**:

```yaml
# Simple (CI, ephemeral)
credentials:
  supplier: plaintext

# Better (development)
credentials:
  supplier: askpass  # Interactive prompt

# Best (production, enterprise)
credentials:
  supplier: vault
```

**Key difference:** Gradle/Maven lock you into plaintext. Kalix gives you choices.

````

## Plugin Discovery

Plugins resolved like dependencies:

```yaml
plugins:
  # First-party
  - io.kalix.java:2.0.0

  # Third-party
  - com.example.kalix:protobuf-plugin:1.5.0

  # Local (development)
  - file:../my-plugin/build/libs/my-plugin.jar
````

Plugins downloaded from Maven repositories, cached locally, loaded in isolated classloaders.

## Core Extension Points

The core exposes hooks that plugins register for:

| Hook              | When Called        | Example Use                |
| ----------------- | ------------------ | -------------------------- |
| `configure`       | After YAML parsed  | Read plugin config section |
| `beforeCompile`   | Before compilation | Generate code              |
| `afterCompile`    | After compilation  | Bytecode transformation    |
| `beforeTest`      | Before tests       | Start test database        |
| `afterPackage`    | After JAR created  | Sign artifact              |
| `provideMetadata` | For IDE export     | Generate source maps       |

## No Special Treatment for First-Party

First-party plugins:

- Use the same API as third-party
- Are tested the same way
- Can be forked/replaced by users
- Have independent versioning

The only difference is maintenance ownership.

## Comparison

| Aspect               | Maven                       | Gradle                     | Kalix                  |
| -------------------- | --------------------------- | -------------------------- | ---------------------- |
| Core size            | Large (everything built-in) | Large (publishing in core) | **Minimal**            |
| Plugin API           | Limited (Mojos)             | Powerful but coupled       | **Clean interfaces**   |
| First-party special? | Yes (built-in)              | Yes (core plugins)         | **No (external)**      |
| Replace compiler?    | Hard                        | Hard                       | **Easy (swap plugin)** |
| Update publishing?   | Maven release               | Wait for Gradle            | **Update plugin**      |

## Configuration Example

```yaml
# kalix.yaml - everything is plugins

plugins:
  # Language
  - io.kalix.java:2.0.0

  # Build lifecycle extensions
  - io.kalix.git:2.0.0 # VCS info
  - io.kalix.checkstyle:2.0.0 # Code quality

  # Packaging
  - io.kalix.jar:2.0.0
  - io.kalix.container:2.0.0 # Docker/OCI

  # Publishing
  - io.kalix.maven-publish:2.0.0

# Plugin-specific configuration
java:
  sourceCompatibility: 21

container:
  base: eclipse-temurin:21-jre

publishing:
  repositories:
    - mavenCentral
```

The core only knows about `plugins:` - everything else is defined by the plugins themselves.
