// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link VersionComparator}.
 */
class VersionComparatorTest {

  private final VersionComparator comparator = VersionComparator.INSTANCE;

  @ParameterizedTest(name = "{0} < {1}")
  @CsvSource(
    {
      "1.0, 1.1",
      "1.0, 2.0",
      "1.0.0, 1.0.1",
      "1.0-alpha, 1.0",
      "1.0-SNAPSHOT, 1.0",
      "1.0-rc1, 1.0",
      "1.0-beta, 1.0-rc1",
      "1.0-alpha, 1.0-beta",
      "1.0-SNAPSHOT, 1.0-release",
      "2.0, 10.0",
      "1.0.0, 1.0.0.1",
    }
  )
  void shouldCompareLessThan(String v1, String v2) {
    assertThat(this.comparator.compare(v1, v2)).as("Expected %s < %s", v1, v2).isNegative();
    assertThat(this.comparator.compare(v2, v1)).as("Expected %s > %s", v2, v1).isPositive();
  }

  @ParameterizedTest(name = "{0} = {1}")
  @CsvSource({ "1.0, 1.0", "1.0.0, 1.0.0", "1.0-SNAPSHOT, 1.0-snapshot", "1.0-RC1, 1.0-rc1" })
  void shouldCompareEqual(String v1, String v2) {
    assertThat(this.comparator.compare(v1, v2)).as("Expected %s = %s", v1, v2).isZero();
  }

  @Test
  void shouldHandleNumericComparison() {
    assertThat(this.comparator.compare("1.10", "1.2")).isPositive();
    assertThat(this.comparator.compare("1.2", "1.10")).isNegative();
  }

  @Test
  void shouldSortVersionList() {
    var versions = Stream.of("1.0", "1.10", "1.1", "1.0-SNAPSHOT", "1.0-rc1", "1.0-beta")
      .sorted(this.comparator)
      .toList();

    assertThat(versions).containsExactly("1.0-beta", "1.0-rc1", "1.0-SNAPSHOT", "1.0", "1.1", "1.10");
  }

  @Test
  void shouldHandleRealWorldVersions() {
    assertThat(this.comparator.compare("1.7.36", "2.0.0")).isNegative();
    assertThat(this.comparator.compare("2.0.9", "2.0.16")).isNegative();
    assertThat(this.comparator.compare("5.9.0", "5.10.0")).isNegative();
    assertThat(this.comparator.compare("5.10.0-M1", "5.10.0")).isNegative();
    assertThat(this.comparator.compare("5.10.0-RC1", "5.10.0")).isNegative();
    assertThat(this.comparator.compare("32.0.0-jre", "33.0.0-jre")).isNegative();
  }

  @Test
  void newestVersionShouldWinInConflict() {
    var versions = Stream.of("1.0", "1.5", "1.10", "1.2").max(this.comparator);

    assertThat(versions).hasValue("1.10");
  }
}
