package org.inksnow.aloader.internal;

import lombok.val;
import org.apache.maven.api.model.Model;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.inksnow.aloader.AkLoader;
import org.inksnow.aloader.model.Repository;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AkCore {
  private final AkLoader loader;

  public AkCore(AkLoader loader) {
    this.loader = loader;
  }

  public Iterator<Pair<Repository, Metadata>> resolve(String groupId, String artifactId) {
    return loader.repositories()
        .stream()
        .map(it->Pair.of(it, it.resolve(groupId, artifactId)))
        .iterator();
  }

  public Iterator<Pair<Repository, Model>> resolve(String groupId, String artifactId, String version) {
    if(version == null){
      return loader.repositories()
          .stream()
          .map(it->Pair.of(it, it.resolve(groupId, artifactId)))
          .filter(Objects::nonNull)
          .map(it-> Pair.of(it.key(), it.key().resolve(
              it.value().getGroupId(),
              it.value().getArtifactId(),
              it.value().getVersioning().getLatest()
          )))
          .iterator();
    }else{
      return loader.repositories()
          .stream()
          .map(it->Pair.of(it, it.resolve(groupId, artifactId, version)))
          .iterator();
    }
  }
}
