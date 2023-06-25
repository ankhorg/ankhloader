package org.inksnow.asteroid.internal;

import com.hrakaroo.glob.MatchingEngine;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.inksnow.asteroid.AkDependency;

import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public final class ConfigPlugin {
  @Getter
  private final Map<String, EnvironmentConfig> environments;

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
