// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.model;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    String coordinatePart = coordinate;
    String scope = DEFAULT_SCOPE;

    int atIndex = coordinate.lastIndexOf('@');
    if (atIndex != -1) {
      coordinatePart = coordinate.substring(0, atIndex);
      scope = coordinate.substring(atIndex + 1);
    }

    String[] parts = coordinatePart.split(":");
    if (parts.length != 3) {
      throw new IllegalArgumentException(
        "Invalid coordinate format: " + coordinate + ". Expected group:artifact:version[@scope]"
      );
    }

    return new Dependency(parts[0], parts[1], parts[2], scope);
  }
}
