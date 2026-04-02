// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.model;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * A dependency coordinate in the form group:artifact:version[@scope].
 *
 * @param group the group ID
 * @param artifact the artifact ID
 * @param version the version
 * @param scope the scope (compile, test, etc.), defaults to "compile"
 */
public record Dependency(
  @NonNull String group,
  @NonNull String artifact,
  @NonNull String version,
  @NonNull String scope
) {
  private static final String DEFAULT_SCOPE = "compile";

  /**
   * Creates a Dependency with the default scope.
   *
   * @param group the group ID
   * @param artifact the artifact ID
   * @param version the version
   */
  public Dependency(String group, String artifact, String version) {
    this(group, artifact, version, DEFAULT_SCOPE);
  }

  /**
   * Parses a dependency coordinate string.
   * Formats: "group:artifact:version" or "group:artifact:version@scope"
   *
   * @param coordinate the coordinate string
   * @return the parsed Dependency
   * @throws IllegalArgumentException if the format is invalid
   */
  public static @NonNull Dependency parse(@NonNull String coordinate) {
    Objects.requireNonNull(coordinate, "coordinate");

    var result = extractScope(coordinate);
    String[] parts = parseParts(result.coordinatePart(), coordinate);

    return new Dependency(parts[0], parts[1], parts[2], result.scope());
  }

  private static ScopeResult extractScope(String coordinate) {
    int atIndex = coordinate.lastIndexOf('@');
    if (atIndex == -1) {
      return new ScopeResult(coordinate, DEFAULT_SCOPE);
    }
    return new ScopeResult(coordinate.substring(0, atIndex), coordinate.substring(atIndex + 1));
  }

  private static String[] parseParts(String coordinatePart, String original) {
    int firstColon = coordinatePart.indexOf(':');
    int secondColon = coordinatePart.indexOf(':', firstColon + 1);

    if (firstColon == -1 || secondColon == -1 || coordinatePart.indexOf(':', secondColon + 1) != -1) {
      throw new IllegalArgumentException(
        "Invalid coordinate format: " + original + ". Expected group:artifact:version[@scope]"
      );
    }

    return new String[] {
      coordinatePart.substring(0, firstColon),
      coordinatePart.substring(firstColon + 1, secondColon),
      coordinatePart.substring(secondColon + 1),
    };
  }

  private record ScopeResult(String coordinatePart, String scope) {}
}
