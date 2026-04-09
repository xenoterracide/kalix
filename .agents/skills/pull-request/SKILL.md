---
name: pull-request
description: |
  ALWAYS use when files are modified, created, or deleted — including bugs,
  features, refactoring, or config changes. Also use when addressing PR review
  comments or feedback. Handles committing, pushing, and PR management through
  GitHub.
---

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Pull Request

**This skill applies whenever files are created, modified, or deleted.**

- Apply `coding-standards` rules — review your code against them before submitting
- Use `commit-message` skill for all commit messages and PR descriptions
- Keep the PR description up to date (it becomes the squash-merge commit message)
  - Do NOT use checkboxes (`- [x]`) — use plain bullet lists (`- item`)
- files should be committed and pushed
  - ensure code compiles and tests pass before committing
    - run relevant, specific tests first for quick feedback
    - prefer `./gradlew compile` for compile-only checks
    - run `./gradlew test` for quick test logic verification
    - run `./gradlew checkstyle` for checkstyle verification
    - run full `./gradlew check` before finalizing or when changes affect multiple modules.
  - ensure documentation is up to date
    - review changes to understand what documentation may need updates
    - update `README.md` if user-facing behavior changes
    - update `AGENTS.md` if build processes, tools, or agent workflows change
    - when renaming workflows or changing their interface, update both README and AGENTS.md
  - verify GitHub PR checks pass after pushing
    - use available tools to check workflow status
    - fix any failures before requesting review
    - if GitHub checks fail after pushing, fix before requesting review
- **NEVER force push.** Force pushes (`--force` and `--force-with-lease`) are **blocked by repository rules** and will always fail. If a push is rejected, do not attempt force push — create a new commit instead.
- must be synchronized with HEAD branch using a merge strategy
  - it is easier to delete and regenerate lockfiles than merge them
- respond to ALL pr comments.
  - ALWAYS leave a reply on each review comment to indicate status
  - If fixed: comment "Fixed" or "Done" with brief explanation
  - If not an issue: comment explaining why (e.g., "Not applicable because...", "Already resolved...")
  - If uncertain: ask for clarification
  - This helps humans know whether to resolve the comment
  - only address UNRESOLVED comments - check review thread resolution status using GraphQL
  - use GraphQL to get review threads with `isResolved` field

## Workflow

**NEVER update MERGED PRs.** If a PR is merged, create a new branch for any follow-up work.

When committing and creating/updating a PR, follow this workflow:

1. **Check current branch status** - Use MCP tools (preferred) or `gh` CLI:
   - Use `pull_request_read` or `list_pull_requests` MCP tools to check PR state
   - Or run `git status` and `gh pr view --json number,url,headRefName,state`
   - Determine: current branch, existing PR status (OPEN/CLOSED/MERGED)

2. **Ensure branch is current** — see `session-init` for full startup checks:
   - Fetch and prune: `git fetch --all --prune`
   - If the current branch has a CLOSED or MERGED PR, switch to the default branch and create a new one
   - Pull latest changes before starting work

3. **If already on a feature branch with an existing OPEN PR:**
   - Do NOT create a new branch
   - Pull latest changes first
   - Commit changes to the current branch
   - Push to update the existing PR
   - Update PR description/title if needed using `gh pr edit`

4. **If on the default branch or no PR exists for current branch:**
   - Create a new feature branch (if not already on one)
   - Commit changes
   - Push and create a new PR

5. **Before finalizing:**
   - Review if documentation needs updates (README.md, AGENTS.md)
   - Ensure PR description accurately reflects all changes including doc updates

### Self-Review Before Submitting

Before creating or updating a PR:

1. Run quality checks locally (tests, static analysis, formatting)
2. Review your own diff — would you approve this if someone else wrote it?
3. Check for obvious issues (debug prints, TODOs without tickets, unjustified suppressions)

See `coding-standards` (Rule 5: Code Quality Standards) for the full checklist.

**Fix issues yourself before requesting human review.**

### Squash Merge Strategy

This repository uses **squash merge** for PRs. This means:

- **Branch history doesn't matter** - All commits get squashed into one on merge
- **Merge is preferred over rebase** - When updating a branch with changes from develop, use `git merge origin/develop` instead of `git rebase`
- **Don't create new branches for updates** - If a PR needs changes, commit to the same branch and push
- **Don't worry about messy commits** - The squash merge cleans everything up

**When to update a branch:**

- If develop has moved forward and you need those changes: `git merge origin/develop`
- If review feedback requires changes: commit and push to same branch
- Never force push (including `--force-with-lease`) — it's unnecessary with squash merge and may be blocked by repository rules

## Creating/Updating PRs

### PR Title Format

Follow conventional commit format for PR titles (they become the squash merge commit message):

```
<type>(<scope>): <summary>
```

- Use commit types from `git-conventional-commits.yaml` (feat, fix, docs, etc.)
- Keep title <= 72 characters
- Use specific scope when possible

### Commit Message and PR Body Format

Follow the `commit-message` skill for commit message format, PR body structure,
and the mandatory "why" paragraph. PR descriptions become permanent commit
history via squash merge.

### Creating a New PR

Always provide explicit title and body. Do NOT use `--fill` as it may use the branch name instead of a proper conventional commit message:

```bash
# Get the commit message for the title
TITLE=$(git log -1 --format="%s" HEAD)

# Create PR with proper title and body
gh pr create --title "$TITLE" --body "- Bullet point describing change 1
- Bullet point describing change 2"
```

### Updating an Existing PR

```bash
gh pr edit --title "$TITLE" --body "- Updated bullet points"
```

## Handling Review Comments

When addressing review comments on a PR:

1. **Pull first** - Always pull the latest changes before starting
2. **Query unresolved comments** - Use MCP tools or GraphQL (see `github` skill for GraphQL examples):
   - Query review threads with `isResolved` field to find unresolved comments
   - Get thread IDs for replying
3. **Reply to each comment** - Use MCP tools or GraphQL mutation `addPullRequestReviewThreadReply` (see `github` skill):
   - Fixed: "Fixed in commit SHA"
   - Not an issue: "Not applicable: [reason]"
   - Question: "Question: [clarification needed]"
4. **Verify fixes** - Confirm changes address the current code state

**Note:** REST API doesn't expose resolved state - use GraphQL (`reviewThreads.isResolved`) or MCP tools to check unresolved comments.

## AI Attribution

When creating commits for a PR, include AI attribution in commit messages using a Co-authored-by trailer:

```
Co-authored-by: <AI_NAME> <AI_NOREPLY_EMAIL>
```

Use your AI identity:

| AI Name | `AI_NAME`        | `AI_NOREPLY_EMAIL`           |
| ------- | ---------------- | ---------------------------- |
| Kimi    | `Kimi`           | `kimi@moonshot.localhost`    |
| Copilot | `GitHub Copilot` | `copilot@github.localhost`   |
| Claude  | `Claude`         | `claude@anthropic.localhost` |

Place the Co-authored-by trailer at the end of the commit message body, after the description.

---

SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
