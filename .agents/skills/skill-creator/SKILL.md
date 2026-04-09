---
name: skill-creator
description: |
  Create and maintain AI skills for this project. Use when adding new skills,
  updating existing `SKILL.md` files, fixing frontmatter, or improving skill
  trigger wording and discoverability.
---

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Skill Creator

Guidance for creating and maintaining AI skills.

## File Structure

```
skill-name/
├── SKILL.md (required)
│   └── Markdown instructions
└── (optional resources)
```

## SKILL.md Format

**CRITICAL:** Skills are fussy with frontmatter. The `---` must be the very
first line in the file.

### Frontmatter Fields

Required fields:

- **`name`**: Skill identifier
  - Max 64 characters
  - Lowercase letters, numbers, and hyphens only
  - Must not start or end with a hyphen
  - Examples: `java`, `pull-request`, `gradle-shadow`

- **`description`**: When to use this skill (this triggers the skill)
  - Max 1024 characters, non-empty
  - Be specific about triggers and usage scenarios
  - Include "when to use" guidance here, not in the body
  - Prefer a multi-line description when the trigger needs examples or
    conditions
  - Reuse the same trigger vocabulary that appears in `AGENTS.md`

Optional field:

- **`allowed-tools`**: Tools that may be pre-approved for the skill
  - Experimental; support may vary by agent implementation
  - Use only when a skill repeatedly needs the same trusted tool
  - Prefer specific tool names such as `git` or `gh` when the runtime supports
    them
  - Avoid broad approvals like `bash` or `shell` unless the skill is explicitly
    about running trusted shell scripts
  - Do **not** use this as a substitute for clear instructions in the body

Avoid adding unrelated metadata just because the format permits it. Keep
frontmatter small and focused on activation plus carefully chosen pre-approval.

### Correct Structure

```markdown
---
name: skill-name
description: |
  When to use this skill. Be specific about triggers.
  Include the kinds of user requests or file changes that should activate it.
allowed-tools: git gh
---

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Skill Title

Content here...
```

### Common Mistakes

❌ **WRONG:** Comment before frontmatter

```markdown
<!--
SPDX-FileCopyrightText: ...
-->

---

name: skill-name
...
```

❌ **WRONG:** Blank line before frontmatter

```markdown
---
name: skill-name
...
```

✅ **CORRECT:** Frontmatter first, then HTML comment for copyright

```markdown
---
name: skill-name
description: ...
---

<!--
SPDX-FileCopyrightText: ...
-->
```

## Copyright and Licensing

- **Year:** Use current year (e.g., `2026`) for new skills, not a range
- **Location:** Place in HTML comment after frontmatter
- **Tool:** Do NOT use `reuse annotate` - it doesn't work with skill files
- **Manual:** Add the SPDX comment block manually

## Code Style

- Run `yarn exec prettier --write SKILL.md` after editing
- Prettier handles Markdown formatting
- No additional linting tools required for skills

## Testing Skills

Skills are recognized by Kimi when:

1. File is named `SKILL.md`
2. Located in `.agents/skills/<skill-name>/`
3. Frontmatter is valid (starts with `---`)
4. Has both `name` and `description` fields

## Best Practices

1. **Keep it concise** - Skills share context window with everything else
2. **Clear description** - The description determines when skill triggers
3. **Specific triggers** - Describe exact scenarios for skill usage
4. **Progressive disclosure** - Put detailed info in references/, keep
   SKILL.md focused
5. **Fix broken commands immediately** - If you discover a skill's command
   or example doesn't work, update the skill right away. Skills are living
   documents that must be kept accurate.
6. **Use `allowed-tools` sparingly** - Pre-approve tools only when the skill
   consistently needs them and the trade-off is worth reducing prompts
7. **Default to no pre-approval** - If a skill works fine without
   `allowed-tools`, leave the field out

## Discoverability Checklist

When updating a skill, verify that:

1. The `description` is specific enough to trigger on real user requests
2. The wording in `AGENTS.md` and the skill description describe the same
   routing signals
3. The body explains confusing boundaries with nearby skills
4. The skill avoids claiming files, tools, or workflows that no longer exist
5. Any `allowed-tools` entry is minimal, trusted, and justified by the skill
