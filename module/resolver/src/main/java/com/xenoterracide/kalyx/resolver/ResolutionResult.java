// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Immutable result of a dependency resolution operation.
 *
 * @param resolvedArtifacts the list of resolved artifacts
 * @param conflicts map of artifact key to conflicting versions
 * @param errors list of resolution errors
 */
public record ResolutionResult(
  List<ResolvedArtifact> resolvedArtifacts,
  Map<String, List<String>> conflicts,
  List<ResolutionError> errors
) {
  private static final String SEP = ":";

  /**
   * Creates a resolution result with validation.
   */
  @SuppressWarnings("Var")
  public ResolutionResult {
    Objects.requireNonNull(resolvedArtifacts, "resolvedArtifacts");
    Objects.requireNonNull(conflicts, "conflicts");
    Objects.requireNonNull(errors, "errors");
    resolvedArtifacts = List.copyOf(resolvedArtifacts);
    conflicts = Map.copyOf(conflicts);
    errors = List.copyOf(errors);
  }

  /**
   * Creates a successful resolution result.
   *
   * @param artifacts the resolved artifacts
   * @return a new ResolutionResult
   */
  public static ResolutionResult success(List<ResolvedArtifact> artifacts) {
    return new ResolutionResult(artifacts, Map.of(), List.of());
  }

  /**
   * Creates an empty resolution result.
   *
   * @return an empty result
   */
  public static ResolutionResult empty() {
    return new ResolutionResult(List.of(), Map.of(), List.of());
  }

  /**
   * Returns true if resolution completed without errors.
   *
   * @return true if successful
   */
  public boolean isSuccess() {
    return this.errors.isEmpty();
  }

  /**
   * Returns true if conflicts were resolved.
   *
   * @return true if conflicts exist
   */
  public boolean hasConflicts() {
    return !this.conflicts.isEmpty();
  }

  /**
   * Returns the classpath as JAR paths.
   *
   * @return list of JAR paths
   */
  public List<Path> classpath() {
    return this.resolvedArtifacts.stream()
      .map(ResolvedArtifact::jarPath)
      .flatMap(Optional::stream)
      .collect(Collectors.toList());
  }

  /**
   * Finds a resolved artifact by coordinates.
   *
   * @param group the group ID
   * @param artifact the artifact ID
   * @return optional resolved artifact
   */
  public Optional<ResolvedArtifact> findArtifact(String group, String artifact) {
    String key = group + SEP + artifact;
    return this.resolvedArtifacts.stream()
      .filter(a -> (a.group() + SEP + a.artifact()).equals(key))
      .findFirst();
  }

  /**
   * Represents a resolution error.
   *
   * @param coordinate the artifact that failed
   * @param message the error message
   * @param cause the underlying cause
   */
  public record ResolutionError(String coordinate, String message, Optional<Throwable> cause) {
    /**
     * Creates a resolution error.
     */
    public ResolutionError {
      Objects.requireNonNull(coordinate, "coordinate");
      Objects.requireNonNull(message, "message");
      Objects.requireNonNull(cause, "cause");
    }

    /**
     * Creates a resolution error without cause.
     *
     * @param coord the failed coordinate
     * @param msg the error message
     * @return a new ResolutionError
     */
    public static ResolutionError of(String coord, String msg) {
      return new ResolutionError(coord, msg, Optional.empty());
    }
  }
}
