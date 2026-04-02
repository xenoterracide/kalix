// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigTest {

  @Test
  void createsConfigWithDependencies() {
    var deps = List.of(
      new Dependency("org.junit", "junit", "4.13.2"),
      new Dependency("com.google.guava", "guava", "33.0.0-jre")
    );
    var config = new Config(deps);

    assertThat(config.dependencies()).hasSize(2);
    assertThat(config.dependencies().get(0).artifact()).isEqualTo("junit");
  }

  @Test
  void createsImmutableDependenciesList() {
    var deps = List.of(new Dependency("org.junit", "junit", "4.13.2"));
    var config = new Config(deps);

    // List should be immutable (List.copyOf in constructor)
    assertThat(config.dependencies()).isUnmodifiable();
  }

  @Test
  void createsEmptyConfig() {
    var config = new Config(List.of());

    assertThat(config.dependencies()).isEmpty();
  }
}
