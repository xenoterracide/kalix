// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.config;

import java.util.List;
import java.util.Objects;

/**
 * Root configuration for a Kalyx project.
 *
 * @param dependencies List of project dependencies (immutable)
 */
public record KalyxConfig(List<ScopedDependency> dependencies) {
  /**
   * Creates a new KalyxConfig with validation.
   *
   * @param dependencies the dependencies
   */
  public KalyxConfig {
    Objects.requireNonNull(dependencies, "dependencies");
  }

  /**
   * Creates an empty configuration.
   *
   * @return empty config
   */
  public static KalyxConfig empty() {
    return new KalyxConfig(List.of());
  }
}
