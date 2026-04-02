# CLI Design

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## No Task Chaining

Unlike Make, Maven, and Gradle, Kalyx does not support `klx clean build`.

```bash
# This doesn't work:
klx clean build

# You must be explicit:
klx clean && klx build
```

Each command is atomic. Clean is just a command like any other - not special, just destructive.

## 2026 CI Practices

The `clean build` pattern is from 2005 when:

- CI was persistent (dedicated build machines)
- Build artifacts accumulated between runs
- "Clean" was necessary for hygiene

In 2026:

- CI is ephemeral (containers, VMs)
- Each job starts fresh
- Caching is explicit and careful
- `clean` is unnecessary in CI

```yaml
# GitHub Actions 2026
- run: klx build
  # No clean needed - fresh container

- run: klx publish
  # Separate step, no chaining
```

## Global Flags

```
klx [global-options] <command> [command-options]
```

### Global Options

| Flag            | Meaning                               |
| --------------- | ------------------------------------- |
| `--dry-run`     | Show what would happen, don't execute |
| `--locked`      | Fail if lock file needs updates       |
| `--rerun`       | Ignore cache, re-run tasks            |
| `-v, --verbose` | More output                           |
| `-q, --quiet`   | Less output                           |

### Commands

| Command    | Purpose                                 |
| ---------- | --------------------------------------- |
| `build`    | Full build: compile, test, package      |
| `assemble` | Package all artifacts (JARs, etc.)      |
| `jar`      | Create JAR only (shortcut for assemble) |
| `compile`  | Compile only, no tests or package       |
| `test`     | Run tests                               |
| `clean`    | Delete build outputs                    |
| `update`   | Update dependencies and lock file       |
| `check`    | Run all verification (lint, etc.)       |
| `run`      | **Execute a defined script**            |
| `exec`     | **Execute a binary from a tool**        |
| `tool`     | **Execute ad-hoc tool by GAV**          |
| `publish`  | Publish to repository                   |

**Note:** There is no `run-the-application` command. Running an application is:

- A plugin concern (e.g., `io.kalyx.spring-boot` plugin provides `klx spring-boot run`)
- Or a script: `klx run start` where `scripts.start: klx exec java -jar build/libs/app.jar`

## Clean is for Debugging

`clean` is the "turn it off and on again" of build systems. You use it when something is mysteriously broken and you suspect stale cache/state.

```bash
# Something's weird with my build...
klx build
# Hmm, still broken?

klx clean
klx build
# Ah, works now. Cache bug, filing issue...
```

**Clean is not part of normal workflow.** It's for debugging. In healthy builds, you never run `klx clean`.

### When to Clean

| Scenario                              | Action                              |
| ------------------------------------- | ----------------------------------- |
| Normal development                    | Never clean                         |
| Suspected cache bug                   | `klx clean && klx build`            |
| Switching branches with major changes | `klx clean` (or let CI handle it)   |
| Before release (paranoia)             | Not needed - trust your builds      |
| CI                                    | Never clean - ephemeral environment |

If you find yourself running `klx clean` regularly, that's a bug in Kalyx. File an issue.

### Clean Command

```bash
# Delete all build outputs
klx clean

# Clean specific subproject
klx clean api
```

Simple. Destructive. For debugging only.

## Comparison

| Tool      | Chaining? | Example                      | Kalyx Equivalent          |
| --------- | --------- | ---------------------------- | ------------------------- |
| Make      | Yes       | `make clean build`           | `klx clean && klx build`  |
| Maven     | Yes       | `mvn clean install`          | `klx clean && klx build`  |
| Gradle    | Yes       | `gradle clean build`         | `klx clean && klx build`  |
| Git       | **No**    | `git commit && git push`     | (encourages explicitness) |
| Cargo     | **No**    | `cargo clean && cargo build` | `klx clean && klx build`  |
| **Kalyx** | **No**    | N/A                          | `klx clean && klx build`  |

## Other Global Flags

### `--dry-run`

```bash
$ klx --dry-run build
Would execute:
  :compileJava
  :processResources
  :classes
  :jar
```

### `klx fetch` - Download Dependencies

While Kalyx downloads dependencies on demand, you may want to prefetch everything (e.g., before getting on a plane):

