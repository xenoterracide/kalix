# CLI Design

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> **Status:** 🚧 Draft

## No Task Chaining

Unlike Make, Maven, and Gradle, Kalix does not support `klx clean build`.

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

| Command   | Purpose                           |
| --------- | --------------------------------- |
| `build`   | Compile and package               |
| `test`    | Run tests                         |
| `run`     | Run the application               |
| `clean`   | Delete build outputs              |
| `publish` | Publish to repository             |
| `update`  | Update dependencies and lock file |
| `check`   | Run all verification              |

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

If you find yourself running `klx clean` regularly, that's a bug in Kalix. File an issue.

### Clean Command

```bash
# Delete all build outputs
klx clean

# Clean specific subproject
klx clean api
```

Simple. Destructive. For debugging only.

## Comparison

| Tool      | Chaining? | Example                      | Kalix Equivalent          |
| --------- | --------- | ---------------------------- | ------------------------- |
| Make      | Yes       | `make clean build`           | `klx clean && klx build`  |
| Maven     | Yes       | `mvn clean install`          | `klx clean && klx build`  |
| Gradle    | Yes       | `gradle clean build`         | `klx clean && klx build`  |
| Git       | **No**    | `git commit && git push`     | (encourages explicitness) |
| Cargo     | **No**    | `cargo clean && cargo build` | `klx clean && klx build`  |
| **Kalix** | **No**    | N/A                          | `klx clean && klx build`  |

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

While Kalix downloads dependencies on demand, you may want to prefetch everything (e.g., before getting on a plane):

```bash
# Download all dependencies for current project
$ klx fetch
Resolving dependencies from kalix.lock...
Downloading 47 artifacts...
Done. All dependencies cached in ~/.kalix/

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
| **Kalix** | Downloads on demand | Always works if cached, downloads if not |
| **Kalix** | `fetch` command     | Proactive download (git-inspired)        |

Kalix doesn't need `--offline` because:

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
| **Kalix** | **No**    | `klx clean && klx build` |

## Command Arguments

Single command means everything after the command is an argument **to that command**:

```bash
# Pass arguments to your application
klx run arg1 arg2 --flag value

# Pass arguments to tests
klx test --tests "*IntegrationTest"

# Pass JVM arguments
klx run --jvm "-Xmx2g" -- arg1 arg2
```

### Comparison: Gradle Application Plugin

Gradle's application plugin is notoriously painful:

```bash
# Gradle - horrible
gradle run --args="arg1 arg2"

# With clean (where does --args go??)
gradle clean run --args="arg1 arg2"  # Sometimes works, sometimes doesn't

# Multiple args with spaces
gradle run --args="arg1 'arg with spaces' arg3"
```

Kalix:

```bash
# Kalix - simple
klx run arg1 arg2

# With clean
klx clean && klx run arg1 arg2  # Explicit, clear

# Args with spaces
klx run arg1 "arg with spaces" arg3
```

### Facilitates Aliases and Plugins

Single command design enables:

```bash
# Shell alias
alias krun='klx run --jvm "-Xmx4g"'
krun arg1 arg2  # Works perfectly

# Kalix alias (in kalix.yaml)
aliases:
  serve: run --server --port 8080

# Then
klx serve  # Runs: klx run --server --port 8080

# Custom plugin commands
cargo add serde  # If we had a cargo-like plugin
klx proto generate --lang kotlin
```

### Yarn/npm Style Scripts

Even better: Yarn-style scripts where args just flow through:

```json
// package.json
{
  "scripts": {
    "dev": "node server.js --watch"
  }
}
```

```bash
# Yarn - args just work
yarn dev --port 8080
# Runs: node server.js --watch --port 8080
```

Kalix equivalent:

```yaml
# kalix.yaml
scripts:
  dev: run --continuous
  test-watch: test --watch
```

```bash
# Kalix - args just work
klx dev --port 8080
# Runs: klx run --continuous --port 8080

klx test-watch --tests "*Integration"
# Runs: klx test --watch --tests "*Integration"
```

No `--args="..."` wrapper. No special parsing. Just append and execute.

## Tools and Scripts

Kalix separates **tool definitions** from **script workflows**:

```yaml
# kalix.yaml

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

- `java-exec` - Kalix manages the artifact (download, cache, classpath)
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
- Shares Kalix's artifact cache

### Complete Example

