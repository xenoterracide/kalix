// SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing
//
// SPDX-License-Identifier: LicenseRef-AllRightsReserved

package com.xenoterracide.kalyx.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xenoterracide.kalyx.config.Scope;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PomParser}.
 */
class PomParserTest {

  private PomParser parser;

  @BeforeEach
  void setUp() throws ResolutionException {
    parser = new PomParser();
  }

  @Test
  void shouldParseBasicPom() throws ResolutionException {
    String pom = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project>
        <groupId>com.example</groupId>
        <artifactId>test-lib</artifactId>
        <version>1.0.0</version>
      </project>
      """;

    PomParser.PomModel model = parser.parse(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));

    assertThat(model.coordinate().group()).isEqualTo("com.example");
    assertThat(model.coordinate().artifact()).isEqualTo("test-lib");
    assertThat(model.coordinate().version()).isEqualTo("1.0.0");
    assertThat(model.packaging()).isEqualTo("jar");
    assertThat(model.parent()).isEmpty();
    assertThat(model.dependencies()).isEmpty();
  }

  @Test
  void shouldParsePomWithDependencies() throws ResolutionException {
    String pom = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project>
        <groupId>com.example</groupId>
        <artifactId>test-app</artifactId>
        <version>1.0.0</version>
        <dependencies>
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.16</version>
          </dependency>
          <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.12.0</version>
            <scope>test</scope>
          </dependency>
        </dependencies>
      </project>
      """;

    PomParser.PomModel model = parser.parse(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));

    assertThat(model.dependencies()).hasSize(2);

    PomParser.PomDependency slf4j = model.dependencies().get(0);
    assertThat(slf4j.groupId()).isEqualTo("org.slf4j");
    assertThat(slf4j.artifactId()).isEqualTo("slf4j-api");
    assertThat(slf4j.version()).isEqualTo("2.0.16");
    assertThat(slf4j.scope()).isEqualTo(Scope.COMPILE);
    assertThat(slf4j.optional()).isFalse();

    PomParser.PomDependency junit = model.dependencies().get(1);
    assertThat(junit.groupId()).isEqualTo("org.junit.jupiter");
    assertThat(junit.artifactId()).isEqualTo("junit-jupiter");
    assertThat(junit.version()).isEqualTo("5.12.0");
    assertThat(junit.scope()).isEqualTo(Scope.TEST);
  }

  @Test
  void shouldParsePomWithParent() throws ResolutionException {
    String pom = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project>
        <parent>
          <groupId>com.example</groupId>
          <artifactId>parent-pom</artifactId>
          <version>2.0.0</version>
        </parent>
        <artifactId>child-module</artifactId>
      </project>
      """;

    PomParser.PomModel model = parser.parse(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));

    assertThat(model.coordinate().group()).isEqualTo("com.example");
    assertThat(model.coordinate().artifact()).isEqualTo("child-module");
    assertThat(model.coordinate().version()).isEqualTo("2.0.0");
    assertThat(model.parent()).isPresent();
    assertThat(model.parent().get().artifact()).isEqualTo("parent-pom");
  }

  @Test
  void shouldSubstituteProperties() throws ResolutionException {
    String pom = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project>
        <groupId>com.example</groupId>
        <artifactId>test-lib</artifactId>
        <version>1.0.0</version>
        <properties>
          <slf4j.version>2.0.16</slf4j.version>
        </properties>
        <dependencies>
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
          </dependency>
        </dependencies>
      </project>
      """;

    PomParser.PomModel model = parser.parse(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));

    assertThat(model.dependencies()).hasSize(1);
    assertThat(model.dependencies().get(0).version()).isEqualTo("2.0.16");
  }

  @Test
  void shouldParsePomPackaging() throws ResolutionException {
    String pom = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project>
        <groupId>com.example</groupId>
        <artifactId>parent-project</artifactId>
        <version>1.0.0</version>
        <packaging>pom</packaging>
      </project>
      """;

    PomParser.PomModel model = parser.parse(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));

    assertThat(model.packaging()).isEqualTo("pom");
    assertThat(model.isPomOnly()).isTrue();
  }

  @Test
  void shouldParseDependencyManagement() throws ResolutionException {
    String pom = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project>
        <groupId>com.example</groupId>
        <artifactId>test-app</artifactId>
        <version>1.0.0</version>
        <dependencyManagement>
          <dependencies>
            <dependency>
              <groupId>org.slf4j</groupId>
              <artifactId>slf4j-api</artifactId>
              <version>2.0.16</version>
            </dependency>
          </dependencies>
        </dependencyManagement>
        <dependencies>
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <!-- version from dependency management -->
          </dependency>
        </dependencies>
      </project>
      """;

    PomParser.PomModel model = parser.parse(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));

    assertThat(model.managedDependencies()).hasSize(1);
    assertThat(model.managedDependencies().get(0).version()).isEqualTo("2.0.16");

    // Dependency without version should get it from management
    assertThat(model.dependencies().get(0).version()).isNull();
  }

  @Test
  void shouldParseOptionalDependency() throws ResolutionException {
    String pom = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project>
        <groupId>com.example</groupId>
        <artifactId>test-app</artifactId>
        <version>1.0.0</version>
        <dependencies>
          <dependency>
            <groupId>org.example</groupId>
            <artifactId>optional-lib</artifactId>
            <version>1.0.0</version>
            <optional>true</optional>
          </dependency>
        </dependencies>
      </project>
      """;

    PomParser.PomModel model = parser.parse(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));

    assertThat(model.dependencies()).hasSize(1);
    assertThat(model.dependencies().get(0).optional()).isTrue();
  }

  @Test
  void shouldHandleProjectVersionProperty() throws ResolutionException {
    String pom = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project>
        <groupId>com.example</groupId>
        <artifactId>test-lib</artifactId>
        <version>1.5.0</version>
        <dependencies>
          <dependency>
            <groupId>com.example</groupId>
            <artifactId>other-lib</artifactId>
            <version>${project.version}</version>
          </dependency>
        </dependencies>
      </project>
      """;

    PomParser.PomModel model = parser.parse(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)));

    assertThat(model.dependencies().get(0).version()).isEqualTo("1.5.0");
  }

  @Test
  void shouldThrowOnInvalidXml() {
    String invalidPom = "not valid xml";

    assertThatThrownBy(() ->
      parser.parse(new ByteArrayInputStream(invalidPom.getBytes(StandardCharsets.UTF_8)))
    ).isInstanceOf(ResolutionException.class);
  }
}
