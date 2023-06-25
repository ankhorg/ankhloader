package org.inksnow.asteroid.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.inksnow.asteroid.AkArtifact;

import java.lang.invoke.MethodHandle;
import java.net.URLClassLoader;

@Slf4j
public class EnvironmentUcl extends AbstractEnvironment {
  private static final MethodHandle urlClassLoader_addUrl =
      BootstrapUtil.ofVirtual("Ljava/net/URLClassLoader;addURL(Ljava/net/URL;)V");

  private final URLClassLoader classLoader;
  private final AkLoaderResolver resolver;

  public EnvironmentUcl(String name, URLClassLoader classLoader) {
    this.classLoader = classLoader;
    this.resolver = new AkLoaderResolver(name, this::onDeploy);
  }

  @SneakyThrows
  protected void onDeploy(AkArtifact artifact) {
    logger.debug("inject {} into {}", artifact.file(), classLoader);
    urlClassLoader_addUrl.invokeExact(classLoader, artifact.file().toURI().toURL());
  }

  @Override
  public URLClassLoader classLoader() {
    return classLoader;
  }

  @Override
  protected AkLoaderResolver selfResolver() {
    return resolver;
  }
}
