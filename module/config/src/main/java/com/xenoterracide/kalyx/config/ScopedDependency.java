// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.config;

import java.util.Locale;
import java.util.Objects;

/**
 * A dependency with its associated scope.
 * Combines an {@link ArtifactCoordinate} with a {@link Scope}.
 *
 * @param coordinate the artifact coordinate (GAV)
 * @param scope the scope, defaults to {@link Scope#COMPILE}
 */
public record ScopedDependency(ArtifactCoordinate coordinate, Scope scope) {
  private static final String SCOPE_SEPARATOR = "@";

  /**
   * Creates a ScopedDependency with default COMPILE scope.
   *
   * @param coordinate the artifact coordinate
   */
  public ScopedDependency(ArtifactCoordinate coordinate) {
    this(coordinate, Scope.COMPILE);
  }

  /**
   * Creates a ScopedDependency with validation.
   *
   * @param coordinate the artifact coordinate
   * @param scope the scope
   */
  public ScopedDependency {
    Objects.requireNonNull(coordinate, "coordinate");
    Objects.requireNonNull(scope, "scope");
  }

  /**
   * Parses a dependency string in the format "group:artifact:version[@scope]".
   *
   * @param dependency the dependency string
   * @return the parsed ScopedDependency
   * @throws IllegalArgumentException if the format is invalid
   */
  public static ScopedDependency parse(String dependency) {
    Objects.requireNonNull(dependency, "dependency");

    var parts = dependency.split(SCOPE_SEPARATOR, 2);
    if (parts.length == 1) {
      return new ScopedDependency(ArtifactCoordinate.parse(parts[0]));
    }

    var scope = parseScope(parts[1]);
    return new ScopedDependency(ArtifactCoordinate.parse(parts[0]), scope);
  }

  private static Scope parseScope(String scopeName) {
    try {
      return Scope.valueOf(scopeName.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown scope: " + scopeName, e);
    }
  }
}
