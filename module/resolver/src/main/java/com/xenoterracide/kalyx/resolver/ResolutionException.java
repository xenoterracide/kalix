// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

/**
 * Exception thrown when dependency resolution fails.
 */
public class ResolutionException extends Exception {

  /**
   * Creates a resolution exception with a message.
   *
   * @param message the error message
   */
  public ResolutionException(String message) {
    super(message);
  }

  /**
   * Creates a resolution exception with message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public ResolutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
