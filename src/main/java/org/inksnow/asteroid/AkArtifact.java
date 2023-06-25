package org.inksnow.asteroid;

import lombok.Builder;
import lombok.Getter;

import java.io.File;
import java.util.Map;

@Builder
@Getter
public final class AkArtifact {
  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String classifier;
  private final String extension;
  private final Map<String, String> properties;
  private final File file;

  @Getter(lazy = true)
  private final String key = groupId + ":" + artifactId
      + (isNullOrEquals(extension, "jar") ? "" : (":" + extension))
      + (isNullOrEquals(classifier, "") ? "" : (":" + classifier));

  private static boolean isNullOrEquals(String value, String defaultValue) {
    return value == null || defaultValue.equals(value);
  }
}
