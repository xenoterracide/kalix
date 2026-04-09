---
name: session-init
description: |
  ALWAYS use this skill at the start of EVERY new session before any other work.
  Use when beginning work in a repository, verifying branch or PR state,
  refreshing the default branch, or confirming you are on current code.
---

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Session Initialization

**CRITICAL: Run these checks IMMEDIATELY at session start, BEFORE any other actions.**

When starting a new session, always verify the current repository state to avoid:

- Working on a branch with a CLOSED or MERGED PR
- Investigating code that doesn't include recent fixes
- Addressing review comments on outdated code

## Mandatory Startup Checks

Run these checks **first** before any investigation or code changes:

**Preferred: Use MCP tools when available:**

- Use `list_pull_requests` or `pull_request_read` to check PR state for current branch
- This gives structured data without parsing shell output

**Alternative: Use git and GitHub CLI:**

```bash
# 1. Fetch latest remote state and prune stale branches
git fetch --all --prune

# 2. Check current branch and git status
git status

# 3. Check if current branch has a PR and its state
gh pr view --json number,url,headRefName,state,baseRefName

# 4. Get the default branch name from origin/HEAD
DEFAULT_BRANCH=$(git rev-parse --abbrev-ref origin/HEAD | sed 's@^origin/@@')
```

## Decision Matrix

Based on the PR state, take action BEFORE proceeding:

### Case 1: No PR exists (or command fails)

```bash
# Fetch latest state and switch to default branch
git fetch --all --prune
DEFAULT_BRANCH=$(git rev-parse --abbrev-ref origin/HEAD | sed 's@^origin/@@')
git checkout "$DEFAULT_BRANCH"
git pull origin "$DEFAULT_BRANCH"
```

Then proceed with new work.

### Case 2: PR is OPEN

```bash
# Pull latest from base branch to ensure you have current state
git pull origin "$BASE_BRANCH"
```

Then proceed with work on the existing branch.

### Case 3: PR is CLOSED or MERGED

```bash
# The branch is stale - switch to default and clean up
git fetch --all --prune
DEFAULT_BRANCH=$(git rev-parse --abbrev-ref origin/HEAD | sed 's@^origin/@@')
git checkout "$DEFAULT_BRANCH"
git branch -D <old-branch-name>  # delete the stale branch
git pull origin "$DEFAULT_BRANCH"
```

Then create a fresh branch for new work.

## Complete Startup Script

Use this as a reference for session initialization logic. Requires: `git`, `gh` (GitHub CLI), `jq`.

```bash
#!/bin/bash
# Always run this first on session start

echo "=== Session Initialization ==="

# Fetch latest remote state and prune stale branches
git fetch --all --prune

# Check git status first
git status

# Get current branch
CURRENT_BRANCH=$(git branch --show-current)
echo "Current branch: $CURRENT_BRANCH"

# Get default branch from origin/HEAD
DEFAULT_BRANCH=$(git rev-parse --abbrev-ref origin/HEAD | sed 's@^origin/@@')
echo "Default branch: $DEFAULT_BRANCH"

# Check for PR state
PR_INFO=$(gh pr view --json number,state,baseRefName 2>/dev/null || echo "null")

if [ "$PR_INFO" = "null" ]; then
    echo "No PR found for current branch"

    # Ensure we're on default branch with latest
    git checkout "$DEFAULT_BRANCH"
    git pull origin "$DEFAULT_BRANCH"
else
    PR_STATE=$(echo "$PR_INFO" | jq -r '.state')
    BASE_BRANCH=$(echo "$PR_INFO" | jq -r '.baseRefName')

    echo "PR state: $PR_STATE"

    if [ "$PR_STATE" = "OPEN" ]; then
        echo "PR is open - pulling latest from $BASE_BRANCH"
        git pull origin "$BASE_BRANCH"
        echo "Ready to continue work on existing PR"
    else
        echo "PR is $PR_STATE - branch is stale, switching to $DEFAULT_BRANCH"
        git checkout "$DEFAULT_BRANCH"
        git branch -D "$CURRENT_BRANCH" 2>/dev/null || true
        git pull origin "$DEFAULT_BRANCH"
        echo "Ready for new work on fresh branch"
    fi
fi
```

## Key Principle

**Never investigate issues or start coding without first knowing your branch state.**

The few seconds spent on these checks prevents:

- Wasted time investigating already-fixed bugs
- Confusion from working on merged PRs
- Merge conflicts from stale branches
- Duplicate work from outdated codebases

## Do Not Assume Synchronized State

Your local state may be stale. The operator may merge PRs, change branches, or
modify files outside your session.

### Do not assume:

- Your local default branch is current with `origin`
- Files haven't changed since you last read them
- Branches you created are still valid (PRs may have been merged/closed)
- Your working directory is clean or as you left it

### Do not assume exclusive access to:

- The filesystem (other processes/agents may modify files)
- Environment variables (may change between invocations)
- Network ports (may be in use by other services)
- Running processes (state may not be what you expect)

**When uncertain, verify** rather than assuming state is as you left it.

## GitHub Workflow Limitations

AI agents **cannot edit GitHub workflow files** (`.github/workflows/*.yml`). This is a hard limitation — workflow file modifications will fail. Other YAML files (`.yaml`) are not affected.

### Planning Implications

When creating plans that involve workflow changes:

- Identify workflow edits as **human-required tasks**
- Provide clear instructions for the human to make the changes
- Structure the plan so AI-managed work can proceed independently

### Example Compensation Strategy

````markdown
## Plan

1. [AI] Update build configuration
2. [AI] Add new test cases for the feature
3. [HUMAN] Update .github/workflows/ci.yml:
   - Add new job step after "Run Tests":
     ```yaml
     - run: <build-tool> integration-test
     ```
4. [AI] Update documentation to reflect new CI step
````

Flag workflow-related changes early so the human can prepare or execute them in parallel.

---

SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
