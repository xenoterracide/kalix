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
├── mcp/                    # Model Context Protocol configuration
│   ├── mcp.json           # MCP server configuration (currently empty {})
│   └── mcp.json.license   # CC0-1.0 license for config files
├── skills/                 # AI skills organized by domain
│   ├── commit-message/    # Conventional commit format
│   ├── general-programming/ # Programming principles
│   ├── github/            # GitHub interaction patterns
│   ├── gradle/            # Gradle build system
│   ├── gradle-shadow/     # Shadow plugin for fat JARs
│   ├── java/              # Java coding style
│   ├── pull-request/      # PR workflow management
│   ├── session-init/      # Mandatory session startup
│   ├── shell-script/      # Shell scripting guidelines
│   ├── skill-creator/     # Creating new skills
│   ├── testing/           # Testing philosophy
│   └── use-case-creator/  # Use case documentation
└── .agents/               # Symlink to root (self-referential)
    └── skills/            # Same as ./skills/
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

The `description` field determines when the skill triggers - be specific about usage scenarios.

## Skill Categories

### Workflow Skills (Always Apply)

| Skill | When to Use |
|-------|-------------|
| `session-init` | **ALWAYS** at the start of EVERY new session |
| `pull-request` | **ALWAYS** when any files are modified, created, or deleted |
| `commit-message` | When writing commit messages or PR descriptions |

### Domain Skills (Apply by Context)

| Skill | When to Use |
|-------|-------------|
| `github` | Interacting with GitHub repos, issues, PRs |
| `java` | Creating or modifying `.java` source files |
| `gradle` | Editing Gradle build files, managing dependencies |
| `gradle-shadow` | Configuring Shadow plugin for fat JARs |
| `shell-script` | Writing shell scripts |
| `testing` | Creating, modifying, or discussing tests |
| `use-case-creator` | Writing use case specifications |
| `skill-creator` | Creating or updating skills |
| `general-programming` | General programming principles |

## Development Workflow

### Making Changes

1. **Start with session-init skill** - Verify branch state before any work
2. **Apply domain-specific skills** as needed for the task
3. **Always use pull-request skill** when modifying files
4. **Follow commit-message skill** for commit/PR formatting

### Formatting Skills

After editing any `SKILL.md` file:

```bash
yarn exec prettier --write skills/<skill-name>/SKILL.md
```

### Creating New Skills

1. Create directory: `skills/<skill-name>/`
2. Create `SKILL.md` with proper frontmatter (see `skill-creator` skill)
3. Add SPDX license comment after frontmatter
4. Run prettier to format
5. Follow pull-request workflow to submit

## Code Style Guidelines

### For Java Projects (referenced skills)

- Prefer `var` keyword over explicit types
- Prefer immutability (`final` fields, `record` classes, `List.of()`)
- Prefer package-private visibility over `private` (except fields)
- Use non-nullability by default with `@Nullable` for nullable types
- Avoid `internal` packages - use package-private instead
- Use builder pattern with `@Builder` from immutables library

### For Skill Files

- Keep skills concise - they share context window
- Use clear, specific descriptions for triggers
- Put detailed info in references/, keep `SKILL.md` focused
- Fix broken commands immediately - skills are living documents

## Testing

Skills themselves don't have automated tests (they're documentation). However:

- Skills should be validated for correct frontmatter format
- Prettier ensures consistent Markdown formatting
- Skills are tested by usage - if a skill's command doesn't work, update it immediately

For testing strategies in code projects, see the `testing` skill which covers:
- Prefer sociable and integration tests over solitary unit tests
- Use real collaborators, not mocks
- Test observable behavior through public APIs
- Target 90%+ coverage

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
