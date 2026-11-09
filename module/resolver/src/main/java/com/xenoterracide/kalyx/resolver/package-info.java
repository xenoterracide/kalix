// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

/**
 * Dependency resolution from Maven repositories.
 *
 * <p>This package provides tools for resolving Maven dependencies using the
 * "newest wins" conflict resolution strategy. It supports:
 *
 * <ul>
 *   <li>Downloading artifacts from Maven repositories</li>
 *   <li>Parsing Maven POM files</li>
 *   <li>Building dependency graphs with transitive dependencies</li>
 *   <li>Resolving version conflicts by selecting newest versions</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * Path cacheDir = Path.of(System.getProperty("user.home"), ".kalyx", "cache");
 * try (var resolver = DependencyResolver.forMavenCentral(cacheDir)) {
 *     var coordinate = new ArtifactCoordinate("org.slf4j", "slf4j-api", "2.0.16");
 *     ResolutionResult result = resolver.resolve(coordinate);
 *
 *     if (result.isSuccess()) {
 *         for (ResolvedArtifact artifact : result.resolvedArtifacts()) {
 *             System.out.println(artifact);
 *         }
 *     }
 * }
 * }</pre>
 */
@NullMarked
package com.xenoterracide.kalyx.resolver;

import org.jspecify.annotations.NullMarked;
