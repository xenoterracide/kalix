// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

import com.xenoterracide.kalyx.config.ArtifactCoordinate;
import com.xenoterracide.kalyx.config.Scope;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Resolves Maven dependencies using "newest wins" conflict resolution.
 *
 * <p>This resolver implements Gradle-style dependency resolution:
 * <ol>
 *   <li>Build a dependency graph by traversing transitive dependencies</li>
 *   <li>When conflicts occur, select the newest version</li>
 *   <li>Report all conflicts for transparency</li>
 * </ol>
 */
public final class DependencyResolver implements AutoCloseable {

  private final MavenRepositoryClient repositoryClient;

  private final PomParser pomParser;

  private final VersionComparator versionComparator;

  /**
   * Creates a resolver using Maven Central.
   *
   * @param cacheDir the local cache directory
   * @throws ResolutionException if the POM parser cannot be created
   */
  public static DependencyResolver forMavenCentral(Path cacheDir) throws ResolutionException {
    return new DependencyResolver(MavenRepositoryClient.forMavenCentral(cacheDir));
  }

  /**
   * Creates a resolver with a custom repository client.
   *
   * @param repositoryClient the repository client
   * @throws ResolutionException if the POM parser cannot be created
   */
  public DependencyResolver(MavenRepositoryClient repositoryClient) throws ResolutionException {
    this.repositoryClient = Objects.requireNonNull(repositoryClient, "repositoryClient");
    this.pomParser = new PomParser();
    this.versionComparator = VersionComparator.INSTANCE;
  }

  /**
   * Resolves a list of dependencies with transitive closure.
   *
   * @param directDeps the direct project dependencies
   * @return the resolution result
   */
  public ResolutionResult resolve(List<ArtifactCoordinate> directDeps) {
    Objects.requireNonNull(directDeps, "directDeps");

    if (directDeps.isEmpty()) {
      return ResolutionResult.empty();
    }

    GraphResult graphResult = buildGraph(directDeps);
    ConflictResolution conflicts = resolveConflicts(graphResult.versionConflicts);
    List<ResolvedArtifact> resolved = downloadArtifacts(conflicts.selectedVersions, graphResult.artifacts);

    return new ResolutionResult(resolved, conflicts.conflictReport, graphResult.errors);
  }

  /**
   * Resolves a single dependency with transitive closure.
   *
   * @param coordinate the dependency to resolve
   * @return the resolution result
   */
  public ResolutionResult resolve(ArtifactCoordinate coordinate) {
    return resolve(List.of(coordinate));
  }

  /**
   * Builds the dependency graph.
   *
   * @param directDeps the direct dependencies
   * @return the graph result
   */
  GraphResult buildGraph(List<ArtifactCoordinate> directDeps) {
    Map<String, List<String>> versionConflicts = new HashMap<>();
    Map<String, Map<String, PomParser.PomModel>> artifacts = new HashMap<>();
    Queue<ResolutionRequest> queue = new ArrayDeque<>();
    Set<String> visited = new HashSet<>();
    List<ResolutionResult.ResolutionError> errors = new ArrayList<>();

    enqueueDirectDeps(directDeps, queue, visited);
    processQueue(queue, versionConflicts, artifacts, visited, errors);

    return new GraphResult(versionConflicts, artifacts, errors);
  }

  /**
   * Enqueues direct dependencies.
   *
   * @param directDeps the direct dependencies
   * @param queue the processing queue
   * @param visited the visited set
   */
  void enqueueDirectDeps(List<ArtifactCoordinate> directDeps, Queue<ResolutionRequest> queue, Set<String> visited) {
    for (ArtifactCoordinate coord : directDeps) {
      String key = coord.group() + ":" + coord.artifact();
      queue.add(new ResolutionRequest(coord, Scope.COMPILE, false));
      visited.add(key + ":" + coord.version());
    }
  }

