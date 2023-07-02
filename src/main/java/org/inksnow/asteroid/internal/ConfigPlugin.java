package org.inksnow.asteroid.internal;

import com.hrakaroo.glob.MatchingEngine;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;
import org.inksnow.asteroid.AkDependency;
import org.inksnow.asteroid.AkLoader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public final class ConfigPlugin {
  @Getter
  private final Map<String, EnvironmentConfig> environments;

  public static ConfigPlugin loadAkPluginConfig(JarFile jarFile, String name) throws IOException {
    val asteroidPluginEntry = jarFile.getEntry("asteroid-plugin.json");
    if (asteroidPluginEntry == null) {
      return null;
    }
    try (val reader = new InputStreamReader(jarFile.getInputStream(asteroidPluginEntry), StandardCharsets.UTF_8)) {
      return AkLoader.gson.fromJson(reader, ConfigPlugin.class);
    }
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  public final class DelegateConfig {
    @Getter
    private final String name;
    @Getter
    private final int priority;
    @Getter
    private final List<MatchingEngine> whitelist;
    @Getter
    private final List<MatchingEngine> blacklist;
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  public final class ResourceMapConfig {
    @Getter
    private final int priority;
    @Getter
    private final String from;
    @Getter
    private final String to;
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  public final class EnvironmentConfig {
    @Getter
    private final List<AkDependency> dependencies;
    @Getter
    private final List<DelegateConfig> delegates;
    @Getter
    private final List<ResourceMapConfig> resourceMaps;
  }
}
