package org.inksnow.asteroid;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.inksnow.asteroid.internal.AkLoaderResolver;
import org.inksnow.asteroid.internal.ConfigRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class AkConfigLoader {
  private static final Pattern COORDINATE_PATTERN =
      Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");
  private static final Pattern VERSION_OPTIONAL_COORDINATE_PATTERN =
      Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+)(:([^: ]+))?)?)?");
  private static final Path configDirectory = Paths.get("plugins", AkLoader.PLUGIN_ID);

  @Getter(lazy = true)
  private static final List<RemoteRepository> repositories = loadRepositoryConfig();

  private final String environmentName;

  private List<AkDependency> dependencies;

  public AkConfigLoader(String environmentName) {
    this.environmentName = environmentName;
    this.dependencies = loadDependencyConfig();
  }

  @SneakyThrows
  private static List<RemoteRepository> loadRepositoryConfig() {
    val repositoryConfigArray = loadConfig("repository.json", new TypeToken<List<ConfigRepository>>() {
    });
    if (repositoryConfigArray == null || repositoryConfigArray.isEmpty()) {
      return Collections.emptyList();
    }
    return repositoryConfigArray.stream()
        .map(AkConfigLoader::createRepositoryConfig)
        .collect(Collectors.toList());
  }

  private static RemoteRepository createRepositoryConfig(ConfigRepository config) {
    val builder = new RemoteRepository.Builder(
        config.id(),
        config.type() == null ? "default" : config.type(),
        config.url()
    );
    builder.setReleasePolicy(createRepositoryPolicy(config.releasePolicy()));
    builder.setSnapshotPolicy(createRepositoryPolicy(config.snapshotPolicy()));
    if (config.proxy() != null) {
      builder.setProxy(new Proxy(
          config.proxy().type(),
          config.proxy().host(),
          config.proxy().port(),
          createAuthentication(config.proxy().authentication())
      ));
    }
    builder.setAuthentication(createAuthentication(config.authentication()));
    if (config.mirroredRepositories() != null) {
      builder.setMirroredRepositories(config.mirroredRepositories()
          .stream()
          .map(AkConfigLoader::createRepositoryConfig)
          .collect(Collectors.toList()));
    }
    return builder.build();
  }

  private static RepositoryPolicy createRepositoryPolicy(ConfigRepository.RepositoryPolicyConfig config) {
    if (config == null) {
      return new RepositoryPolicy();
    }
    return new RepositoryPolicy(
        !config.disable(),
        config.updatePolicy() == null ? "" : config.updatePolicy().value(),
        config.checksumPolicy() == null ? "" : config.checksumPolicy().value()
    );
  }

  private static Authentication createAuthentication(ConfigRepository.AuthenticationConfig config) {
    if (config == null) {
      return null;
    }
    return new AuthenticationBuilder()
        .addUsername(config.username())
        .addPassword(config.password())
        .addNtlm(config.ntlmWorkstation(), config.ntlmDomain())
        .addPrivateKey(config.privateKeyPathname(), config.privateKeyPassphrase())
        .build();
  }

  private static <T> T loadConfig(String name, Class<T> type) throws IOException {
    return loadConfig(name, TypeToken.get(type));
  }

  private static <T> T loadConfig(String name, TypeToken<T> type) throws IOException {
    val configFile = configDirectory.resolve(name);
    if (!Files.exists(configFile)) {
      Files.createDirectories(configFile.getParent());
      try (val in = AkLoaderResolver.class.getClassLoader().getResourceAsStream("asteroid_default_config/" + name)) {
        if (in == null) {
          return null;
        }
        Files.copy(in, configFile);
      }
    }
    try (val reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
      return AkLoader.gson.fromJson(reader, type.getType());
    }
  }

  private static void editConfig(String name, Consumer<List<JsonObject>> action) throws IOException {
    val configFile = configDirectory.resolve(name);
    if (!Files.exists(configFile)) {
      Files.createDirectories(configFile.getParent());
      try (val in = AkLoaderResolver.class.getClassLoader().getResourceAsStream("asteroid_default_config/" + name)) {
        if (in != null) {
          Files.copy(in, configFile);
        }
      }
    }
    List<JsonObject> data;
    try (val reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
      List<JsonObject> rawData = AkLoader.gson.fromJson(reader, new TypeToken<List<JsonObject>>() {
      }.getType());
      data = rawData == null ? new ArrayList<>() : new ArrayList<>(rawData);
    }
    action.accept(data);
    try (val writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
      AkLoader.gson.toJson(data, writer);
    }
  }

  @SneakyThrows
  public void install(String coords) {
    val m = COORDINATE_PATTERN.matcher(coords);
    if (!m.matches()) {
      throw new IllegalArgumentException("Bad artifact coordinates " + coords
          + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
    }
    editConfig(environmentName + ".json", list -> {
      list.removeIf(it ->
          it.has("groupId") && it.has("artifactId")
              && it.get("groupId").getAsString().equals(m.group(1))
              && it.get("artifactId").getAsString().equals(m.group(2)));
      val newEntry = new JsonObject();
      newEntry.addProperty("groupId", m.group(1));
      newEntry.addProperty("artifactId", m.group(2));
      newEntry.addProperty("version", m.group(7));
      if (m.group(4) != null && !m.group(4).isEmpty()) {
        newEntry.addProperty("extension", m.group(4));
      }
      if (m.group(6) != null && !m.group(6).isEmpty()) {
        newEntry.addProperty("classifier", m.group(6));
      }
      list.add(newEntry);
    });
    this.dependencies = loadDependencyConfig();
  }

  @SneakyThrows
  public void remove(String coords) {
    val m = VERSION_OPTIONAL_COORDINATE_PATTERN.matcher(coords);
    if (!m.matches()) {
      throw new IllegalArgumentException("Bad artifact coordinates " + coords
          + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>[:<version>]]]");
    }
    editConfig(environmentName + ".json", list -> {
      list.removeIf(it -> {
        if (!it.has("groupId") || !it.has("artifactId")) {
          return false;
        }
        if (!it.get("groupId").getAsString().equals(m.group(1)) || !it.get("artifactId").getAsString().equals(m.group(2))) {
          return false;
        }
        if (m.group(4) != null && !m.group(4).isEmpty()) {
          val rawExtension = it.has("extension") ? it.get("extension").getAsString() : "jar";
          if (!rawExtension.equals(m.group(4))) {
            return false;
          }
        }
        if (m.group(6) != null && !m.group(6).isEmpty()) {
          val rawClassifier = it.has("classifier") ? it.get("classifier").getAsString() : "";
          if (!rawClassifier.equals(m.group(6))) {
            return false;
          }
        }
        if (m.group(7) != null && !m.group(7).isEmpty() && it.has("version")) {
          return it.get("version").getAsString().equals(m.group(7));
        }
        return true;
      });
    });
    this.dependencies = loadDependencyConfig();
  }

  public void applyToEnvironment(AkEnvironment environment){
    for (AkDependency dependency : dependencies) {
      environment.add(dependency);
    }
  }

  @SneakyThrows
  private List<AkDependency> loadDependencyConfig() {
    val dependencyConfigArray = loadConfig(environmentName + ".json", new TypeToken<List<AkDependency>>() {
    });
    if (dependencyConfigArray == null) {
      return Collections.emptyList();
    }else {
      return dependencyConfigArray;
    }
  }
}
