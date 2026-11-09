<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

# Dependency Resolution Spike (A1)

## Summary

This document validates **Assumption A1**: that we can study and reimplement Gradle's "newest wins" dependency resolution algorithm.

## Implementation

Created `module/resolver` with a working dependency resolver that:

1. **Downloads artifacts from Maven Central** via `MavenRepositoryClient`
2. **Parses Maven POM files** via `PomParser` (handles properties, parent POMs, dependency management)
3. **Builds dependency graphs** with transitive dependency traversal
4. **Resolves conflicts using "newest wins"** via `VersionComparator`
5. **Reports conflicts** for transparency

## Key Design Decisions

### Version Comparison

Implements Maven's version ordering:

- Numeric comparison (1.10 > 1.2)
- Qualifier ordering: alpha < beta < milestone < rc < snapshot < release < sp
- Case-insensitive qualifier matching

### Conflict Resolution

When the same artifact appears with different versions:

1. Collect all requested versions during graph traversal
2. Select the newest using Maven-compatible version comparison
3. Report the conflict with all versions considered

### Caching

- POM and JAR files cached locally using Maven repository layout
- Content-addressed by coordinate
- Cache hits skip network download

## Test Results

### Unit Tests

- ✅ `VersionComparatorTest` - Version comparison edge cases
- ✅ `PomParserTest` - POM parsing with parent inheritance, properties, dependency management

### Integration Tests (in `DependencyResolverIntegrationTest`)

- Resolve simple artifact (SLF4J API)
- Resolve with transitive dependencies (JUnit Jupiter)
- Version conflict resolution (Guava 32.0 vs 33.0)
- Caching behavior verification

## Risks Validated

| Risk                                | Status        | Notes                                              |
| ----------------------------------- | ------------- | -------------------------------------------------- |
| Maven version comparison complexity | ✅ Mitigated  | Implemented per Maven spec, tested edge cases      |
| Parent POM chains                   | ✅ Acceptable | Limited depth, works for typical cases             |
| Circular dependencies               | ⚠️ Partial    | Not explicitly handled, BFS traversal limits depth |
| Network failures                    | ✅ Mitigated  | Errors collected, resolution continues             |

## Out of Scope (for spike)

- Lock file generation
- Gradle metadata (.module files)
- Authentication for private repositories
- Parallel downloads
- BOM imports
- Version ranges

## Conclusion

**Assumption A1 is VALIDATED.** We can successfully:

1. Download and parse Maven artifacts
2. Build dependency graphs with transitive resolution
3. Implement "newest wins" conflict resolution
4. Cache artifacts locally

The implementation is ready for the **vertical slice** milestone (single Java file project with JUnit dependency).
