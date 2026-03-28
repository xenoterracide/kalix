<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Kalix Build System

SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0

---

## Project Overview

**Kalix** is a build system and dependency manager for the Java ecosystem. It aims to provide:

- **Configuration over code** - Declarative build definitions
- **Performance** - Fast builds through smart caching and parallelism
- **Predictability** - Reproducible builds
- **Modern defaults** - Current JDKs, current practices

The project is currently in early specification phase. The `spec/` directory contains the design documents.

## Repository Structure

```
.
├── .agents/                    # AI agent skills (symlinked from .share/.agents)
│   ├── mcp/mcp.json           # Model Context Protocol configuration
│   └── skills/                # AI coding agent skills
├── .share/                     # Shared tooling submodule
│   ├── .agents/               # Source of truth for agent skills
│   ├── .github/workflows/     # GitHub Actions workflows
│   ├── git/hooks/             # Git hooks
│   ├── node/packages/         # Node.js packages
│   │   └── merge/             # PR merge automation tool
│   └── package.json           # Shared Node.js workspace config
├── spec/                       # Project specification documents
│   ├── README.md              # Specification index
│   └── 01-vision.md           # Core philosophy and goals
├── LICENSES/                   # SPDX license texts
├── package.json               # Root Node.js configuration
├── pyproject.toml             # Python project configuration
├── git-conventional-commits.yaml  # Conventional commit config
├── REUSE.toml                 # REUSE compliance configuration
└── renovate.json5             # Renovate bot configuration
```

## Technology Stack

### Package Managers

- **Node.js**: Yarn 4.x with Plug'n'Play (PnP) - no `node_modules` at root
- **Python**: uv (modern Python package manager)

### Tool Versions (managed by asdf)

```
nodejs 24.11.1
python 3.14.3
shfmt  3.13.0
```

### Development Tools

- **Prettier**: Code formatting with plugins for XML, Java, Properties, TOML
- **lint-staged**: Pre-commit linting
- **git-conventional-commits**: Commit message linting
- **REUSE**: License compliance checking
- **Renovate**: Automated dependency updates

## Build and Development Commands

### Initial Setup

```bash
# Full setup (run after cloning)
yarn contribute

# Or step by step:
yarn setup:corepack    # Install and enable corepack
yarn setup:submodules  # Initialize git submodules
yarn setup:yarn        # Install dependencies
```

### Linting

```bash
yarn lint              # Run all linters
yarn lint:prettier     # Check formatting
yarn lint:reuse        # Check REUSE compliance
```

### Testing

```bash
yarn test              # Run all workspace tests
cd .share && yarn test # Run shared package tests
```

### Merge Automation

```bash
yarn merge:kimi        # AI-assisted PR merge using Kimi
yarn merge:copilot     # AI-assisted PR merge using Copilot
yarn merge:junie       # AI-assisted PR merge using Junie
```

### Python (uv)

```bash
uv sync --frozen       # Sync Python dependencies
uv run --frozen --group dev reuse lint  # Run REUSE linter
```

## Code Style Guidelines

### EditorConfig

- Charset: UTF-8
- End of line: LF
- Indent: 2 spaces
- Insert final newline: true

### Prettier Configuration

- Print width: 120 characters
- XML whitespace sensitivity: ignore
- Key separator: `=`

### File Type Conventions

| Extension                  | License          | Notes                       |
| -------------------------- | ---------------- | --------------------------- |
| `.json` (not package.json) | CC0-1.0          | Configuration               |
| `package.json`             | MIT              | Scripts contain logic       |
| `.js`, `.cjs`, `.yml`      | MIT              | Scripts/workflows           |
| `.md`, `.adoc`             | CC-BY-NC-SA-4.0  | Documentation               |
| `.xml`, `.yaml`, `.toml`   | CC0-1.0          | Configuration               |
| `.ts`, `.java`             | GPL-3.0-or-later | Source code                 |
| `.properties`              | CC0-1.0          | No Unicode copyright symbol |
| Shell scripts              | MIT              | Use shfmt --style python    |

## Git Workflow

### Conventional Commits

Commit types (defined in `git-conventional-commits.yaml`):

- `ci` - CI/CD changes
- `feat` - New features
- `fix` - Bug fixes
- `perf` - Performance improvements
- `refactor` - Code restructuring
- `style` - Formatting changes
- `test` - Test additions/changes
- `build` - Build system changes
- `ops` - Operational changes
- `docs` - Documentation
- `chore` - Maintenance tasks
- `merge` - Merge commits
- `revert` - Reverts

### Git Hooks

Located in `.share/git/hooks/`:

- **pre-commit**: Runs `yarn lint-staged`
- **commit-msg**: Validates conventional commit format
- **post-checkout**: Post-checkout actions
- **post-merge**: Post-merge actions

Enable hooks:

```bash
git config core.hooksPath .share/git/hooks
```

### Pull Request Workflow

