// SPDX-FileCopyrightText: 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ScopedDependencyTest {

  @Test
  void parseWithDefaultScope() {
    var dep = ScopedDependency.parse("org.junit:junit:4.13.2");

    assertThat(dep.coordinate().group()).isEqualTo("org.junit");
    assertThat(dep.scope()).isEqualTo(Scope.COMPILE);
  }

  @Test
  void parseWithExplicitScope() {
    var dep = ScopedDependency.parse("org.junit:junit:4.13.2@test");

    assertThat(dep.coordinate().artifact()).isEqualTo("junit");
    assertThat(dep.scope()).isEqualTo(Scope.TEST);
  }

  @Test
  void parseWithCompileOnlyScope() {
    var dep = ScopedDependency.parse("some:lib:1.0@compile_only");

    assertThat(dep.scope()).isEqualTo(Scope.COMPILE_ONLY);
  }

  @Test
  void defaultScopeIsCompile() {
    var coord = new ArtifactCoordinate("org.junit", "junit", "4.13.2");
    var dep = new ScopedDependency(coord);

    assertThat(dep.scope()).isEqualTo(Scope.COMPILE);
  }

  @Test
  void constructorRejectsNullCoordinate() {
    assertThatThrownBy(() -> new ScopedDependency(null, Scope.TEST)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructorRejectsNullScope() {
    var coord = new ArtifactCoordinate("org.junit", "junit", "4.13.2");
    assertThatThrownBy(() -> new ScopedDependency(coord, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void parseNullThrows() {
    assertThatThrownBy(() -> ScopedDependency.parse(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void parseUnknownScopeThrows() {
    assertThatThrownBy(() -> ScopedDependency.parse("org.junit:junit:4.13.2@unknown"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unknown scope");
  }
}
