// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DependencyTest {

  @Test
  void parseSimpleCoordinate() {
    var dep = Dependency.parse("org.junit:junit:4.13.2");

    assertThat(dep.group()).isEqualTo("org.junit");
    assertThat(dep.artifact()).isEqualTo("junit");
    assertThat(dep.version()).isEqualTo("4.13.2");
    assertThat(dep.scope()).isEqualTo("compile");
  }

  @Test
  void parseCoordinateWithScope() {
    var dep = Dependency.parse("org.junit:junit:4.13.2@test");

    assertThat(dep.group()).isEqualTo("org.junit");
    assertThat(dep.artifact()).isEqualTo("junit");
    assertThat(dep.version()).isEqualTo("4.13.2");
    assertThat(dep.scope()).isEqualTo("test");
  }

  @Test
  void parseInvalidCoordinateThrows() {
    assertThatThrownBy(() -> Dependency.parse("invalid"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid coordinate format");
  }

  @Test
  void parseNullCoordinateThrows() {
    assertThatThrownBy(() -> Dependency.parse(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void parseCoordinateWithTooManyColonsThrows() {
    assertThatThrownBy(() -> Dependency.parse("org.junit:junit:4.13.2:extra"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid coordinate format");
  }

  @Test
  void parseCoordinateWithTooFewColonsThrows() {
    assertThatThrownBy(() -> Dependency.parse("org.junit:junit"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid coordinate format");
  }

  @Test
  void createsDependencyWithExplicitScope() {
    var dep = new Dependency("org.junit", "junit", "4.13.2", "test");

    assertThat(dep.group()).isEqualTo("org.junit");
    assertThat(dep.artifact()).isEqualTo("junit");
    assertThat(dep.version()).isEqualTo("4.13.2");
    assertThat(dep.scope()).isEqualTo("test");
  }

  @Test
  void defaultScopeIsCompile() {
    var dep = new Dependency("org.junit", "junit", "4.13.2");

    assertThat(dep.scope()).isEqualTo("compile");
  }
}
