# Subtree AI - AI Agent Skills Repository

SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0

---

## Project Overview

This repository contains **AI coding agent skills** designed for use with Kimi Code CLI (and compatible AI coding agents). Skills are modular, composable capabilities that provide specialized knowledge, workflow patterns, and tool integrations to enhance AI agent effectiveness.

The project is hosted at: https://github.com/xenoterracide/subtree-ai

## Repository Structure

```
.
├── mcp/                     # Model Context Protocol configuration
│   ├── mcp.json            # MCP server configuration (currently empty {})
│   └── mcp.json.license    # CC0-1.0 license for config files
├── skills/                  # AI skills organized by concern
│   ├── commit-message/     # Conventional commit and PR description format
│   ├── coding-standards/    # Cross-cutting coding principles and quality
│   ├── github/             # GitHub and GraphQL interaction patterns
│   ├── gradle/             # Gradle build system and dependency management
│   ├── iterative-development/ # Planning and iterative design guidance
│   ├── java/               # Java coding style and null-safety guidance
│   ├── pull-request/       # Commit, push, and PR workflow management
│   ├── shell-script/       # Shell scripting and command automation guidance
│   ├── session-init/       # Mandatory session startup workflow
│   ├── skill-creator/      # Creating and maintaining skills
│   ├── testing/            # Testing philosophy and patterns
│   └── use-case-creator/   # Use case documentation guidance
└── .agents/                # Symlink to root (self-referential)
    └── skills/             # Same as ./skills/
```

## Technology Stack

This is a **documentation/knowledge repository** (not a code project):

- **Format**: Markdown with YAML frontmatter for skill definitions
- **Documentation**: AsciiDoc for use case specifications
- **Formatting**: Prettier for Markdown
- **Licensing**: SPDX license identifiers (CC-BY-NC-SA-4.0 for content, CC0-1.0 for config)

## Skill System Architecture

### Skill File Format

Each skill is a directory containing `SKILL.md` with this structure:

```markdown
---
name: skill-name
description: When to use this skill. Be specific about triggers.
---

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Skill Title

Content here...
```

**CRITICAL FORMATTING RULES:**

1. `---` must be the VERY FIRST line in the file - no comments, no blank lines before
2. Frontmatter must include `name` and `description` fields
3. SPDX copyright comment goes AFTER frontmatter, in an HTML comment block
4. Use current year (2026) for new skills, not a range

### How Skills Work

Skills are recognized by Kimi when:

1. File is named exactly `SKILL.md`
2. Located in `.agents/skills/<skill-name>/` or `skills/<skill-name>/`
3. Frontmatter is valid (starts with `---`)
4. Has both `name` and `description` fields

The `description` field determines when the skill triggers, so it is the primary
machine-readable routing surface. Keep descriptions specific, concrete, and easy
to match against user intent.

`allowed-tools` is an optional, experimental field you may use when a skill
needs to pre-approve a small, trusted tool set. Prefer omitting it by default,
and only add it when a runtime supports specific tool names such as `git` or
`gh` and the skill repeatedly needs them. Keep broader routing guidance such as
anti-triggers, related skills, and detailed examples in the body of the skill
file rather than overloading frontmatter.

## Skill Routing

Skills fall into four activation categories:

- **Workflow** (`session-init`, `pull-request`, `commit-message`): Apply on every session or file change
- **Cross-cutting** (`coding-standards`): Apply to all coding tasks regardless of language
- **Domain** (`github`, `java`, `gradle`, `shell-script`, `testing`, `use-case-creator`): Apply by file type or tool context
- **Planning** (`iterative-development`, `skill-creator`): Apply when designing features or maintaining skills

### Discoverability Index

Use this table as the canonical routing guide when deciding which skill to load.

| Skill                   | Scope                                                      | Activate When                                                                            | Signals                                                   |
| ----------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------- | --------------------------------------------------------- |
| `session-init`          | Git state verification at session start                    | Starting work in a repo session                                                          | "start work", "new session", "check branch"               |
| `pull-request`          | Commit, push, and PR lifecycle; addressing review feedback | Any repository file is created, modified, or deleted; PR review comments need addressing | "fix", "update", "add", "refactor", "address PR comments" |
| `commit-message`        | Conventional commit and PR description formatting          | Writing a commit message, PR title, or PR description                                    | "write commit", "PR title", "PR description"              |
| `coding-standards`      | Cross-language coding principles and quality standards     | Implementing or changing code in any language                                            | "implement", "refactor", "bug", "error handling"          |
| `github`                | GitHub platform tools, APIs, and GraphQL queries           | Querying or interacting with GitHub-hosted resources                                     | "GitHub", "issue", "gh", "GraphQL"                        |
| `java`                  | Java language conventions and null-safety                  | Creating or modifying `.java` source files                                               | `.java`, class, interface, record, enum                   |
| `gradle`                | Gradle build system and dependency management              | Editing Gradle build files or resolving dependency issues                                | `build.gradle.kts`, `settings.gradle.kts`, dependency     |
| `shell-script`          | Shell scripting for POSIX, Bash, and Zsh                   | Writing or editing shell scripts, functions, or shell config                             | `.sh`, `.zsh`, `.zshrc`, bash, zsh, pipeline              |
| `testing`               | Test philosophy, patterns, and anti-patterns               | Adding, updating, debugging, or discussing tests                                         | test, coverage, fixture, integration                      |
| `use-case-creator`      | Use case specifications in Cockburn/AsciiDoc format        | Writing or revising use cases and business behavior docs                                 | use case, scenario, ubiquitous language                   |
| `iterative-development` | Iteration planning and domain model evolution              | Scoping a feature, selecting an iteration, or refining design                            | iteration, vertical slice, domain model, risk             |
| `skill-creator`         | Creating and maintaining AI skill definitions              | Working on `SKILL.md` files or skill trigger behavior                                    | skill, frontmatter, trigger, discoverability              |

## Development Workflow

### Making Changes

1. **Start with session-init skill** - Verify branch state before any work
2. **Apply cross-cutting and domain-specific skills** as needed for the task
3. **Always use pull-request skill** when modifying files
4. **Follow commit-message skill** for commit/PR formatting
5. **Use the AI Discoverability Index above** when the correct skill is not obvious

### Formatting Skills

After editing any `SKILL.md` file:

```bash
yarn exec prettier --write skills/<skill-name>/SKILL.md
```

### Creating New Skills

See the `skill-creator` skill for the full creation workflow and format contract.

## Code Style Guidelines

See language-specific skills (`java`, `shell-script`) for detailed style guidance.

### For Skill Files

- Keep skills concise — they share context window with everything else
- Align description wording between `AGENTS.md` and each skill
- Add boundary notes when confusion with a related skill is likely
- Put detailed reference material in `references/`, keep `SKILL.md` focused

## Testing

Skills themselves don't have automated tests (they're documentation). They are
validated by correct frontmatter format and tested by usage — if a skill's
command doesn't work, update it immediately.

For testing strategies in code projects, see the `testing` skill.

## Pull Request Workflow

This repository uses **squash merge** for PRs:

- Branch history doesn't matter - all commits get squashed
- Use `git merge origin/develop` instead of rebase when updating
- Don't create new branches for updates - commit to same branch
- PR titles must follow conventional commit format (they become commit messages)
- PR descriptions must explain WHY the change exists
- Do NOT use checkboxes (`- [x]`) in PR descriptions - use plain bullets

## License

All skills are licensed under **CC-BY-NC-SA-4.0** (Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International).

Configuration files (like `mcp.json`) are licensed under **CC0-1.0** (public domain dedication).

---

SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
