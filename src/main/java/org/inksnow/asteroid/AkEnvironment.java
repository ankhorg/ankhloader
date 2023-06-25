package org.inksnow.asteroid;

import java.util.List;

public interface AkEnvironment {
  ClassLoader classLoader();

  void add(AkDependency dependency);

  AkDeployResult deploy();

  List<AkArtifact> installedArtifact();
}
