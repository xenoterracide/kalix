// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.config;

import java.util.Objects;

/**
 * A Maven artifact coordinate in the form group:artifact:version.
 * This is a value object representing the GAV coordinates only.
 *
 * @param group the group ID
 * @param artifact the artifact ID
 * @param version the version
 */
public record ArtifactCoordinate(String group, String artifact, String version) {
  private static final String DELIMITER = ":";

  /**
   * Creates an ArtifactCoordinate with validation.
   *
   * @param group the group ID
   * @param artifact the artifact ID
   * @param version the version
   */
  public ArtifactCoordinate {
    Objects.requireNonNull(group, "group");
    Objects.requireNonNull(artifact, "artifact");
    Objects.requireNonNull(version, "version");
  }

  /**
   * Parses a coordinate string in the format "group:artifact:version".
   *
   * @param coordinate the coordinate string
   * @return the parsed ArtifactCoordinate
   * @throws IllegalArgumentException if the format is invalid
   */
  public static ArtifactCoordinate parse(String coordinate) {
    Objects.requireNonNull(coordinate, "coordinate");

    int firstColon = coordinate.indexOf(':');
    int secondColon = coordinate.indexOf(':', firstColon + 1);

    if (firstColon == -1 || secondColon == -1 || coordinate.indexOf(':', secondColon + 1) != -1) {
      throw new IllegalArgumentException(
        "Invalid coordinate format: " + coordinate + ". Expected group:artifact:version"
      );
    }

    var grp = coordinate.substring(0, firstColon);
    var art = coordinate.substring(firstColon + 1, secondColon);
    var ver = coordinate.substring(secondColon + 1);

    return new ArtifactCoordinate(grp, art, ver);
  }

  /**
   * Returns the coordinate in the format "group:artifact:version".
   *
   * @return the coordinate string
   */
  @Override
  public String toString() {
    return this.group + DELIMITER + this.artifact + DELIMITER + this.version;
  }
}
