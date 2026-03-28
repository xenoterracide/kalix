# Configuration Schema

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## Philosophy

Like Maven, Kalix has a minimal core. Most functionality lives in plugins (even first-class ones). Unlike Gradle, we avoid a monolithic self-anchoring architecture.

YAML provides flexibility without marrying configuration to implementation. However, we need:

- **IDE ergonomics** - autocomplete, navigation
- **Validation** - catch typos early
- **Documentation** - inline help

## Solution: JSON Schema

Kalix provides a JSON Schema that the IDE consumes for:

- Type-aware autocomplete
- Real-time validation
- Hover documentation

```yaml
# kalix.yaml - IDE knows this structure from schema
project:
  name: my-app
  version: 1.0.0

dependencies:
  compile:
    - org.slf4j:slf4j-api:2.0.9

plugins:
  - io.kalix.container:2.0.0

container:
  base: eclipse-temurin:21-jre-alpine
```

The IDE validates `container:` because the container plugin contributed that schema.

## Schema Architecture

### Core Schema

Defines minimal Kalix without plugins:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://kalix.dev/schemas/core.json",
  "title": "Kalix Configuration",
  "type": "object",
  "properties": {
    "project": {
      "type": "object",
      "properties": {
        "name": { "type": "string" },
        "version": { "type": "string" }
      }
    },
    "dependencies": {
      "$ref": "#/definitions/dependencies"
    },
    "plugins": {
      "type": "array",
      "items": { "type": "string" }
    }
  }
}
```

### Plugin Schema Contributions

Plugins can contribute schema fragments. The `klx` CLI aggregates:

```yaml
# From plugin io.kalix.container
{
  "$id": "https://kalix.dev/schemas/plugins/container.json",
  "properties":
    {
      "container":
        {
          "type": "object",
          "properties":
            {
              "base": { "type": "string", "description": "Base container image" },
              "port": { "type": "integer", "description": "Exposed port" },
            },
        },
    },
}
```

### Schema Aggregation

When a plugin is declared in `kalix.yaml`, `klx` merges its schema:

```bash
$ klx schema update  # generates .kalix/schema.json for IDE
```

Or the IDE plugin calls `klx schema export` to get the merged schema.

## Schema Sources

| Source                  | Purpose                                    |
| ----------------------- | ------------------------------------------ |
| Core (in `klx` binary)  | Minimal project structure                  |
| Built-in plugins        | Container, native-image, code-gen          |
| Third-party plugins     | Their own schema, discovered via classpath |
| User-defined (optional) | Additional constraints                     |

## Validation Levels

| Level       | Behavior                               |
| ----------- | -------------------------------------- |
| IDE         | Warnings for unknown keys, type errors |
| `klx check` | Full validation, fails on errors       |
| `klx build` | Validation with plugin loading         |

## Example: Type Safety

```yaml
# ❌ Caught by IDE (schema violation)
dependencies:
  compile:
    - org.slf4j:slf4j-api # missing version

# ❌ Caught by IDE (typo)
dependecies:
  compile: []

# ❌ Caught by IDE (unknown key in plugin config)
container:
  base-image: eclipse-temurin:21 # should be 'base', not 'base-image'
```

## Implementation Notes

- SnakeYAML Engine parses YAML 1.2
- Schema validates the resulting JSON-equivalent structure
- Lock file (`kalix.lock`) is JSON (no schema needed - machine generated)
- Plugin schemas bundled in plugin JARs at `META-INF/kalix/schema.json`
