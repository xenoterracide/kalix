# Elaboration Milestones

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

> Work-in-progress breakdown. Not a commitment, just a thinking tool.

## Milestone 0: Config Parser (Day 1-2)

**Goal:** Parse a minimal `kalix.yaml`

```yaml
dependencies:
  - org.junit:junit:4.13.2
```

**Done when:**

- [ ] Can parse YAML to Java objects
- [ ] Extracts dependency coordinates
- [ ] Test: parse → print coordinates

**Explicitly NOT included:**

- Version resolution
- Scope handling
- Multiple dependencies
- Validation

---

## Milestone 1: Download One JAR (Day 3-5)

**Goal:** Download a hardcoded JAR from Maven Central

**Done when:**

- [ ] Construct Maven Central URL from coordinates
- [ ] Download to `~/.kalyx/cache/`
- [ ] Test: run → verify file exists

**Explicitly NOT included:**

- Transitive dependencies
- Lock files
- Authentication
- Parallel downloads

---

## Milestone 2: Compile One File (Day 6-10)

**Goal:** Compile a single Java file with downloaded JAR

```
src/main/java/App.java
  ↓
klx build
  ↓
build/classes/App.class
```

**Done when:**

- [ ] Find `src/main/java/*.java`
- [ ] Call `javac` with JAR on classpath
- [ ] Produce `.class` file
- [ ] Test: compile → run `java App`

**Explicitly NOT included:**

- Multiple source files
- Resources
- Annotation processors
- Error handling

---

## Milestone 3: Run Tests (Day 11-15)

**Goal:** Run JUnit tests

**Done when:**

- [ ] Download JUnit + dependencies (still hardcoded)
- [ ] Compile test files
- [ ] Run JUnit, report pass/fail
- [ ] Exit code reflects test result

**Explicitly NOT included:**

- Test discovery
- Parallel execution
- Test reports
- Gradle-style test output

---

## Milestone 4: Package JAR (Day 16-18)

**Goal:** Create a JAR file

**Done when:**

- [ ] Package `.class` files into JAR
- [ ] Include manifest (basic)
- [ ] Test: `java -jar build/libs/app.jar` works

**Explicitly NOT included:**

- Manifest customization
- Multi-release JARs
- Signing
- Reproducible builds

---

## Milestone 5: Incremental Build (Day 19-21)

**Goal:** Skip compilation if up-to-date

**Done when:**

- [ ] Compare `.java` timestamp vs `.class` timestamp
- [ ] Skip `javac` if nothing changed
- [ ] Test: touch file → recompiles

**Explicitly NOT included:**

- Content hashing
- Annotation processor awareness
- Parallel compilation
- Build cache

---

## Summary

| Milestone        | Days | Core Skill           |
| ---------------- | ---- | -------------------- |
| 0. Config Parser | 2    | YAML parsing         |
| 1. Download JAR  | 3    | HTTP + caching       |
| 2. Compile       | 5    | Process spawning     |
| 3. Test          | 5    | JUnit integration    |
| 4. Package       | 3    | ZIP/JAR creation     |
| 5. Incremental   | 3    | Timestamp comparison |

**Total: 21 days** (one 3-week iteration)

Each milestone is a "first PR" sized chunk. Merge, then move to next.
