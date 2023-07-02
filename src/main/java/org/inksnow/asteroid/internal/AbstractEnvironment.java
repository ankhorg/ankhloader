package org.inksnow.asteroid.internal;

import lombok.val;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.inksnow.asteroid.AkArtifact;
import org.inksnow.asteroid.AkDependency;
import org.inksnow.asteroid.AkDeployResult;
import org.inksnow.asteroid.AkEnvironment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractEnvironment implements AkEnvironment {
  protected static Dependency createDependency(AkDependency config) {
    val artifact = new DefaultArtifact(
        config.groupId(),
        config.artifactId(),
        config.classifier(),
        config.extension() == null ? "jar" : config.extension(),
        config.version(),
        config.properties(),
        (File) null
    );
    val exclusions = config.exclusions() == null ? null :
        config.exclusions()
            .stream()
            .map(AbstractEnvironment::createExclusion)
            .collect(Collectors.toList());
    return new Dependency(
        artifact,
        config.scope() == null ? "runtime" : config.scope(),
        config.optional(),
        exclusions
    );
  }

  protected static Exclusion createExclusion(AkDependency.ExclusionConfig config) {
    val anyNotNull = (config.groupId() != null
        || config.artifactId() != null
        || config.classifier() != null
        || config.extension() != null);
    return new Exclusion(
        (anyNotNull && config.groupId() == null) ? "*" : config.groupId(),
        (anyNotNull && config.artifactId() == null) ? "*" : config.groupId(),
        (anyNotNull && config.classifier() == null) ? "*" : config.classifier(),
        (anyNotNull && config.extension() == null) ? "*" : config.extension()
    );
  }

  protected abstract AkLoaderResolver selfResolver();

  @Override
  public List<AkArtifact> installedArtifact() {
    return new ArrayList<>(selfResolver().installedArtifactMap().values());
  }

  @Override
  public void add(AkDependency config) {
    selfResolver().dependencies()
        .add(createDependency(config));
  }

  @Override
  public AkDeployResult deploy() {
    return selfResolver().deploy();
  }

}
