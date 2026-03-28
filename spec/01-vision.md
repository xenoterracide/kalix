# Kalix Vision & Philosophy

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
SPDX-FileCopyrightText: 2026 Kalix Contributors

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## What is Kalix?

A build system and dependency manager for the Java ecosystem.

## Core Principles

1. **Configuration over code** - Declarative build definitions
2. **Performance** - Fast builds through smart caching and parallelism
3. **Predictability** - Reproducible builds
4. **Modern defaults** - Current JDKs, current practices

## Configuration Format

Kalix uses YAML for project configuration (supports anchors for DRY configs):

```yaml
# kalix.yaml
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
