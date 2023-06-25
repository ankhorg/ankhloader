package org.inksnow.asteroid.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Getter
public final class ConfigRepository {
  private final String id;
  private final String type;
  private final String url;
  private final RepositoryPolicyConfig releasePolicy;
  private final RepositoryPolicyConfig snapshotPolicy;
  private final ProxyConfig proxy;
  private final AuthenticationConfig authentication;
  private final List<ConfigRepository> mirroredRepositories;

  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  @Getter
  public static final class RepositoryPolicyConfig {
    private final boolean disable;
    private final UpdatePolicy updatePolicy;
    private final ChecksumPolicy checksumPolicy;

    @RequiredArgsConstructor
    public enum UpdatePolicy {
      NEVER("never"),
      ALWAYS("always"),
      DAILY("daily"),
      INTERVAL("interval"),
      DEFAULT("");

      @Getter
      private final String value;
    }

    @RequiredArgsConstructor
    public enum ChecksumPolicy {
      FAIL("fail"),
      WARN("warn"),
      IGNORE("ignore"),
      DEFAULT("");

      @Getter
      private final String value;
    }
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  @Getter
  public static final class ProxyConfig {
    private final String type;
    private final String host;
    private final int port;
    private final AuthenticationConfig authentication;
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  @Getter
  public static final class AuthenticationConfig {
    private final String username;
    private final String password;

    private final String ntlmWorkstation;
    private final String ntlmDomain;

    private final String privateKeyPathname;
    private final String privateKeyPassphrase;
  }
}