```yaml
tools:
  # Java tools (managed by Kalix)
  ktlint:
    uses: com.pinterest.ktlint:ktlint-cli:1.2.0
    plugin: java-exec

  checkstyle:
    uses: com.puppycrawl.tools:checkstyle:10.12.0
    plugin: java-exec

  # Python tool (managed by you, executed by Kalix)
  reuse:
    run: uv run --frozen reuse

  # Node tool (managed by Kalix via node plugin)
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

- `ktlint`, `checkstyle` - Downloaded by Kalix, executed via `java -jar`
- `prettier` - Node ecosystem managed by Kalix (yarn/corepack)
- `reuse` - Installed by you via `uv tool install reuse`, executed as-is
- `git` - System tool, executed directly

### Why This Matters

Gradle and Maven make simple things hard:

| Task                       | Maven                                                | Gradle                                         | Kalix                                                                           |
| -------------------------- | ---------------------------------------------------- | ---------------------------------------------- | ------------------------------------------------------------------------------- |
| Run checkstyle on one file | Create POM, configure plugin, `mvn checkstyle:check` | Add plugin, configure, `gradle checkstyleMain` | `klx tool com.puppycrawl.tools:checkstyle:10.12.0 MyFile.java`                  |
| Git hook for linting       | Plugin configuration nightmare                       | Custom task + plugin                           | `klx tool com.pinterest.ktlint:ktlint-cli:1.2.0 --git-pre-commit-hook`          |
| One-off formatting         | `mvn formatter:format`                               | `gradle spotlessApply`                         | `klx tool com.google.googlejavaformat:google-java-format:1.22.0 -i MyFile.java` |

**Kalix philosophy:** Simple things should be one command. Config is for complex workflows, not for running a tool.

## Command Resolution

Scripts provide custom commands. They **cannot** shadow built-in commands.

### Reserved Commands

These command names are reserved by Kalix core and **cannot** be used as script names:

| Category              | Commands                                          |
| --------------------- | ------------------------------------------------- |
| Build lifecycle       | `build`, `compile`, `test`, `jar`, `run`, `clean` |
| Dependency management | `update`, `fetch`, `add`                          |
| Publishing            | `publish`                                         |
| Verification          | `check`                                           |
| Utility               | `version`, `help`                                 |

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

Unlike Yarn/npm where `yarn run` is a meta-command that executes scripts, Kalix's `klx run` is a core command that runs the compiled application. Allowing shadowing would:

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

Kalix provides a wrapper script `./klx` (like `./gradlew` but better):

```bash
# Clone and go - no installation needed
git clone git@github.com:company/project.git
cd project
./klx build  # Downloads klx automatically
```

**Wrapper behavior:**

1. Check `kalix/wrapper/kalix-wrapper.properties` for version
2. Download if not cached in `~/.kalix/wrapper/<version>/`
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

Kalix works with jbang for zero-install usage:

```bash
# Run without installing
jbang klx@kalix build

# Or install via jbang
jbang app install klx@kalix
klx build  # Now available in PATH
```

**For git hooks (the killer use case):**

```bash
#!/bin/sh
# .git/hooks/pre-commit
# Works even if klx not installed!
jbang klx@kalix tool com.pinterest.ktlint:ktlint-cli:1.2.0 --git-pre-commit-hook
```

### Wrapper Script Locations

| Platform   | Wrapper         | Cache                           |
| ---------- | --------------- | ------------------------------- |
| Unix/macOS | `./klx` (shell) | `~/.kalix/wrapper/`             |
| Windows    | `./klx.bat`     | `%USERPROFILE%\.kalix\wrapper\` |
| Universal  | `./klx` (jbang) | jbang cache                     |

### Comparison

| Tool      | Wrapper            | Global Delegates | jbang Support | Clone-and-Go |
| --------- | ------------------ | ---------------- | ------------- | ------------ |
| Make      | No                 | N/A              | ❌            | ❌           |
| Maven     | `./mvnw`           | ❌ No            | ❌            | ✅           |
| Gradle    | `./gradlew`        | ❌ No            | ❌            | ✅           |
| Yarn      | `.yarn/releases/*` | ✅ Yes           | ❌            | ✅           |
| **Kalix** | `./klx`            | ✅ Yes           | ✅ Yes        | ✅           |

## Open Questions

1. Should there be a `klx all` command that runs multiple phases explicitly?
2. Should we provide a `klx rebuild` convenience command (clean + build)?
3. Should we have a `--no-cache` flag for debugging (different from `--rerun`)?
4. Should wrapper auto-update to latest patch version (configurable)?