- Uses **squash merge** strategy
- PR titles must follow conventional commit format
- PR descriptions must explain WHY the change exists
- Use `git merge origin/HEAD` instead of rebase
- Branch history doesn't matter (gets squashed)

## AI Agent Skills System

The project includes a sophisticated skill system for AI coding agents.

### Skill Structure

Each skill is in `.agents/skills/<skill-name>/SKILL.md`:

```markdown
---
name: skill-name
description: When to use this skill. Be specific.
---

<!-- SPDX copyright comment -->

# Skill Title

Content...
```

**CRITICAL FORMATTING RULES:**

1. `---` must be the VERY FIRST line (no comments before)
2. Frontmatter must include `name` and `description`
3. SPDX comment goes AFTER frontmatter in HTML comment block
4. Use current year (2026) for new skills

### Workflow Skills (Always Apply)

| Skill            | Purpose                                               |
| ---------------- | ----------------------------------------------------- |
| `session-init`   | **ALWAYS** run at session start to check branch state |
| `pull-request`   | **ALWAYS** use when modifying any files               |
| `commit-message` | Use when writing commits or PR descriptions           |

### Domain Skills (Apply by Context)

| Skill                 | When to Use                      |
| --------------------- | -------------------------------- |
| `github`              | GitHub repos, issues, PRs        |
| `java`                | Creating/modifying `.java` files |
| `gradle`              | Gradle build files, dependencies |
| `gradle-shadow`       | Shadow plugin configuration      |
| `shell-script`        | Writing shell scripts            |
| `testing`             | Creating/modifying tests         |
| `use-case-creator`    | Writing use case specifications  |
| `skill-creator`       | Creating/updating skills         |
| `general-programming` | General programming principles   |

### Session Initialization Protocol

**ALWAYS** run at the start of EVERY session:

1. Check current branch: `git branch --show-current`
2. Check PR state: `gh pr view --json number,state`
3. Based on state:
   - **No PR**: Switch to default branch, pull latest
   - **OPEN**: Pull latest, continue work
   - **CLOSED/MERGED**: Delete branch, switch to default, pull latest

## Testing Strategy

### Philosophy

- **Prefer sociable tests** over solitary unit tests
- **Use real collaborators**, not mocks
- **Test behavior through public APIs**
- Target 90%+ coverage

### Test Types

1. **Sociable Tests** - Unit under test with real dependencies
2. **Narrow Integration Tests** - One integration point at a time
3. **Avoid** - Solitary tests with excessive mocking

### When to Use Test Doubles

Only for:

- External services you don't control
- Non-deterministic behavior
- Extremely slow operations
- Infrastructure you can't run locally

## Licensing

All files must have SPDX license identifiers.

### License Mapping

| Content Type                        | License          |
| ----------------------------------- | ---------------- |
| Source code (.ts, .java)            | GPL-3.0-or-later |
| Documentation (.md, .adoc)          | CC-BY-NC-SA-4.0  |
| Configuration (.json, .yaml, .toml) | CC0-1.0          |
| Scripts (.js, shell)                | MIT              |
| Package metadata (package.json)     | MIT              |

### REUSE Compliance

```bash
# Check compliance
yarn lint:reuse

# Or directly
uv run --frozen --group dev reuse lint

# Annotate new files
uv run --frozen --group dev reuse annotate \
  --copyright "Caleb Cushing" \
  --license "GPL-3.0-or-later" \
  <file>
```

## Dependency Management

### Node.js (Yarn)

- Uses Yarn 4.x with PnP (Plug'n'Play)
- Zero-installs: `.pnp.cjs` and `.pnp.loader.mjs` committed
- Cache stored in `.yarn/cache/`

### Python (uv)

- `pyproject.toml` - Project metadata and dependencies
- `uv.lock` - Locked dependency versions
- `requires-python = ">=3.12"`

### Renovate Configuration

Automated updates configured in `renovate.json5`:

- Gradle dependencies: Daily at 04:00 UTC (major only)
- Gradle plugins: Weekly Wednesday at 05:00 UTC
- npm/asdf devDependencies: Weekly Wednesday at 04:00 UTC
- GitHub Actions: Auto-merge enabled

## CI/CD

GitHub Actions workflow (`.share/.github/workflows/pre-commit.yml`):

- **license**: REUSE compliance check
- **prettier**: Code formatting check

## Working with the `.share` Submodule

The `.share` directory contains shared tooling and is maintained as a submodule:

```bash
# Update submodule
git submodule update --init --recursive

# The .share directory has its own:
# - package.json with workspaces
# - Node.js packages in node/packages/
# - Git hooks in git/hooks/
# - GitHub workflows in .github/workflows/
```

## Useful Commands Reference

```bash
# Check git status
git status

# View PR information
gh pr view --json number,state,title,body

# List PRs
gh pr list

# Format with prettier
yarn exec prettier --write <file>

# Run git hooks manually
yarn lint-staged

# Conventional commit check
yarn git-conventional-commits commit-msg-hook <msg-file>
```

---

SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