```bash
# Download all dependencies for current project
$ klx fetch
Resolving dependencies from kalyx.lock...
Downloading 47 artifacts...
Done. All dependencies cached in ~/.kalyx/

# Download dependencies and tools
$ klx fetch --all
Resolving dependencies...
Downloading tools (checkstyle, ktlint, spotbugs)...
Done.

# Now all commands work without network
$ klx build  # Uses cached artifacts, no network needed
```

**Git-inspired naming:**

- `klx fetch` - Download dependencies (like `git fetch`)
- `klx fetch --all` - Download dependencies + tools

**Difference from Gradle's `--offline`:**

| Tool      | Approach            | Behavior                                 |
| --------- | ------------------- | ---------------------------------------- |
| Gradle    | `--offline` flag    | Fail if not cached                       |
| Maven     | `--offline` flag    | Fail if not cached                       |
| **Kalyx** | Downloads on demand | Always works if cached, downloads if not |
| **Kalyx** | `fetch` command     | Proactive download (git-inspired)        |

Kalyx doesn't need `--offline` because:

1. Downloads are automatic when needed
2. Lock files ensure reproducibility
3. Immutable versions mean downloads are safe
4. `fetch` command lets you prefetch when convenient

### `--rerun`

```bash
# Force recompilation even if sources unchanged
klx --rerun build
```

## Comparison

| Tool      | Chaining? | Clean Pattern            |
| --------- | --------- | ------------------------ |
| Make      | Yes       | `make clean && make`     |
| Maven     | Yes       | `mvn clean install`      |
| Gradle    | Yes       | `gradle clean build`     |
| **Kalyx** | **No**    | `klx clean && klx build` |

## `klx run` - Execute Scripts

Execute user-defined scripts from `kalyx.yaml`:

```bash
# Run a script
klx run dev
klx run test-integration
klx run server --port 8080 --debug
```

### Why No `run-the-application` Command?

npm, yarn, and uv use `run` to execute scripts - not specifically "the application". This is because:

- **Libraries** don't have applications to run
- **Multiple apps** may exist in one project
- **Application running** is a plugin concern

```bash
# Application via plugin (if spring-boot plugin installed)
klx spring-boot run

# Application via script
klx run start  # Where scripts.start: klx exec java -jar build/libs/app.jar

# Multiple applications
klx run server  # Start server
klx run cli --input data.csv  # Run CLI tool
```

### Script Arguments

Scripts defined in `kalyx.yaml` receive arguments directly:

```yaml
scripts:
  lint: ktlint
  test-watch: test --watch
```

```bash
# Run lint with extra args
klx run lint --format --color
# Executes: ktlint --format --color

# Run test-watch with filter
klx run test-watch --tests "*Integration"
# Executes: klx test --watch --tests "*Integration"
```

No `--args="..."` wrapper. No special parsing. Just append and execute.

## `klx exec` - Execute Tool Binaries

Execute binaries provided by tools, added to a managed PATH:

```yaml
tools:
  node-22:
    plugin: node
    version: 22.0.0
    provides: [node, npm, npx, corepack]
```

```bash
# Run npm from node-22 tool
klx exec npm install

# Run node
klx exec node server.js

# Ambiguous? Use namespace prefix
klx exec :node-22 npm install
```

### Namespace Prefix (`:`)

For unambiguous parsing, use `:` prefix to specify the tool namespace:

```bash
# Format: klx exec :<tool> <binary> [args...]
klx exec :node-22 npm install
klx exec :node-20 npm install
```

The `:` makes it clear where the tool name ends and the binary name begins, simplifying argument parsing.

### Conflict Resolution

If multiple tools provide the same binary:

```bash
klx exec npm
Error: Ambiguous binary 'npm' provided by multiple tools:
  - node-20
  - node-22

Use namespace prefix:
  klx exec :node-22 npm install
  klx exec :node-20 npm install
```

Scripts handle the common case without ambiguity:

```yaml
scripts:
  lint: klx exec prettier --check 'src/'
  dev: klx exec npm run dev
```

## Tools and Scripts

Kalyx separates **tool definitions** from **script workflows**:

