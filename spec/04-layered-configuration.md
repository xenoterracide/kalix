# Layered Configuration

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## Philosophy

Copy-paste configuration from other projects must be trivial. GAV (group/artifact/version) and project metadata should not live in the main build file. Instead, Kalyx uses a layered configuration directory with merge/override semantics.

## Configuration Directory Structure

```
project-root/
├── .config/
│   └── kalyx/
│       ├── 00-defaults.yaml          # Distribution defaults
│       ├── 01-company-defaults.yaml  # Company-wide standards
│       ├── 02-myorg-defaults.yaml    # Org/team specific
│       ├── 03-project.yaml           # Project specific
│       └── 99-local.yaml             # User local overrides (gitignored)
└── src/
```

## File Naming Convention

| Pattern                | Purpose                                           |
| ---------------------- | ------------------------------------------------- |
| `*.yaml`               | Any `.yaml` file is loaded                        |
| Numeric prefix (`NN-`) | Determines merge order (optional but recommended) |
| No prefix              | Loaded in filesystem order after prefixed files   |

## Merge Semantics

Files are loaded in sorted order, with **deep merge** for objects and **append** for arrays:

```yaml
# 00-defaults.yaml
project:
  sourceCompatibility: "21"

repositories:
  - https://repo1.maven.org/maven2
```

```yaml
# 01-company-defaults.yaml
project:
  group: com.mycompany # Merged into project object

repositories:
  - https://nexus.mycompany.com/repository/maven-public # Appended
```

```yaml
# 03-project.yaml
project:
  name: my-service # Merged
  version: "${git.describe}" # Merged, can use placeholders

dependencies:
  compile:
    - org.slf4j:slf4j-api:2.0.9
```

**Result after merge:**

```yaml
project:
  sourceCompatibility: "21"
  group: com.mycompany
  name: my-service
  version: "${git.describe}"

repositories:
  - https://repo1.maven.org/maven2
  - https://nexus.mycompany.com/repository/maven-public

dependencies:
  compile:
    - org.slf4j:slf4j-api:2.0.9
```

## Common Patterns

### Enterprise Template (Copy-Paste Ready)

```bash
# New project setup
cd my-new-service
mkdir -p .config/kalyx

# Copy from company template repo
cp ~/templates/kalyx/01-company-defaults.yaml .config/kalyx/
cp ~/templates/kalyx/02-java-service.yaml .config/kalyx/

# Add project-specific stuff
cat > .config/kalyx/03-project.yaml << 'EOF'
project:
  name: my-new-service
dependencies:
  compile:
    - org.springframework.boot:spring-boot-starter-web:3.2.0
EOF
```

### Git Subtree/Submodule for Shared Config

```
.config/kalyx/
├── 00-defaults.yaml           # From Kalyx distribution
├── 10-company/                # Git subtree from company/kalyx-configs
│   ├── default-repos.yaml
│   ├── security-scanning.yaml
│   └── publishing.yaml
├── 20-team/                   # Git subtree from team/kalyx-configs
│   └── microservice-defaults.yaml
└── 99-project.yaml            # Project specific
```

### Layer Purposes

| Layer | Typical Content                                             | Source                |
| ----- | ----------------------------------------------------------- | --------------------- |
| 00-09 | Kalyx defaults, Java version defaults                       | Bundled with `klx`    |
| 10-19 | Company-wide: repositories, group prefix, security policies | Company template repo |
| 20-49 | Team/org specific: framework defaults, testing config       | Team template repo    |
| 50-89 | Project specific: name, version, dependencies               | Project repo          |
| 90-99 | Local overrides (gitignored): credentials, local paths      | Developer machine     |

## Where Things Live

| Element                     | Belongs In                         | Example                                         |
| --------------------------- | ---------------------------------- | ----------------------------------------------- |
| **Coordinates (GAV)**       | Layered config (org/project level) | `01-company-defaults.yaml` sets `project.group` |
| **Repositories**            | Layered config (org level)         | `01-company-defaults.yaml` adds Nexus           |
| **Dependencies**            | Project config                     | `03-project.yaml`                               |
| **Plugin versions**         | Layered config (org level)         | `02-team-defaults.yaml` pins plugin versions    |
| **Build logic**             | Plugins                            | Not in config files                             |
| **Local paths/credentials** | 99-local.yaml (gitignored)         | Local Maven repo override                       |

## No Root Build File?

With full layering, `kalyx.yaml` at root is optional:

```
my-service/
├── .config/kalyx/
│   ├── 01-company.yaml    # group: com.mycompany
│   ├── 02-service.yaml    # plugin: io.kalyx.spring-boot
│   └── 03-project.yaml    # name: my-service
└── src/main/java/...
```

```bash
$ klx build
# Works! Merges all configs, auto-detects main class, builds jar
```

The root `kalyx.yaml` can still exist for single-file convenience in simple projects.

## Copy-Paste Ergonomics

**Scenario: Start a new microservice**

```bash
# From company template repo
cp -r templates/microservice/.config my-new-service/
cd my-new-service

# Edit one file
echo 'project: { name: my-new-service }' > .config/kalyx/99-project.yaml

klx build
```

**Scenario: Update company-wide repository**

```bash
# In each project
git subtree pull --prefix=.config/kalyx/10-company company-kalyx-configs main
```

## Lock Files

Lock files (`kalyx.lock`) are always at project root and represent the resolved state. They are not layered - they capture the concrete result of the merge.

## Questions

1. Should layers be able to `import` or `extend` other layers explicitly?
2. How do we handle conflicts (same key defined in multiple layers)? Last wins?
3. Should there be a `klx config validate` to show the merged result?
4. Array merge: always append, or allow replace semantics?
