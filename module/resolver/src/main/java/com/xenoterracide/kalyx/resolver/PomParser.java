// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

import com.xenoterracide.kalyx.config.ArtifactCoordinate;
import com.xenoterracide.kalyx.config.Scope;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parses Maven POM files to extract project metadata.
 */
public final class PomParser {

  private static final String TAG_GROUP = "groupId";

  private static final String TAG_ARTIFACT = "artifactId";

  private static final String TAG_VERSION = "version";

  private static final String TAG_PACKAGING = "packaging";

  private static final String TAG_PARENT = "parent";

  private static final String TAG_PROPERTIES = "properties";

  private static final String TAG_DEPENDENCIES = "dependencies";

  private static final String TAG_DEPENDENCY = "dependency";

  private static final String TAG_SCOPE = "scope";

  private static final String TAG_OPTIONAL = "optional";

  private static final String TAG_DEP_MGMT = "dependencyManagement";

  private static final String DEFAULT_PACKAGING = "jar";

  private DocumentBuilder documentBuilder;

  /**
   * Creates a new POM parser.
   *
   * @throws ResolutionException if parser cannot be configured
   */
  public PomParser() throws ResolutionException {
    this.documentBuilder = createDocumentBuilder();
  }

  /**
   * Creates a document builder with security features.
   *
   * @return the document builder
   * @throws ResolutionException if configuration fails
   */
  private static DocumentBuilder createDocumentBuilder() throws ResolutionException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      return factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new ResolutionException("Failed to configure XML parser", e);
    }
  }

  /**
   * Parses a POM file.
   *
   * @param pomPath the path to the POM file
   * @return the parsed POM model
   * @throws ResolutionException if parsing fails
   */
  public PomModel parse(Path pomPath) throws ResolutionException {
    Objects.requireNonNull(pomPath, "pomPath");

    try (InputStream in = Files.newInputStream(pomPath)) {
      Document doc = this.documentBuilder.parse(in);
      return parseDocument(doc);
    } catch (IOException e) {
      throw new ResolutionException("Failed to read POM: " + pomPath, e);
    } catch (SAXException e) {
      throw new ResolutionException("Failed to parse POM XML: " + pomPath, e);
    }
  }

  /**
   * Parses a POM from an input stream.
   *
   * @param input the input stream
   * @return the parsed POM model
   * @throws ResolutionException if parsing fails
   */
  public PomModel parse(InputStream input) throws ResolutionException {
    Objects.requireNonNull(input, "input");

    try {
      Document doc = this.documentBuilder.parse(input);
      return parseDocument(doc);
    } catch (IOException e) {
      throw new ResolutionException("Failed to read POM", e);
    } catch (SAXException e) {
      throw new ResolutionException("Failed to parse POM XML", e);
    }
  }

  /**
   * Parses a DOM document into a POM model.
   *
   * @param doc the XML document
   * @return the parsed POM model
   */
  private static PomModel parseDocument(Document doc) {
    Element project = doc.getDocumentElement();

    Map<String, String> props = extractProperties(project);
    Element parent = getChildElement(project, TAG_PARENT);

    String groupId = extractGroupId(project, parent);
    String artifactId = extractArtifactId(project);
    String version = extractVersion(project, parent);
    String packaging = extractPackaging(project);

    ArtifactCoordinate coordinate = new ArtifactCoordinate(
      substitute(groupId, props),
      substitute(artifactId, props),
      substitute(version, props)
    );

    List<PomDependency> deps = extractDeps(project, props);
    List<PomDependency> managed = extractManagedDeps(project, props);
    Optional<ArtifactCoordinate> parentCoord = extractParentCoord(parent, props);

    return new PomModel(coordinate, packaging, parentCoord, deps, managed);
  }

  /**
   * Extracts the group ID from the project or parent.
   *
   * @param project the project element
   * @param parent the parent element
   * @return the group ID
   */
  @SuppressWarnings("Var") // Mutable: may be replaced from parent POM
  private static String extractGroupId(Element project, @Nullable Element parent) {
    @Nullable
    String groupId = getTextContent(project, TAG_GROUP);
    if (groupId == null && parent != null) {
      groupId = getTextContent(parent, TAG_GROUP);
    }
    return Objects.requireNonNull(groupId, "groupId must not be null");
  }

  /**
   * Extracts the artifact ID.
   *
   * @param project the project element
   * @return the artifact ID
   */
  private static String extractArtifactId(Element project) {
    return Objects.requireNonNull(getTextContent(project, TAG_ARTIFACT), "artifactId must not be null");
  }

  /**
   * Extracts the version from the project or parent.
   *
   * @param project the project element
   * @param parent the parent element
   * @return the version
   */
  @SuppressWarnings("Var") // Mutable: may be replaced from parent POM
  private static String extractVersion(Element project, @Nullable Element parent) {
    @Nullable
    String version = getTextContent(project, TAG_VERSION);
    if (version == null && parent != null) {
      version = getTextContent(parent, TAG_VERSION);
    }
    return Objects.requireNonNull(version, "version must not be null");
  }

  /**
   * Extracts the packaging type.
   *
   * @param project the project element
   * @return the packaging type
   */
  private static String extractPackaging(Element project) {
    @Nullable
    String packaging = getTextContent(project, TAG_PACKAGING);
    if (packaging == null || packaging.isEmpty()) {
      return DEFAULT_PACKAGING;
    }
    return packaging;
  }

  /**
   * Extracts the parent coordinate.
   *
   * @param parent the parent element
   * @param props the properties map
   * @return optional parent coordinate
   */
  private static Optional<ArtifactCoordinate> extractParentCoord(@Nullable Element parent, Map<String, String> props) {
    if (parent == null) {
      return Optional.empty();
    }

    String pg = Objects.requireNonNull(getTextContent(parent, TAG_GROUP), "parent groupId");
    String pa = Objects.requireNonNull(getTextContent(parent, TAG_ARTIFACT), "parent artifactId");
    String pv = Objects.requireNonNull(getTextContent(parent, TAG_VERSION), "parent version");

    return Optional.of(new ArtifactCoordinate(substitute(pg, props), substitute(pa, props), substitute(pv, props)));
  }

  /**
   * Extracts properties from the POM.
   *
   * @param project the project element
   * @return the properties map
   */
  private static Map<String, String> extractProperties(Element project) {
    Map<String, String> props = new HashMap<>();

    addBuiltInProps(project, props);
    addCustomProps(project, props);

    return props;
  }

  /**
   * Adds built-in properties.
   *
   * @param project the project element
   * @param props the properties map
   */
  private static void addBuiltInProps(Element project, Map<String, String> props) {
    @Nullable
    String version = getTextContent(project, TAG_VERSION);
    if (version != null) {
      props.put("project.version", version);
      props.put("pom.version", version);
    }

    @Nullable
    String groupId = getTextContent(project, TAG_GROUP);
    if (groupId != null) {
      props.put("project.groupId", groupId);
      props.put("pom.groupId", groupId);
    }

    String artifactId = Objects.requireNonNull(getTextContent(project, TAG_ARTIFACT), TAG_ARTIFACT);
    props.put("project." + TAG_ARTIFACT, artifactId);
    props.put("pom." + TAG_ARTIFACT, artifactId);
  }

  /**
   * Adds custom properties from the properties element.
   *
   * @param project the project element
   * @param props the properties map
   */
  private static void addCustomProps(Element project, Map<String, String> props) {
    Element propsElement = getChildElement(project, TAG_PROPERTIES);
    if (propsElement == null) {
      return;
    }

    NodeList children = propsElement.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        String key = child.getNodeName();
        String value = child.getTextContent().trim();
        props.put(key, value);
      }
    }
  }

  /**
   * Extracts dependencies.
   *
   * @param project the project element
   * @param props the properties map
   * @return list of dependencies
   */
  private static List<PomDependency> extractDeps(Element project, Map<String, String> props) {
    Element depsElement = getChildElement(project, TAG_DEPENDENCIES);
    return parseDeps(depsElement, props);
  }

  /**
   * Extracts managed dependencies.
   *
   * @param project the project element
   * @param props the properties map
   * @return list of managed dependencies
   */
  private static List<PomDependency> extractManagedDeps(Element project, Map<String, String> props) {
    Element depMgmt = getChildElement(project, TAG_DEP_MGMT);
    if (depMgmt == null) {
      return List.of();
    }

    Element depsElement = getChildElement(depMgmt, TAG_DEPENDENCIES);
    return parseDeps(depsElement, props);
  }

  /**
   * Parses dependencies from a dependencies element.
   *
   * @param depsElement the dependencies element
   * @param props the properties map
   * @return list of dependencies
   */
  private static List<PomDependency> parseDeps(@Nullable Element depsElement, Map<String, String> props) {
    if (depsElement == null) {
      return List.of();
    }

    List<PomDependency> deps = new ArrayList<>();
    NodeList depNodes = depsElement.getElementsByTagName(TAG_DEPENDENCY);

    for (int i = 0; i < depNodes.getLength(); i++) {
      Element dep = (Element) depNodes.item(i);
      PomDependency dependency = parseDep(dep, props);
      if (dependency != null) {
        deps.add(dependency);
      }
    }

    return deps;
  }

  /**
   * Parses a single dependency element.
   *
   * @param dep the dependency element
   * @param props the properties map
   * @return the parsed dependency, or null if invalid
   */
  @SuppressWarnings("Var") // Mutable: property substitution may replace values
  private static @Nullable PomDependency parseDep(Element dep, Map<String, String> props) {
    @Nullable
    String groupId = getTextContent(dep, TAG_GROUP);
    @Nullable
    String artifactId = getTextContent(dep, TAG_ARTIFACT);
    @Nullable
    String version = getTextContent(dep, TAG_VERSION);
    @Nullable
    String scope = getTextContent(dep, TAG_SCOPE);
    @Nullable
    String optional = getTextContent(dep, TAG_OPTIONAL);

    if (groupId == null || artifactId == null) {
      return null;
    }

    groupId = substitute(groupId, props);
    artifactId = substitute(artifactId, props);
    version = version != null ? substitute(version, props) : null;

    Scope depScope = parseScope(scope);
    boolean isOptional = "true".equalsIgnoreCase(optional);

    return new PomDependency(groupId, artifactId, version, depScope, isOptional);
  }

  /**
   * Parses a scope string.
   *
   * @param scope the scope string
   * @return the parsed scope
   */
  private static Scope parseScope(@Nullable String scope) {
    if (scope == null) {
      return Scope.COMPILE;
    }
    try {
      return Scope.valueOf(scope.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return Scope.COMPILE;
    }
  }

  /**
   * Substitutes property placeholders.
   *
   * @param value the value with placeholders
   * @param props the properties map
   * @return the substituted value
   */
  @SuppressWarnings("Var") // Mutable: replaced during property substitution
  private static String substitute(String value, Map<String, String> props) {
    String result = value;
    for (Map.Entry<String, String> entry : props.entrySet()) {
      String placeholder = "${" + entry.getKey() + "}";
      if (result.contains(placeholder)) {
        result = result.replace(placeholder, entry.getValue());
      }
    }
    return result;
  }

  /**
   * Gets the text content of a child element.
   *
   * @param parent the parent element
   * @param tagName the tag name
   * @return the text content, or null if not found
   */
  private static @Nullable String getTextContent(Element parent, String tagName) {
    Element child = getChildElement(parent, tagName);
    if (child != null) {
      return child.getTextContent().trim();
    }
    return null;
  }

  /**
   * Gets a direct child element by tag name.
   *
   * @param parent the parent element
   * @param tagName the tag name
   * @return the child element, or null if not found
   */
  private static @Nullable Element getChildElement(Element parent, String tagName) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
        return (Element) child;
      }
    }
    return null;
  }

  /**
   * Represents a parsed POM file.
   *
   * @param coordinate the project coordinates
   * @param packaging the packaging type
   * @param parent the parent POM coordinates
   * @param dependencies the project dependencies
   * @param managedDependencies the dependency management entries
   */
  @SuppressWarnings("Var")
  public record PomModel(
    ArtifactCoordinate coordinate,
    String packaging,
    Optional<ArtifactCoordinate> parent,
    List<PomDependency> dependencies,
    List<PomDependency> managedDependencies
  ) {
    /**
     * Creates a POM model.
     */
    public PomModel {
      Objects.requireNonNull(coordinate, "pomCoordinate");
      Objects.requireNonNull(packaging, TAG_PACKAGING);
      Objects.requireNonNull(parent, TAG_PARENT);
      Objects.requireNonNull(dependencies, TAG_DEPENDENCIES);
      Objects.requireNonNull(managedDependencies, "managedDependencies");
      dependencies = List.copyOf(dependencies);
      managedDependencies = List.copyOf(managedDependencies);
    }

    /**
     * Returns true if this is a POM-only project.
     *
     * @return true if packaging is "pom"
     */
    public boolean isPomOnly() {
      return "pom".equals(packaging);
    }
  }

  /**
   * Represents a dependency declared in a POM.
   *
   * @param groupId the group ID
   * @param artifactId the artifact ID
   * @param version the version (may be null)
   * @param scope the scope
   * @param optional whether optional
   */
  public record PomDependency(
    String groupId,
    String artifactId,
    @Nullable String version,
    Scope scope,
    boolean optional
  ) {
    /**
     * Creates a POM dependency.
     */
    public PomDependency {
      Objects.requireNonNull(groupId, TAG_GROUP);
      Objects.requireNonNull(artifactId, TAG_ARTIFACT);
      Objects.requireNonNull(scope, TAG_SCOPE);
    }

    /**
     * Returns the artifact key.
     *
     * @return the artifact key (groupId:artifactId)
     */
    public String key() {
      return groupId + ":" + artifactId;
    }

    /**
     * Returns true if version is specified.
     *
     * @return true if version present
     */
    public boolean hasVersion() {
      return version != null && !version.isEmpty();
    }

    /**
     * Converts to an artifact coordinate.
     *
     * @return optional artifact coordinate
     */
    public Optional<ArtifactCoordinate> toCoordinate() {
      if (hasVersion()) {
        return Optional.of(new ArtifactCoordinate(groupId, artifactId, Objects.requireNonNull(version)));
      }
      return Optional.empty();
    }
  }
}