  /**
   * Processes the resolution queue.
   *
   * @param queue the processing queue
   * @param versionConflicts the version conflicts map
   * @param artifacts the artifacts map
   * @param visited the visited set
   * @param errors the errors list
   */
  void processQueue(
    Queue<ResolutionRequest> queue,
    Map<String, List<String>> versionConflicts,
    Map<String, Map<String, PomParser.PomModel>> artifacts,
    Set<String> visited,
    List<ResolutionResult.ResolutionError> errors
  ) {
    while (!queue.isEmpty()) {
      ResolutionRequest request = queue.poll();
      processRequest(request, queue, versionConflicts, artifacts, visited, errors);
    }
  }

  /**
   * Processes a single resolution request.
   *
   * @param request the resolution request
   * @param queue the processing queue
   * @param versionConflicts the version conflicts map
   * @param artifacts the artifacts map
   * @param visited the visited set
   * @param errors the errors list
   */
  void processRequest(
    ResolutionRequest request,
    Queue<ResolutionRequest> queue,
    Map<String, List<String>> versionConflicts,
    Map<String, Map<String, PomParser.PomModel>> artifacts,
    Set<String> visited,
    List<ResolutionResult.ResolutionError> errors
  ) {
    ArtifactCoordinate coord = request.coordinate();
    String key = coord.group() + ":" + coord.artifact();

    versionConflicts.computeIfAbsent(key, k -> new ArrayList<>()).add(coord.version());

    PomParser.@Nullable PomModel pomModel = fetchAndParsePom(coord, errors);
    if (pomModel == null) {
      return;
    }

    artifacts.computeIfAbsent(key, k -> new HashMap<>()).put(coord.version(), pomModel);

    if (!pomModel.isPomOnly()) {
      enqueueTransitiveDeps(pomModel, queue, visited);
    }
  }

  /**
   * Enqueues transitive dependencies.
   *
   * @param pomModel the POM model
   * @param queue the processing queue
   * @param visited the visited set
   */
  void enqueueTransitiveDeps(PomParser.PomModel pomModel, Queue<ResolutionRequest> queue, Set<String> visited) {
    for (PomParser.PomDependency dep : pomModel.dependencies()) {
      if (!shouldIncludeTransitive(dep)) {
        continue;
      }

      String depVersion = resolveVersion(dep, pomModel.managedDependencies());
      if (depVersion == null) {
        continue;
      }

      String depKey = dep.groupId() + ":" + dep.artifactId();
      String depFullKey = depKey + ":" + depVersion;

      if (visited.contains(depFullKey)) {
        continue;
      }

      visited.add(depFullKey);
      ArtifactCoordinate depCoord = new ArtifactCoordinate(dep.groupId(), dep.artifactId(), depVersion);
      queue.add(new ResolutionRequest(depCoord, dep.scope(), dep.optional()));
    }
  }

  /**
   * Fetches and parses a POM file.
   *
   * @param coord the artifact coordinate
   * @param errors the errors list
   * @return the parsed POM model, or null if failed
   */
  PomParser.@Nullable PomModel fetchAndParsePom(
    ArtifactCoordinate coord,
    List<ResolutionResult.ResolutionError> errors
  ) {
    try {
      Path pomPath = this.repositoryClient.downloadPom(coord);
      return this.pomParser.parse(pomPath);
    } catch (ResolutionException e) {
      errors.add(ResolutionResult.ResolutionError.of(coord.toString(), "Failed to fetch/parse POM: " + e.getMessage()));
      return null;
    }
  }

  /**
   * Resolves a dependency version.
   *
   * @param dep the dependency
   * @param managedDeps the managed dependencies
   * @return the resolved version, or null if not found
   */
  static @Nullable String resolveVersion(PomParser.PomDependency dep, List<PomParser.PomDependency> managedDeps) {
    if (dep.hasVersion()) {
      return dep.version();
    }

    for (PomParser.PomDependency managed : managedDeps) {
      if (managed.groupId().equals(dep.groupId()) && managed.artifactId().equals(dep.artifactId())) {
        return managed.version();
      }
    }

    return null;
  }

