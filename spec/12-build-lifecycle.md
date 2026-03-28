# Build Lifecycle

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## Scripts vs Build Lifecycle

Kalix distinguishes between **scripts** (ad-hoc commands) and the **build lifecycle** (structured build process):

| Aspect           | Scripts                     | Build Lifecycle              |
| ---------------- | --------------------------- | ---------------------------- |
| **Purpose**      | Ad-hoc tasks (lint, format) | Compile, test, package       |
| **Incremental**  | No - runs every time        | Yes - only if inputs changed |
| **Caching**      | No                          | Yes - content-addressed      |
| **Dependencies** | Simple composition          | Full task graph              |
| **Examples**     | `klx lint`, `klx format`    | `klx build`, `klx test`      |

### The Node Problem

Node/npm's "everything is a script" approach:

```json
{
  "scripts": {
    "build": "tsc && webpack && ...",
    "test": "jest",
    "clean": "rm -rf dist"
  }
}
```

Problems:

- No incremental builds - `npm run build` always rebuilds everything
- No task dependencies - manual ordering with `&&`
- No caching - can't skip unchanged work
- No parallelization - sequential by default

Kalix separates concerns:

- **Scripts** for ad-hoc commands (format, lint)
- **Build lifecycle** for real builds with incremental execution

## Core Build Tasks

Kalix defines a minimal set of core tasks. Everything else is plugins.

### Task Graph

```
compileJava
├── sources (src/main/java)
├── classpath (dependencies)
└── outputs (build/classes/main)

compileTestJava
├── dependsOn: compileJava
├── sources (src/test/java)
├── classpath (dependencies + compileJava outputs)
└── outputs (build/classes/test)

test
├── dependsOn: compileTestJava
├── inputs (compileTestJava outputs, test resources)
└── outputs (build/reports/tests)

jar
├── dependsOn: compileJava
├── inputs (compileJava outputs, resources)
└── outputs (build/libs/*.jar)

build
└── dependsOn: [compileJava, test, jar]
```

### Task Definition (Plugin)

Plugins define tasks with inputs, outputs, and actions:

```java
// Java plugin defines compileJava task
public interface Task {
  String getName();
  List<Task> getDependsOn();

  // What this task reads
  FileCollection getInputs();

  // What this task produces
  FileCollection getOutputs();

  // The work
  void execute(TaskContext context);
}
```

### Incremental Execution

```bash
# First run - compiles everything
$ klx build
:compileJava - executed
:compileTestJava - executed
:test - executed
:jar - executed

# Second run - nothing changed, all cached
$ klx build
:compileJava - UP-TO-DATE
:compileTestJava - UP-TO-DATE
:test - UP-TO-DATE
:jar - UP-TO-DATE

# Change one source file
$ echo "// comment" >> src/main/java/App.java
$ klx build
:compileJava - executed (1 file)
:compileTestJava - UP-TO-DATE (inputs unchanged)
:test - UP-TO-DATE (inputs unchanged)
:jar - executed (compileJava output changed)
```

### Input/Output Tracking

Kalix tracks inputs at the content level (SHA-256):

| Task          | Inputs                                                            | Outputs      |
| ------------- | ----------------------------------------------------------------- | ------------ |
| `compileJava` | Source files, classpath JARs, compiler options                    | Class files  |
| `test`        | Test classes, test resources, main classes, test framework config | Test reports |
| `jar`         | Class files, resources, manifest                                  | JAR file     |

If any input SHA changes, the task re-executes. Otherwise, it's UP-TO-DATE.

## Configuration

Users don't define the task graph directly. They configure plugins that provide tasks:

```yaml
# kalix.yaml
plugins:
  - io.kalix.java:2.0.0

tasks:
  compileJava:
    options:
      release: 21

  test:
    options:
      parallel: true
      maxParallelForks: 4
```

## Custom Tasks

Projects can add custom tasks via plugins or inline:

```yaml
# kalix.yaml
plugins:
  - io.kalix.java:2.0.0
  - io.kalix.protobuf:2.0.0 # Adds generateProto task

tasks:
  # Custom task inline
  generateDocs:
    dependsOn: [compileJava]
    run: |
      javadoc -d build/docs/javadoc \
        -sourcepath src/main/java \
        -subpackages com.example
```

## Comparison

| Feature     | Maven            | Gradle      | Node/npm     | Kalix             |
| ----------- | ---------------- | ----------- | ------------ | ----------------- |
| Task graph  | Phases/goals     | Yes         | No (scripts) | Yes               |
| Incremental | Limited          | Yes         | No           | Yes               |
| Caching     | No               | Build cache | No           | Content-addressed |
| Parallel    | Partial          | Yes         | No           | By default        |
| Scripts     | No (exec plugin) | No          | Yes          | Yes (separate)    |

## Open Questions

1. Should custom tasks be YAML-defined or require plugins?
2. How do we handle "soft" dependencies (compile should run before test, but test can skip if compile failed)?
3. Should there be a `klx watch` for continuous build?
4. How do we integrate with IDE incremental compilation?
