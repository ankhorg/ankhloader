package org.inksnow.asteroid;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@lombok.Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public final class AkDependency {
  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String classifier;
  private final String extension;
  private final String scope;
  private final boolean optional;
  private final List<ExclusionConfig> exclusions;
  private final Map<String, String> properties;

  public static AkDependency fillDependencyDefault(AkArtifact artifact, AkDependency dependency) {
    return AkDependency.builder()
        .groupId(dependency.groupId() == null ? artifact.groupId() : dependency.groupId())
        .artifactId(dependency.artifactId() == null ? artifact.artifactId() : dependency.artifactId())
        .version(dependency.version() == null ? artifact.version() : dependency.version())
        .extension(dependency.extension() == null ? "jar" : dependency.extension())
        .classifier(dependency.classifier() == null ? "" : dependency.classifier())
        .scope(dependency.scope() == null ? "runtime" : dependency.scope())
        .exclusions(dependency.exclusions())
        .properties(dependency.properties())
        .build();
  }

  @Getter
  @lombok.Builder
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  public static final class ExclusionConfig {
    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String extension;
  }
}
