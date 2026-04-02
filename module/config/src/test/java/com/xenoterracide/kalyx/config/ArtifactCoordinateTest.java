// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ArtifactCoordinateTest {

  @Test
  void parseValidCoordinate() {
    var coord = ArtifactCoordinate.parse("org.junit:junit:4.13.2");

    assertThat(coord.group()).isEqualTo("org.junit");
    assertThat(coord.artifact()).isEqualTo("junit");
    assertThat(coord.version()).isEqualTo("4.13.2");
  }

  @Test
  void toStringReturnsCoordinate() {
    var coord = new ArtifactCoordinate("org.junit", "junit", "4.13.2");

    assertThat(coord.toString()).isEqualTo("org.junit:junit:4.13.2");
  }

  @Test
  void parseTooManyColonsThrows() {
    assertThatThrownBy(() -> ArtifactCoordinate.parse("org.junit:junit:4.13.2:extra"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid coordinate format");
  }

  @Test
  void parseTooFewColonsThrows() {
    assertThatThrownBy(() -> ArtifactCoordinate.parse("org.junit:junit"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid coordinate format");
  }

  @Test
  void parseNullThrows() {
    assertThatThrownBy(() -> ArtifactCoordinate.parse(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructorRejectsNullGroup() {
    assertThatThrownBy(() -> new ArtifactCoordinate(null, "junit", "4.13.2")).isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructorRejectsNullArtifact() {
    assertThatThrownBy(() -> new ArtifactCoordinate("org.junit", null, "4.13.2")).isInstanceOf(
      NullPointerException.class
    );
  }

  @Test
  void constructorRejectsNullVersion() {
    assertThatThrownBy(() -> new ArtifactCoordinate("org.junit", "junit", null)).isInstanceOf(
      NullPointerException.class
    );
  }
}
