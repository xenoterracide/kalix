// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

import com.xenoterracide.kalyx.config.ArtifactCoordinate;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Objects;

/**
 * Client for downloading artifacts from Maven repositories.
 *
 * <p>Supports HTTP operations to fetch POM and JAR files from Maven Central
 * or other Maven-compatible repositories.
 */
public final class MavenRepositoryClient implements AutoCloseable {

  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private static final String POM_EXT = ".pom";

  private static final String JAR_EXT = ".jar";

  private static final String DELIM = ":";

  private final HttpClient httpClient;

  private String baseUrl;

  private Path cacheDir;

  /**
   * Creates a client for Maven Central.
   *
   * @param cacheDir the local directory to cache downloaded artifacts
   * @return a new MavenRepositoryClient
   */
  public static MavenRepositoryClient forMavenCentral(Path cacheDir) {
    return new MavenRepositoryClient("https://repo.maven.apache.org/maven2", cacheDir);
  }

  /**
   * Creates a client for a custom Maven repository.
   *
   * @param baseUrl the repository base URL
   * @param cacheDir the local directory to cache downloaded artifacts
   */
  public MavenRepositoryClient(String baseUrl, Path cacheDir) {
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl").replaceAll("/$", "");
    this.cacheDir = Objects.requireNonNull(cacheDir, "cacheDir");
    this.httpClient = HttpClient.newBuilder()
      .connectTimeout(TIMEOUT)
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

    try {
      Files.createDirectories(cacheDir);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to create cache directory: " + cacheDir, e);
    }
  }

  /**
   * Downloads the POM file for an artifact.
   *
   * @param coordinate the artifact coordinates
   * @return the path to the local POM file
   * @throws ResolutionException if download fails
   */
  public Path downloadPom(ArtifactCoordinate coordinate) throws ResolutionException {
    Objects.requireNonNull(coordinate, "pomCoordinate");

    Path cached = this.getCachedPath(coordinate, POM_EXT);
    if (Files.exists(cached)) {
      return cached;
    }

    String url = this.buildArtifactUrl(coordinate, POM_EXT);
    return this.download(url, cached);
  }

  /**
   * Downloads the JAR file for an artifact.
   *
   * @param coordinate the artifact coordinates
   * @return the path to the local JAR file
   * @throws ResolutionException if download fails
   */
  public Path downloadJar(ArtifactCoordinate coordinate) throws ResolutionException {
    Objects.requireNonNull(coordinate, "jarCoordinate");

    Path cached = this.getCachedPath(coordinate, JAR_EXT);
    if (Files.exists(cached)) {
      return cached;
    }

    String url = this.buildArtifactUrl(coordinate, JAR_EXT);
    return this.download(url, cached);
  }

  /**
   * Checks if an artifact exists without downloading.
   *
   * @param coordinate the artifact coordinates
   * @return true if the artifact exists
   */
  public boolean exists(ArtifactCoordinate coordinate) {
    try {
      String url = this.buildArtifactUrl(coordinate, POM_EXT);
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .method("HEAD", HttpRequest.BodyPublishers.noBody())
        .timeout(TIMEOUT)
        .build();

      HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      return response.statusCode() == 200;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  @Override
  public void close() {
    // HttpClient doesn't need explicit closing
  }

  /**
   * Builds the URL for an artifact file.
   *
   * @param coordinate the artifact coordinate
   * @param extension the file extension
   * @return the URL string
   */
  private String buildArtifactUrl(ArtifactCoordinate coordinate, String extension) {
    String groupPath = coordinate.group().replace('.', '/');
    return String.format(
      "%s/%s/%s/%s/%s-%s%s",
      this.baseUrl,
      groupPath,
      coordinate.artifact(),
      coordinate.version(),
      coordinate.artifact(),
      coordinate.version(),
      extension
    );
  }

  /**
   * Gets the local cache path for an artifact.
   *
   * @param coordinate the artifact coordinate
   * @param extension the file extension
   * @return the cache path
   */
  private Path getCachedPath(ArtifactCoordinate coordinate, String extension) {
    String groupPath = coordinate.group().replace('.', '/');
    Path dir = this.cacheDir.resolve(groupPath).resolve(coordinate.artifact()).resolve(coordinate.version());

    String filename = coordinate.artifact() + DELIM + coordinate.version() + extension;
    return dir.resolve(filename);
  }

  /**
   * Downloads a file from URL to the target path.
   *
   * @param url the URL to download
   * @param target the target file path
   * @return the target path
   * @throws ResolutionException if download fails
   */
  private Path download(String url, Path target) throws ResolutionException {
    try {
      Path parent = target.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(TIMEOUT)
        .header("User-Agent", "Kalyx-Resolver/0.1.0")
        .GET()
        .build();

      HttpResponse<InputStream> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() != 200) {
        throw new ResolutionException("HTTP " + response.statusCode() + " for " + url);
      }

      InputStream body = response.body();
      if (body == null) {
        throw new ResolutionException("Empty response body for " + url);
      }

      try (InputStream in = body) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      }

      return target;
    } catch (IOException e) {
      throw new ResolutionException("Failed to download " + url + DELIM + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResolutionException("Download interrupted" + DELIM + url, e);
    }
  }
}
