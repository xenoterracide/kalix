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

    int atIndex = dependency.lastIndexOf('@');
    if (atIndex == -1) {
      return new ScopedDependency(ArtifactCoordinate.parse(dependency));
    }

    String coordinatePart = dependency.substring(0, atIndex);
    String scopeName = dependency.substring(atIndex + 1);

    Scope scope = parseScope(scopeName);
    return new ScopedDependency(ArtifactCoordinate.parse(coordinatePart), scope);
  }

  private static Scope parseScope(String scopeName) {
    try {
      return Scope.valueOf(scopeName.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown scope: " + scopeName, e);
    }
  }
}