```yaml
# kalyx.yaml

# Tools define how to run external programs
tools:
  ktlint:
    uses: com.pinterest.ktlint:ktlint-cli:1.2.0
    plugin: java-exec

  prettier:
    plugin: node
    setup: npm install -g corepack && corepack enable
    run: yarn run prettier

  jreleaser:
    plugin: jbang
    uses: jreleaser@jreleaser

  checkstyle:
    uses: com.puppycrawl.tools:checkstyle:10.12.0
    plugin: java-exec

# Scripts compose tools into workflows
scripts:
  lint:
    run:
      prettier: --check '**'
      ktlint: # Empty/null means default arguments

  format-all:
    run:
      # Sequential step
      - reuse: uv run --frozen reuse annotate --license "GPL-3.0-or-later" --copyright "Me" "**/*.java"
      # Parallel steps (map within array = parallel)
      - prettier: --ignore-unknown --write '**'
        ktlint: --format
```

### Tools

Tools define reusable external programs:

```yaml
tools:
  # Java tool - uses java -jar
  checkstyle:
    uses: com.puppycrawl.tools:checkstyle:10.12.0
    plugin: java-exec

  # Node tool - requires setup
  prettier:
    plugin: node
    setup: corepack enable && yarn install
    run: yarn prettier

  # JBang tool
  jreleaser:
    plugin: jbang
    uses: jreleaser@jreleaser
```

| Field    | Description                                           |
| -------- | ----------------------------------------------------- |
| `uses`   | Maven coordinate (for java-exec) or JBang alias       |
| `plugin` | How to execute: `java-exec`, `node`, `jbang`, `shell` |
| `setup`  | One-time setup command (optional)                     |
| `run`    | Base command (optional, defaults to tool name)        |

### Scripts

Scripts compose tools. Two forms:

**Single tool:**

```yaml
scripts:
  check: checkstyle
  # Runs: checkstyle with default args
```

**Tool with arguments:**

```yaml
scripts:
  check:
    checkstyle: -c /google_checks.xml src/
```

**Multiple tools (parallel):**

```yaml
scripts:
  lint:
    prettier: --check '**'
    ktlint: # Runs both in parallel
```

**Sequential workflow (array):**

```yaml
scripts:
  format-all:
    run:
      # Step 1: run reuse (sequential)
      - reuse: annotate --license GPL-3.0 "**/*.java"
      # Step 2: run prettier AND ktlint (parallel)
      - prettier: --write '**'
        ktlint: --format
      # Step 3: verify (sequential)
      - git: diff --exit-code
```

### YAML Null Values

Yes, YAML allows keys with no/null values:

```yaml
scripts:
  lint:
    run:
      ktlint: # Valid YAML - null value
      prettier: --check '**'
```

This means "run with default arguments". If ktlint defaults to checking all files when run with no args, this works perfectly.

Alternative explicit forms:

```yaml
ktlint: ~       # Explicit null
ktlint: null    # Explicit null
ktlint: ""      # Empty string
```

### Execution Model

```bash
$ klx lint
# 1. Resolve all tools (checkstyle, prettier, ktlint)
# 2. Download artifacts if needed
# 3. Execute parallel steps:
#    - prettier --check '**'
#    - ktlint
# 4. Wait for all to complete
# 5. Report results
```

```bash
$ klx format-all
# 1. Execute sequential array:
#    Step 1: reuse annotate ...
#    Step 2 (parallel): prettier --write '**' & ktlint --format
#    Step 3: git diff --exit-code
```

### Plugins for Tool Execution

| Plugin      | Use Case                        | Behavior                                                 | Implicit?         |
| ----------- | ------------------------------- | -------------------------------------------------------- | ----------------- |
| `exec`      | Local executables               | Runs command as-is, no downloading                       | **Yes (default)** |
| `java-exec` | Maven artifacts with Main-Class | Downloads artifact, runs `java -jar artifact.jar <args>` | No                |
| `node`      | Node.js tools                   | Sets up Node environment, runs via yarn/npm              | No                |
| `jbang`     | JBang ecosystem                 | Delegates to JBang for resolution and execution          | No                |
| `docker`    | Containerized tools             | Runs `docker run --rm <image> <args>`                    | No                |

