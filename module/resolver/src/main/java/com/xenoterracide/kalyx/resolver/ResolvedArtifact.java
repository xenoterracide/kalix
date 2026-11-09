// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

import com.xenoterracide.kalyx.config.ArtifactCoordinate;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an artifact resolved from a Maven repository.
 *
 * @param coordinate the resolved artifact coordinates
 * @param jarPath the local path to the JAR file
 * @param source the repository source
 */
public record ResolvedArtifact(ArtifactCoordinate coordinate, Optional<Path> jarPath, String source) {
  /**
   * Creates a resolved artifact with validation.
   */
  public ResolvedArtifact {
    Objects.requireNonNull(coordinate, "coordinate");
    Objects.requireNonNull(jarPath, "jarPath");
    Objects.requireNonNull(source, "source");
  }

  /**
   * Creates an artifact without a JAR path.
   *
   * @param coord the artifact coordinates
   * @param src the repository source
   * @return a new ResolvedArtifact
   */
  public static ResolvedArtifact of(ArtifactCoordinate coord, String src) {
    return new ResolvedArtifact(coord, Optional.empty(), src);
  }

  /**
   * Creates an artifact with a JAR path.
   *
   * @param coord the artifact coordinates
   * @param path the local JAR file path
   * @param src the repository source
   * @return a new ResolvedArtifact
   */
  public static ResolvedArtifact withJar(ArtifactCoordinate coord, Path path, String src) {
    return new ResolvedArtifact(coord, Optional.of(path), src);
  }

  /**
   * Returns the group ID.
   *
   * @return the group ID
   */
  public String group() {
    return this.coordinate.group();
  }

  /**
   * Returns the artifact ID.
   *
   * @return the artifact ID
   */
  public String artifact() {
    return this.coordinate.artifact();
  }

  /**
   * Returns the version.
   *
   * @return the version
   */
  public String version() {
    return this.coordinate.version();
  }

  @Override
  public String toString() {
    return this.coordinate.toString() + " from " + this.source;
  }
}
