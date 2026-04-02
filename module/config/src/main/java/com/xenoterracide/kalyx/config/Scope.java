// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.config;

/**
 * Dependency scope enumeration.
 * Defines the classpath visibility and transitive behavior of dependencies.
 */
public enum Scope {
  /**
   * Available in all classpaths, transitive.
   * Default scope if none specified.
   */
  COMPILE,

  /**
   * Available at runtime only, transitive.
   */
  RUNTIME,

  /**
   * Available in test classpath only, not transitive.
   */
  TEST,

  /**
   * Available at compile time only, not transitive.
   */
  PROVIDED,

  /**
   * Alias for PROVIDED, more explicit naming.
   */
  COMPILE_ONLY,

  /**
   * Alias for RUNTIME, more explicit naming.
   */
  RUNTIME_ONLY,
}