**Default behavior:** If no `plugin` is specified, `exec` is assumed. Secure by default (doesn't download), simple for common cases.

### The `exec` Plugin (Local Commands) - Implicit Default

Unlike `java-exec` which downloads artifacts, `exec` just runs local commands. **If no plugin is specified, `exec` is assumed:**

```yaml
tools:
  # These are all equivalent
  reuse:
    plugin: exec
    run: uv run --frozen reuse

  reuse-implicit:
    run: uv run --frozen reuse # Same as above

  # Just a string - runs as shell command
  simple: echo hello # Implicit: run: echo hello
```

Explicit form when you need it:

```yaml
tools:
  # Local shell script
  local-script:
    run: ./scripts/my-script.sh

  # System tool
  git:
    run: git
```

Usage:

```yaml
scripts:
  license-check:
    reuse: lint
    # Runs: uv run --frozen reuse lint

  license-annotate:
    reuse: annotate --license "GPL-3.0-or-later" --copyright "Caleb Cushing" "**/*.java"
    # Runs: uv run --frozen reuse annotate --license "GPL-3.0-or-later" ...
```

**Key difference:**

- `java-exec` - Kalyx manages the artifact (download, cache, classpath)
- `exec` - You manage the tool (install uv, install reuse via uv, etc.)

### Ad-Hoc Tool Execution (dlx-style)

Sometimes you need to run a tool without defining it in config - like `npx` or `yarn dlx`:

```bash
# Run checkstyle on a single file without config
klx tool com.puppycrawl.tools:checkstyle:10.12.0 -c /google_checks.xml MyFile.java

# Use in git hook
#!/bin/sh
# .git/hooks/pre-commit
klx tool com.pinterest.ktlint:ktlint-cli:1.2.0 --git-pre-commit-hook
```

**Syntax:**

```bash
klx tool <gav> [args...]
klx tool --plugin java-exec <gav> [args...]  # Explicit plugin
```

Benefits:

- No YAML config needed for one-off runs
- Perfect for git hooks
- Perfect for CI one-liners
- Shares Kalyx's artifact cache

### Complete Example

```yaml
tools:
  # Java tools (managed by Kalyx)
  ktlint:
    uses: com.pinterest.ktlint:ktlint-cli:1.2.0
    plugin: java-exec

  checkstyle:
    uses: com.puppycrawl.tools:checkstyle:10.12.0
    plugin: java-exec

  # Python tool (managed by you, executed by Kalyx)
  reuse:
    run: uv run --frozen reuse

  # Node tool (managed by Kalyx via node plugin)
  prettier:
    plugin: node
    setup: corepack enable
    run: yarn prettier

scripts:
  lint:
    run:
      checkstyle: -c /google_checks.xml src/
      ktlint:
      prettier: --check '**'
      reuse: lint

  format-all:
    run:
      - reuse: annotate --license "GPL-3.0-or-later" "**/*.java"
      - prettier: --write '**'
        ktlint: --format
      - git: diff --exit-code
```

In this example:

- `ktlint`, `checkstyle` - Downloaded by Kalyx, executed via `java -jar`
- `prettier` - Node ecosystem managed by Kalyx (yarn/corepack)
- `reuse` - Installed by you via `uv tool install reuse`, executed as-is
- `git` - System tool, executed directly

### Why This Matters

Gradle and Maven make simple things hard:

| Task                       | Maven                                                | Gradle                                         | Kalyx                                                                           |
| -------------------------- | ---------------------------------------------------- | ---------------------------------------------- | ------------------------------------------------------------------------------- |
| Run checkstyle on one file | Create POM, configure plugin, `mvn checkstyle:check` | Add plugin, configure, `gradle checkstyleMain` | `klx tool com.puppycrawl.tools:checkstyle:10.12.0 MyFile.java`                  |
| Git hook for linting       | Plugin configuration nightmare                       | Custom task + plugin                           | `klx tool com.pinterest.ktlint:ktlint-cli:1.2.0 --git-pre-commit-hook`          |
| One-off formatting         | `mvn formatter:format`                               | `gradle spotlessApply`                         | `klx tool com.google.googlejavaformat:google-java-format:1.22.0 -i MyFile.java` |

**Kalyx philosophy:** Simple things should be one command. Config is for complex workflows, not for running a tool.

## Command Resolution

Scripts provide custom commands. They **cannot** shadow built-in commands.

### Reserved Commands

These command names are reserved by Kalyx core and **cannot** be used as script or tool names:

| Category              | Commands                                               |
| --------------------- | ------------------------------------------------------ |
| Build lifecycle       | `build`, `assemble`, `jar`, `compile`, `test`, `clean` |
| Dependency management | `update`, `fetch`, `add`                               |
| Publishing            | `publish`                                              |
| Verification          | `check`                                                |
| Execution             | `run`, `exec`, `tool`                                  |
| Utility               | `version`, `help`                                      |

**Note:** `run`, `exec`, and `tool` are reserved as **verbs** that execute things, not as targets.

### Error on Conflict

```yaml
scripts:
  build: echo "custom build" # ERROR: 'build' is a reserved command
```

```bash
$ klx build
Error: Script 'build' conflicts with built-in command.

Built-in commands cannot be overridden. Choose a different name:
  scripts:
    my-build: echo "custom build"

Then run: klx my-build
```

### Valid Script Names

Script names must:

- Not match reserved commands (case-sensitive)
- Use lowercase letters, numbers, hyphens
- Start with a letter

```yaml
scripts:
  # Valid - no conflict
  lint: ktlint src/main/kotlin
  format: ktlint -F src/main/kotlin
  ci-check: klx check && klx test

  # Invalid - reserved words
  build: echo "no" # ERROR: reserved
  test-integration: ... # OK: 'test-integration' != 'test'
```

### Why Prevent Shadowing?

Scripts and tools use the `run` and `tool` commands to execute. These names are reserved because:

1. Break core functionality unpredictably
2. Confuse team members ("why does `klx build` do something weird?")
3. Make documentation unreliable

Explicit error > silent surprising behavior.

## Subproject Selection

Commands can target specific subprojects:

```bash
# Build everything
klx build

# Build only api subproject
klx build api

# Build api and service
klx build api service
```

## Wrapper and Delegation

### The ./klx Wrapper

Kalyx provides a wrapper script `./klx` (like `./gradlew` but better):

```bash
# Clone and go - no installation needed
git clone git@github.com:company/project.git
cd project
./klx build  # Downloads klx automatically
```

**Wrapper behavior:**

1. Check `kalyx/wrapper/kalyx-wrapper.properties` for version
2. Download if not cached in `~/.kalyx/wrapper/<version>/`
3. Verify checksum
4. Delegate to downloaded binary

### Global Installation Delegates to Wrapper

Unlike Gradle, **globally installed `klx` automatically delegates to `./klx` wrapper** (like Yarn):

```bash
# You have klx 2.0.0 installed globally
klx --version  # 2.0.0

cd ~/projects/my-project  # Has ./klx wrapper with version 1.5.0
klx --version  # 1.5.0 (delegated to wrapper)
./klx --version  # 1.5.0 (explicit wrapper)
```

**Benefits:**

- Type `klx` or `./klx` interchangeably
- Always use project-specified version
- No "oops I used the wrong version" mistakes

### jbang Support

Kalyx works with jbang for zero-install usage:

```bash
# Run without installing
jbang klx@kalyx build

# Or install via jbang
jbang app install klx@kalyx
klx build  # Now available in PATH
```

**For git hooks (the killer use case):**

```bash
#!/bin/sh
# .git/hooks/pre-commit
# Works even if klx not installed!
jbang klx@kalyx tool com.pinterest.ktlint:ktlint-cli:1.2.0 --git-pre-commit-hook
```

### Wrapper Script Locations

| Platform   | Wrapper         | Cache                           |
| ---------- | --------------- | ------------------------------- |
| Unix/macOS | `./klx` (shell) | `~/.kalyx/wrapper/`             |
| Windows    | `./klx.bat`     | `%USERPROFILE%\.kalyx\wrapper\` |
| Universal  | `./klx` (jbang) | jbang cache                     |

### Comparison

| Tool      | Wrapper            | Global Delegates | jbang Support | Clone-and-Go |
| --------- | ------------------ | ---------------- | ------------- | ------------ |
| Make      | No                 | N/A              | ❌            | ❌           |
| Maven     | `./mvnw`           | ❌ No            | ❌            | ✅           |
| Gradle    | `./gradlew`        | ❌ No            | ❌            | ✅           |
| Yarn      | `.yarn/releases/*` | ✅ Yes           | ❌            | ✅           |
| **Kalyx** | `./klx`            | ✅ Yes           | ✅ Yes        | ✅           |

## Open Questions

1. Should there be a `klx all` command that runs multiple phases explicitly?
2. Should we provide a `klx rebuild` convenience command (clean + build)?
3. Should we have a `--no-cache` flag for debugging (different from `--rerun`)?
4. Should wrapper auto-update to latest patch version (configurable)?
