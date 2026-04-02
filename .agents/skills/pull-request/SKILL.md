---
name: pull-request
description: |
  ALWAYS use when files are modified, created, or deleted - including bugs,
  features, refactoring, or config changes. Handles committing, pushing, and
  PR management through GitHub.

  Use when: making file changes, adding features, fixing bugs, refactoring,
  updating docs or configs. Must be used with domain-specific skills.
---

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

**CRITICAL: This skill must ALWAYS be used whenever files are created, modified, or deleted, regardless of what other skills are also being applied.**

**CRITICAL: Apply general-programming rules** - All code changes must follow the
rules in `general-programming` skill (error handling, tests, immutability,
libraries, code quality). Review your own code against these rules before
submitting.

- use commit-message
- keep the pull request message up to date
  - NOTE: The PR description becomes the commit message when the PR is squash-merged
  - Follow the commit-message format for PR descriptions since they become permanent commit history
  - DO NOT use checkboxes (`- [x]`) in PR descriptions - they render poorly in commit messages
  - Use plain bullet lists (`- item`) instead of GitHub task lists
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
    - if Github checks fail after pushing, fix before requesting review
- git push --force is not allowed
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

When committing and creating/updating a PR, follow this workflow:

1. **Check current branch status** - Use MCP tools (preferred) or `gh` CLI:
   - Use `pull_request_read` or `list_pull_requests` MCP tools to check PR state
   - Or run `git status` and `gh pr view --json number,url,headRefName,state`
   - Determine: current branch, existing PR status (OPEN/CLOSED/MERGED)

2. **Handle closed/merged PRs:**
   - If the current branch has a CLOSED or MERGED PR, delete the local branch:
     - `git checkout develop` (the default HEAD branch)
     - `git branch -D <old-branch-name>`
   - Then create a new branch off the updated HEAD for new work

3. **Pull latest changes before starting work:**
   - Run `git pull origin develop` to get the latest changes
   - This ensures you're working on the current state and not outdated code
   - This also ensures you don't address review comments that are already resolved

4. **If already on a feature branch with an existing OPEN PR:**
   - Do NOT create a new branch
   - Pull latest changes first
   - Commit changes to the current branch
   - Push to update the existing PR
   - Update PR description/title if needed using `gh pr edit`

5. **If on develop or no PR exists for current branch:**
   - Create a new feature branch (if not already on one)
   - Commit changes
   - Push and create a new PR

6. **Before finalizing:**
   - Review if documentation needs updates (README.md, AGENTS.md)
   - Ensure PR description accurately reflects all changes including doc updates

### Self-Review Before Submitting

Before creating or updating a PR, review your own code:

1. **Run quality checks locally:**
   - Tests pass with adequate coverage
   - Static analysis passes (Checkstyle, SpotBugs, Error Prone, etc.)
   - Code formatting is correct

2. **Review against general-programming rules:**
   - Error handling is explicit (no silent catches)
   - Immutability preferred where appropriate
   - Existing libraries used instead of reinventing
   - Code quality standards met (Rule 7)

3. **Check for obvious issues:**
   - No commented-out code or debug prints
   - No TODOs without ticket references
   - No suppressions without justification

4. **Review the diff:**
   - Would you approve this if someone else wrote it?
   - Is the "why" clear from comments and documentation?

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
- Avoid force push - repository rules may block it, and it's unnecessary with squash merge

## Creating/Updating PRs

### PR Title Format

Follow conventional commit format for PR titles (they become the squash merge commit message):

```
<type>(<scope>): <summary>
```

- Use commit types from `git-conventional-commits.yaml` (feat, fix, docs, etc.)
- Keep title <= 72 characters
- Use specific scope when possible

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

### PR Body Format (MANDATORY)

PR description MUST include a body explaining the change:

1. **Why paragraph (REQUIRED)** - Start with a paragraph explaining WHY this change exists:
   - What problem does this solve?
   - What motivated this change?
   - Why is this the right approach?
   - This context is crucial for code review and future maintainers

2. **What bullets** - Follow with bullet points describing WHAT changed:
   - Each bullet describes one complete logical change
   - Be specific about what was modified
   - Reference specific files or components if helpful

- Wrap all lines to <= 72 characters
- Use plain bullet lists (`- item`) not checkboxes

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
