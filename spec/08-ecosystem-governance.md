# Ecosystem Governance (Future)

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft  
> **Note:** Premature consideration for future Kalix repository hosting

## Philosophy

The Java community is increasingly failing to follow established best practices. If Kalix operates its own artifact hosting service, we should make it **more painful to violate conventions** than to follow them.

This is about two things:

1. **Ecosystem health** - Protecting against the chaos of poor naming conventions
2. **Security** - Preventing impersonation, typosquatting, and provenance confusion

### Security Considerations

Reverse-DNS verification is a security control, not just a convention:

| Attack                  | How Reverse-DNS Helps                                                   |
| ----------------------- | ----------------------------------------------------------------------- |
| **Typosquatting**       | `org.apachee.commons` requires `apachee.org` domain (not `apache.org`)  |
| **Namespace squatting** | Can't claim `com.google.guava` without `google.com` control             |
| **Module confusion**    | `spring.core` vs `org.springframework.core` - provenance is clear       |
| **Supply chain**        | Domain verification ties artifact to verifiable organizational identity |

Without this, any attacker can publish:

- `com.fasterxml.jackson.core` (claiming to be Jackson)
- `spring.security` (impersonating Spring Security)
- `org.apache.logging` (faking Log4j)

With DNS verification:

- `com.fasterxml.jackson.core` requires TXT record at `fasterxml.com`
- `org.springframework.boot` requires TXT record at `springframework.org`
- `org.apache.logging` requires TXT record at `apache.org`

### Verification Flow

```
Artifact: org.apache.commons:commons-lang3:3.14.0
  ↓
Extract group: org.apache.commons
  ↓
Map to domain: apache.org
  ↓
DNS query: apache.org TXT kalix-key
  ↓
Verify: Published key matches artifact signature
  ↓
Publish approved ✓
```

If DNS TXT is missing or key mismatch:

- Build/publish fails with clear error
- No manual override (this is security-critical)

## Reverse-DNS Enforcement

### Maven Central Precedent

Maven Central requires:

- Group ID must match a domain you control
- Proof of domain ownership via DNS TXT record or similar
- `com.example` requires control of `example.com`

### Enhanced DNS Verification

Kalix requires DNS TXT record verification:

```dns
# For group org.apache.commons
apache.org. IN TXT "kalix-key=0x1234567890ABCDEF"
```

This TXT record contains the **public signing key fingerprint** used to sign artifacts. This binds:

1. **Domain ownership** (control of DNS)
2. **Artifact identity** (group ID matches domain)
3. **Cryptographic provenance** (signing key published in DNS)

An attacker would need to:

1. Control the DNS for `apachee.org` (not `apache.org`)
2. Publish their key in that DNS
3. Sign artifacts with their key

Even if they typosquat the group ID (`org.apachee.commons`), their key won't match the real Apache project's published key.

### JPMS Module Name Enforcement

Kalix would extend this to JPMS module names:

| Module Name                | Status           | Notes                                    |
| -------------------------- | ---------------- | ---------------------------------------- |
| `org.springframework.core` | ✅ Auto-approved | Matches reverse-DNS of controlled domain |
| `io.micronaut.http`        | ✅ Auto-approved | Matches reverse-DNS of controlled domain |
| `spring.core`              | ⚠️ Manual review | Violates reverse-DNS convention          |
| `kotlin.stdlib`            | ⚠️ Manual review | Violates reverse-DNS convention          |
| `guava`                    | ❌ Rejected      | Flat namespace, no provenance            |

### Enforcement Tiers

| Tier       | Module Pattern                         | Action                                   |
| ---------- | -------------------------------------- | ---------------------------------------- |
| **Green**  | Reverse-DNS matching verified domain   | Automatic approval, expedited publishing |
| **Yellow** | Reverse-DNS but no domain verification | Standard verification process            |
| **Orange** | Flat name (e.g., `spring.core`)        | Manual review required, delay imposed    |
| **Red**    | Conflicts with reserved namespace      | Rejected                                 |

## The "Spring.Core" Problem

Consider Spring Framework:

