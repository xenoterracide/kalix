---
name: shell-script
description: |
  Write or modify shell scripts and command-line automation. Use when creating
  or editing `.sh` files, shell functions, portable shell snippets, Zsh
  configuration, or Bash pipelines, especially when quoting, error handling,
  and safe command composition matter.
---

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Shell Script

Guidance for writing safe, maintainable shell scripts and shell-based
automation.

## When to Use This Skill

Use this skill when:

- Creating or editing `.sh` files
- Editing `.zsh`, `.zshrc`, or other Zsh-specific shell files
- Writing Bash functions, loops, or pipelines
- Writing Zsh functions, aliases, widgets, or completion helpers
- Automating CLI workflows with shell scripts
- Debugging quoting, expansion, or exit-status behavior

## When NOT to Use This Skill

Do **not** use this skill when:

- Running a one-off command that does not require script design
- Editing application code in another language with only incidental shell usage
- Working only on GitHub workflows or build configuration where another skill is
  the better primary match

## Core Practices

### 1. Match the Target Shell Explicitly

Default to POSIX shell for `.sh` scripts unless Bash or Zsh is explicitly
specified or the file is clearly shell-specific.

```sh
#!/bin/sh
set -eu
```

- Use POSIX-compatible syntax in `.sh` files by default
- Only switch to Bash-specific or Zsh-specific features when that shell is
  explicitly requested or the file is clearly shell-specific
- Make the target shell obvious with the shebang and the constructs you choose

When Bash is the right target, say so explicitly and then use Bash features
deliberately.

```bash
#!/usr/bin/env bash
set -euo pipefail
```

When Zsh is the right target, say so explicitly and use Zsh features in
shell-specific files such as `.zshrc`, `.zsh`, or Zsh plugin/config files.

```zsh
#!/usr/bin/env zsh
set -eu
```

### 2. Prefer Explicit Error Handling

Fail early and make failure visible.

- Use `set -eu` for POSIX shell unless you have a specific reason not to
- Use `set -euo pipefail` for Bash scripts unless you have a specific reason
  not to
- Check command exit codes intentionally when partial failure is acceptable
- Print actionable error messages instead of silently ignoring failures

### 3. Quote Expansions

Quote variable expansions unless you intentionally need word splitting or glob
expansion.

```bash
# GOOD
cp "$source_file" "$target_file"

# BAD
cp $source_file $target_file
```

Prefer arrays over string-building for command arguments in Bash or Zsh.

```bash
args=(--flag "$value" --output "$target")
command "${args[@]}"
```

For POSIX shell, prefer positional parameters or helper functions instead of
Bash/Zsh arrays.

### 4. Keep Scripts Composable

- Prefer small functions with a single responsibility
- Use descriptive function names
- Keep side effects close to the entrypoint
- Pass values explicitly instead of depending on ambient globals where possible

### 5. Avoid Fragile Parsing

- Prefer structured CLI output when available
- Avoid parsing human-oriented output with brittle `grep | awk | cut` chains if
  a machine-readable flag exists
- Prefer `printf` over `echo` when formatting matters

### 6. Clean Up Temporary State

Use `mktemp` and `trap` for temporary files or directories.

```bash
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT
```

## Safety Notes

- Do not use `eval` unless absolutely necessary and justified
- Avoid unbounded globs on user-controlled paths
- Prefer exact command invocation over dynamically constructed shell fragments
- Review commands for spaces, newlines, and special characters in input values
- Run `shellcheck` and `shfmt` when available
- Remember that Zsh startup files often optimize for interactive shell behavior,
  so keep interactive customizations separate from portable script logic

## Related Skills

- `coding-standards` - broader design, testing, and quality guidance
- `github` - GitHub and GraphQL operations
- `pull-request` - committing, pushing, and PR updates for script changes
- `gradle` - Gradle build logic rather than shell automation
