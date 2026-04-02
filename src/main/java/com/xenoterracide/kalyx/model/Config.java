// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.model;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Root configuration model for kalyx.yaml.
 *
 * @param dependencies List of project dependencies
 */
public record Config(@NonNull List<@NonNull Dependency> dependencies) {
  /**
   * Creates a new Config with validation.
   *
   * @param dependencies the dependencies
   */
  public Config {
    dependencies = List.copyOf(dependencies);
  }
}