```java
// Current (violates convention)
module spring.core {
    exports org.springframework.core;
}

// What it should be
module org.springframework.core {
    exports org.springframework.core;
}
```

If Spring wanted to publish to Kalix hosting:

1. `org.springframework.core` → Immediate approval (they own springframework.org)
2. `spring.core` → Triggers manual review ticket:
   - Requires explanation of why convention is being violated
   - Requires commitment to migration plan
   - May be approved with warnings/deprecation timeline

## Local vs Published

| Context                 | Enforcement                                          |
| ----------------------- | ---------------------------------------------------- |
| Local builds            | No restrictions. Use `spring.core` if you want.      |
| Internal corporate repo | Configurable policy (can enforce or ignore)          |
| Kalix public repository | Strict enforcement with manual review for violations |

## The Migration Path

Existing projects with flat module names:

1. **Grace period**: Violations allowed with manual review
2. **Deprecation warnings**: Published artifacts get warnings in build output
3. **Hard cutover**: New versions must use proper module names
4. **Legacy support**: Old versions remain available indefinitely

## Rationale

### Why Be Hostile to Bad Practices?

The Java module system introduced the concept of "strong encapsulation" and "reliable configuration." Flat module names undermine this:

1. **Collisions**: Two libraries claiming `util` or `core`
2. **No provenance**: Who owns `kotlin.stdlib`? JetBrains? The community?
3. **Tooling confusion**: IDEs can't reliably map modules to sources

Maven Central's group ID verification solved this for coordinates. JPMS needs the same for module names.

### Comparison Table

| Aspect                   | Maven Central | Kalix (Proposed)      |
| ------------------------ | ------------- | --------------------- |
| Group ID verification    | DNS-based     | DNS-based (same)      |
| Module name verification | None          | DNS-based reverse-DNS |
| Flat names (`guava`)     | Allowed       | Manual review         |
| Reserved namespaces      | None          | Enforcement planned   |

## Configuration

Local projects can suppress warnings:

```yaml
# kalix.yaml
governance:
  moduleNameWarnings: false # Silence reverse-DNS warnings locally
```

This does not affect publishing to Kalix repositories—only local builds.

## Future Supply Chain Hardening (Backlog)

### GitHub Actions & Build Environment Attestation

Recent exploits (e.g., similar vectors to SHAi Hulud) demonstrate that artifact signing alone is insufficient. The entire build pipeline must be verified.

Consider:

- **SLSA provenance** - Attestations of how artifacts were built
- **Build environment verification** - Is this artifact from the expected CI/CD pipeline?
- **Reproducible builds** - Same inputs always produce same outputs, verifiable by third parties
- **Transparency logs** - Append-only log of all published artifacts for detection of unauthorized releases

Potential approach:

```yaml
# kalix.yaml
provenance:
  slsa: true # Generate SLSA attestations
  reproducible: true # Fail build if not reproducible
  transparency:
    log: https://kalix-transparency.dev
    include: all # All releases logged publicly
```

### The Complete Chain

| Layer               | Security Control               | Status   |
| ------------------- | ------------------------------ | -------- |
| Domain verification | DNS TXT with signing key       | Proposed |
| Artifact signing    | PGP/Minisign on every artifact | Proposed |
| Build attestation   | SLSA provenance                | Backlog  |
| Transparency        | Public append-only log         | Backlog  |
| Reproducibility     | Bit-for-bit identical builds   | Backlog  |

This would make Kalix the most secure artifact repository system in existence.

## Open Questions

1. Should Kalix even operate a public repository, or focus on tooling?
2. How do we handle legitimate legacy projects that predate JPMS?
3. What about projects that don't own a domain (personal projects)?
4. Should there be a "Kalix Core" namespace (`io.kalix.*`) for common utilities?
5. How do we prevent namespace squatting before domain verification?
6. How do we handle DNS compromise scenarios (domain hijacking)?

## Related

- [07-versions-and-resolutions.md](07-versions-and-resolutions.md) - Toolchain and dependency handling
- [05-subprojects.md](05-subprojects.md) - Module vs subproject terminology
