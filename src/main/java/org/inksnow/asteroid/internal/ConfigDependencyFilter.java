package org.inksnow.asteroid.internal;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class ConfigDependencyFilter implements DependencyFilter {
  private final ClassLoader classLoader;
  private final Map<String, List<String>> excludeMap;

  @Override
  public synchronized boolean accept(DependencyNode node, List<DependencyNode> parents) {
    val artifact = node.getArtifact();
    if (artifact == null) {
      return true;
    }
    val key = artifact.getGroupId() + ":" + artifact.getArtifactId();
    val testClasses = excludeMap.get(key);
    if (testClasses == null) {
      return true;
    } else if (testClasses.isEmpty()) {
      return false;
    }
    for (val testClass : testClasses) {
      if (classLoader.getResource(testClass.replace('.', '/') + ".class") == null) {
        excludeMap.remove(key);
        return true;
      }
    }
    excludeMap.put(key, Collections.emptyList());
    return false;
  }
}
