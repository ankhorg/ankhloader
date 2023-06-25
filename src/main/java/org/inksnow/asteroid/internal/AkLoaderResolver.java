package org.inksnow.asteroid.internal;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.guice.AetherModule;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.inksnow.asteroid.AkArtifact;
import org.inksnow.asteroid.AkConfigLoader;
import org.inksnow.asteroid.AkDeployResult;
import org.inksnow.asteroid.AkLoader;

import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public final class AkLoaderResolver {
  private static final Path cacheDirectory = Paths.get("plugins", AkLoader.PLUGIN_ID, "cache");
  private final String environmentName;
  private final Consumer<AkArtifact> onDeploy;
  @Getter
  private final Map<String, AkArtifact> installedArtifactMap;
  private final RepositorySystem repositorySystem;
  private final LocalRepository localRepository;
  private final ConfigDependencyFilter configDependencyFilter;
  @Getter
  private final List<Dependency> dependencies;
  private boolean pendingReboot;

  public AkLoaderResolver(String environmentName, Consumer<AkArtifact> onDeploy) {
    this.environmentName = environmentName;
    this.onDeploy = onDeploy;
    this.installedArtifactMap = new LinkedHashMap<>();
    this.repositorySystem = createRepositorySystem();
    this.localRepository = new LocalRepository(cacheDirectory.toFile());
    this.configDependencyFilter = loadConfigDependencyFilter();
    this.dependencies = new ArrayList<>();
    this.pendingReboot = false;
  }

  private RepositorySystem createRepositorySystem() {
    val locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
        logger.error("Service creation failed for {} with implementation {}", type, impl, exception);
      }
    });
    return locator.getService(RepositorySystem.class);
  }

  @SneakyThrows
  private ConfigDependencyFilter loadConfigDependencyFilter() {
    val classLoader = AkLoaderResolver.class.getClassLoader().getParent();
    val excludeMap = new HashMap<String, List<String>>();
    try (val reader = new InputStreamReader(AkLoaderResolver.class.getClassLoader().getResourceAsStream("asteroid_build_meta/testable-dependency.json"))) {
      val dependency = AkLoader.gson.fromJson(reader, JsonObject.class);
      for (val entry : dependency.entrySet()) {
        val key = entry.getKey();
        val value = entry.getValue();
        final List<String> testClasses;
        if (value.isJsonArray()) {
          val valueArray = value.getAsJsonArray();
          testClasses = new ArrayList<>(valueArray.size());
          for (val element : valueArray) {
            testClasses.add(element.getAsString());
          }
        } else {
          testClasses = Collections.singletonList(value.getAsString());
        }
        excludeMap.put(key, testClasses);
      }
    }
    return new ConfigDependencyFilter(classLoader, excludeMap);
  }

  private RepositorySystemSession newSession() {
    val session = MavenRepositorySystemUtils.newSession();
    session.setConfigProperty("aether.connector.userAgent", "AsteroidPackageTool/1.0.0(im@inker.bot)");
    session.setConfigProperty("aether.connector.basic.threads", "5");
    session.setConfigProperty("aether.metadataResolver.threads", "5");
    session.setConfigProperty("aether.dependencyCollector.impl", "bf");
    session.setConfigProperty("aether.dependencyCollector.bf.threads", "5");
    session.setConfigProperty("aether.enhancedLocalRepository.splitRemoteRepository", "true");
    session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));
    session.setTransferListener(new AkTransferListener());
    session.setRepositoryListener(new AkRepositoryListener());
    return session;
  }

  private List<AkArtifact> resolve() throws DependencyResolutionException {
    val session = newSession();
    val collectRequest = new CollectRequest();
    collectRequest.setRootArtifact(new DefaultArtifact(
        "your",
        "server",
        "classpath",
        environmentName,
        Long.toString(System.currentTimeMillis())
    ));
    collectRequest.setDependencies(dependencies);
    collectRequest.setRepositories(AkConfigLoader.repositories());

    val dependencyRequest = new DependencyRequest();
    dependencyRequest.setCollectRequest(collectRequest);
    dependencyRequest.setFilter(configDependencyFilter);
    val rootNode = repositorySystem.resolveDependencies(session, dependencyRequest).getRoot();

    rootNode.accept(new DependencyGraphDumper(logger::debug));

    PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
    rootNode.accept(nlg);

    return nlg.getArtifacts(false)
        .stream()
        .map(it -> AkArtifact.builder()
            .groupId(it.getGroupId())
            .artifactId(it.getArtifactId())
            .version(it.getVersion())
            .classifier(it.getClassifier())
            .extension(it.getExtension())
            .properties(it.getProperties())
            .file(it.getFile())
            .build())
        .collect(Collectors.toList());
  }

  @SneakyThrows
  public AkDeployResult deploy() {
    val artifactList = resolve();
    if (pendingReboot) {
      return AkDeployResult.NEED_RESTART;
    }
    val pendingMap = new LinkedHashMap<String, AkArtifact>();
    for (val artifact : artifactList) {
      val oldArtifact = installedArtifactMap.get(artifact.key());
      if (oldArtifact == null) {
        pendingMap.put(artifact.key(), artifact);
        continue;
      }
      if (!oldArtifact.version().equals(artifact.version())) {
        pendingReboot = true;
        break;
      }
    }
    if (pendingReboot) {
      return AkDeployResult.NEED_RESTART;
    }
    for (val entry : pendingMap.entrySet()) {
      onDeploy.accept(entry.getValue());
      installedArtifactMap.put(entry.getKey(), entry.getValue());
    }
    return AkDeployResult.SUCCESS;
  }
}
