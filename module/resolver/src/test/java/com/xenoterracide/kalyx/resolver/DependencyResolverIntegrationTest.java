// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.xenoterracide.kalyx.config.ArtifactCoordinate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for {@link DependencyResolver} against Maven Central.
 *
 * <p>These tests require network access to Maven Central.
 */
class DependencyResolverIntegrationTest {

  @TempDir
  Path tempDir;

  private DependencyResolver resolver;
  private Path cacheDir;

  @BeforeEach
  void setUp() throws ResolutionException {
    cacheDir = tempDir.resolve("cache");
    resolver = DependencyResolver.forMavenCentral(cacheDir);
  }

  @AfterEach
  void tearDown() {
    resolver.close();
  }

  @Test
  void shouldResolveSimpleArtifact() {
    var coordinate = ArtifactCoordinate.parse("org.slf4j:slf4j-api:2.0.16");

    ResolutionResult result = resolver.resolve(coordinate);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.resolvedArtifacts()).hasSize(1);

    ResolvedArtifact artifact = result.resolvedArtifacts().get(0);
    assertThat(artifact.coordinate()).isEqualTo(coordinate);
    assertThat(artifact.jarPath()).isPresent();
    assertThat(Files.exists(artifact.jarPath().get())).isTrue();
  }

  @Test
  void shouldResolveWithTransitiveDependencies() {
    // JUnit Jupiter API has transitive dependencies
    var coordinate = ArtifactCoordinate.parse("org.junit.jupiter:junit-jupiter-api:5.12.0");

    ResolutionResult result = resolver.resolve(coordinate);

    assertThat(result.isSuccess()).isTrue();
    // Should have junit-jupiter-api + its transitive deps (junit-platform-commons, opentest4j, etc.)
    assertThat(result.resolvedArtifacts()).hasSizeGreaterThanOrEqualTo(3);

    // Verify the main artifact is present
    assertThat(result.findArtifact("org.junit.jupiter", "junit-jupiter-api"))
      .isPresent()
      .hasValueSatisfying(a -> assertThat(a.version()).isEqualTo("5.12.0"));

    // Verify transitive dependencies were resolved
    assertThat(result.findArtifact("org.junit.platform", "junit-platform-commons")).isPresent();
    assertThat(result.findArtifact("org.opentest4j", "opentest4j")).isPresent();
  }

  @Test
  void shouldResolveVersionConflictWithNewestWins() {
    // Create a scenario with conflicting versions
    // Using artifacts that depend on different versions of the same library

    // Start with an older version
    var older = ArtifactCoordinate.parse("org.slf4j:slf4j-api:1.7.36");
    // Then add a newer version
    var newer = ArtifactCoordinate.parse("org.slf4j:slf4j-api:2.0.16");

    ResolutionResult result = resolver.resolve(List.of(older, newer));

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.hasConflicts()).isTrue();

    // Should have conflict report for slf4j-api
    assertThat(result.conflicts()).containsKey("org.slf4j:slf4j-api");
    assertThat(result.conflicts().get("org.slf4j:slf4j-api")).contains("1.7.36", "2.0.16");

    // Newest version should win
    assertThat(result.findArtifact("org.slf4j", "slf4j-api"))
      .isPresent()
      .hasValueSatisfying(a -> assertThat(a.version()).isEqualTo("2.0.16"));
  }

  @Test
  void shouldCacheDownloadedArtifacts() throws ResolutionException {
    var coordinate = ArtifactCoordinate.parse("org.slf4j:slf4j-api:2.0.16");

    // First resolution - should download
    ResolutionResult result1 = resolver.resolve(coordinate);
    assertThat(result1.isSuccess()).isTrue();

    Path jarPath = result1.resolvedArtifacts().get(0).jarPath().orElseThrow();
    assertThat(Files.exists(jarPath)).isTrue();

    // Close and create new resolver - should use cached files
    resolver.close();

    try (var resolver2 = DependencyResolver.forMavenCentral(cacheDir)) {
      ResolutionResult result2 = resolver2.resolve(coordinate);

      assertThat(result2.isSuccess()).isTrue();
      assertThat(result2.resolvedArtifacts().get(0).jarPath()).hasValue(jarPath); // Same path, cached
    }
  }

  @Test
  void shouldResolveMultipleDirectDependencies() {
    var deps = List.of(
      ArtifactCoordinate.parse("org.slf4j:slf4j-api:2.0.16"),
      ArtifactCoordinate.parse("org.assertj:assertj-core:3.27.3")
    );

    ResolutionResult result = resolver.resolve(deps);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.resolvedArtifacts()).hasSizeGreaterThanOrEqualTo(2);

    assertThat(result.findArtifact("org.slf4j", "slf4j-api")).isPresent();
    assertThat(result.findArtifact("org.assertj", "assertj-core")).isPresent();
  }

  @Test
  void shouldBuildClasspathFromResolvedArtifacts() {
    var coordinate = ArtifactCoordinate.parse("org.junit.jupiter:junit-jupiter-api:5.12.0");

    ResolutionResult result = resolver.resolve(coordinate);

    List<Path> classpath = result.classpath();

    assertThat(classpath).isNotEmpty();
    // All paths should exist
    assertThat(classpath).allMatch(Files::exists);
    // All should be JAR files
    assertThat(classpath).allMatch(p -> p.toString().endsWith(".jar"));
  }

  @Test
  void shouldResolveGuavaWithManyTransitives() {
    // Guava has many transitive dependencies - good stress test
    var coordinate = ArtifactCoordinate.parse("com.google.guava:guava:33.0.0-jre");

    ResolutionResult result = resolver.resolve(coordinate);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.resolvedArtifacts()).hasSizeGreaterThan(5);

    // Verify key transitive deps
    assertThat(result.findArtifact("com.google.guava", "failureaccess")).isPresent();
    assertThat(result.findArtifact("com.google.errorprone", "error_prone_annotations")).isPresent();
  }
}