  /**
   * Determines if a transitive dependency should be included.
   *
   * @param dep the dependency
   * @return true if should be included
   */
  static boolean shouldIncludeTransitive(PomParser.PomDependency dep) {
    if (dep.optional()) {
      return false;
    }
    return dep.scope() == Scope.COMPILE || dep.scope() == Scope.RUNTIME;
  }

  /**
   * Resolves version conflicts.
   *
   * @param versionConflicts the version conflicts map
   * @return the conflict resolution result
   */
  ConflictResolution resolveConflicts(Map<String, List<String>> versionConflicts) {
    Map<String, String> selectedVersions = new LinkedHashMap<>();
    Map<String, List<String>> conflictReport = new LinkedHashMap<>();

    for (Map.Entry<String, List<String>> entry : versionConflicts.entrySet()) {
      String key = entry.getKey();
      List<String> versions = entry.getValue();

      if (versions.size() > 1) {
        String selected = versions.stream().max(this.versionComparator).orElse(versions.get(0));
        selectedVersions.put(key, selected);
        conflictReport.put(key, List.copyOf(versions));
      } else if (!versions.isEmpty()) {
        selectedVersions.put(key, versions.get(0));
      }
    }

    return new ConflictResolution(selectedVersions, conflictReport);
  }

  /**
   * Downloads artifacts for selected versions.
   *
   * @param selectedVersions the selected versions map
   * @param artifacts the artifacts map
   * @return list of resolved artifacts
   */
  List<ResolvedArtifact> downloadArtifacts(
    Map<String, String> selectedVersions,
    Map<String, Map<String, PomParser.PomModel>> artifacts
  ) {
    List<ResolvedArtifact> resolved = new ArrayList<>();

    for (Map.Entry<String, String> entry : selectedVersions.entrySet()) {
      ResolvedArtifact artifact = downloadArtifact(entry.getKey(), entry.getValue(), artifacts);
      if (artifact != null) {
        resolved.add(artifact);
      }
    }

    return resolved;
  }

  /**
   * Downloads a single artifact.
   *
   * @param key the artifact key
   * @param version the version
   * @param artifacts the artifacts map
   * @return the resolved artifact, or null if failed
   */
  @Nullable
  ResolvedArtifact downloadArtifact(
    String key,
    String version,
    Map<String, Map<String, PomParser.PomModel>> artifacts
  ) {
    Map<String, PomParser.PomModel> versions = artifacts.get(key);
    if (versions == null) {
      return null;
    }

    PomParser.PomModel pomModel = versions.get(version);
    if (pomModel == null) {
      return null;
    }

    ArtifactCoordinate coord = pomModel.coordinate();
    Optional<Path> jarPath = downloadJarIfNeeded(pomModel);

    return new ResolvedArtifact(coord, jarPath, "Maven Central");
  }

  /**
   * Downloads JAR if needed.
   *
   * @param pomModel the POM model
   * @return optional JAR path
   */
  Optional<Path> downloadJarIfNeeded(PomParser.PomModel pomModel) {
    if (pomModel.isPomOnly()) {
      return Optional.empty();
    }

    try {
      return Optional.of(this.repositoryClient.downloadJar(pomModel.coordinate()));
    } catch (ResolutionException e) {
      return Optional.empty();
    }
  }

  @Override
  public void close() {
    this.repositoryClient.close();
  }

  /**
   * Resolution request record.
   *
   * @param coordinate the artifact coordinate
   * @param scope the scope
   * @param optional whether optional
   */
  record ResolutionRequest(ArtifactCoordinate coordinate, Scope scope, boolean optional) {}

  /**
   * Graph result record.
   *
   * @param versionConflicts the version conflicts map
   * @param artifacts the artifacts map
   * @param errors the errors list
   */
  record GraphResult(
    Map<String, List<String>> versionConflicts,
    Map<String, Map<String, PomParser.PomModel>> artifacts,
    List<ResolutionResult.ResolutionError> errors
  ) {}

  /**
   * Conflict resolution record.
   *
   * @param selectedVersions the selected versions map
   * @param conflictReport the conflict report map
   */
  record ConflictResolution(Map<String, String> selectedVersions, Map<String, List<String>> conflictReport) {}
}
