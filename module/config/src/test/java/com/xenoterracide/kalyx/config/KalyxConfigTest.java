// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KalyxConfigTest {

  @Test
  void createsConfigWithDependencies() {
    var deps = List.of(
      ScopedDependency.parse("org.junit:junit:4.13.2@test"),
      ScopedDependency.parse("com.google.guava:guava:33.0.0-jre")
    );
    var config = new KalyxConfig(deps);

    assertThat(config.dependencies())
      .hasSize(2)
      .first()
      .extracting(d -> d.coordinate().artifact())
      .isEqualTo("junit");
  }

  @Test
  void emptyConfigHasNoDependencies() {
    var config = KalyxConfig.empty();

    assertThat(config.dependencies()).isEmpty();
  }

  @Test
  void dependenciesListIsImmutable() {
    var config = new KalyxConfig(List.of(ScopedDependency.parse("a:b:1")));

    assertThat(config.dependencies()).isUnmodifiable();
  }
}
