# Kalyx Specification

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

This directory contains the specification for the Kalyx build system.

## Documents

| Document                                                         | Description                                           |
| ---------------------------------------------------------------- | ----------------------------------------------------- |
| [01-vision.md](01-vision.md)                                     | Core philosophy, goals, and non-goals                 |
| [02-zero-config.md](02-zero-config.md)                           | Near-zero configuration for enterprise/container apps |
| [03-configuration-schema.md](03-configuration-schema.md)         | JSON Schema for IDE support and validation            |
| [04-layered-configuration.md](04-layered-configuration.md)       | Nestable config directory with merge semantics        |
| [05-subprojects.md](05-subprojects.md)                           | Subproject structure and Maven compatibility          |
| [06-locks-and-resolution.md](06-locks-and-resolution.md)         | Lock file semantics and zero-command resolution       |
| [07-versions-and-resolutions.md](07-versions-and-resolutions.md) | Version syntax and JPMS considerations                |
| [08-ecosystem-governance.md](08-ecosystem-governance.md)         | Repository policies and best practice enforcement     |
| [09-reproducible-builds.md](09-reproducible-builds.md)           | Reproducible builds by default                        |
| [10-plugin-architecture.md](10-plugin-architecture.md)           | Minimal core + plugin system                          |
| [11-cli-design.md](11-cli-design.md)                             | Command-line interface design                         |
| [12-build-lifecycle.md](12-build-lifecycle.md)                   | Build tasks, incremental execution, caching           |
| [13-real-world-requirements.md](13-real-world-requirements.md)   | Must-solve pain points from real usage                |
| [14-dependencies-and-scopes.md](14-dependencies-and-scopes.md)   | Dependency scopes and BOM support                     |

## Specification Status

| Document                       | Status   |
| ------------------------------ | -------- |
| 01-vision.md                   | 🚧 Draft |
| 02-zero-config.md              | 🚧 Draft |
| 03-configuration-schema.md     | 🚧 Draft |
| 04-layered-configuration.md    | 🚧 Draft |
| 05-subprojects.md              | 🚧 Draft |
| 06-locks-and-resolution.md     | 🚧 Draft |
| 07-versions-and-resolutions.md | 🚧 Draft |
| 08-ecosystem-governance.md     | 🚧 Draft |
| 09-reproducible-builds.md      | 🚧 Draft |
| 10-plugin-architecture.md      | 🚧 Draft |
| 11-cli-design.md               | 🚧 Draft |
| 12-build-lifecycle.md          | 🚧 Draft |
| 13-real-world-requirements.md  | 🚧 Draft |
| 14-dependencies-and-scopes.md  | 🚧 Draft |

## Legend

- 🚧 Draft - In progress
- 📝 Planned - Not started
- ✅ Stable - Frozen
