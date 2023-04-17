package org.inksnow.aloader;

import lombok.Getter;
import lombok.Singular;
import org.apache.maven.api.model.Model;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.inksnow.aloader.internal.AkCore;
import org.inksnow.aloader.internal.DcLazy;
import org.inksnow.aloader.internal.IteratorProvider;
import org.inksnow.aloader.internal.Pair;
import org.inksnow.aloader.model.Repository;

import java.util.ArrayList;
import java.util.List;

public final class AkLoader {
  @Singular @Getter
  private final List<Repository> repositories = new ArrayList<>();
  private final DcLazy<AkCore> core = DcLazy.of(()->new AkCore(this));

  public Iterable<Pair<Repository, Metadata>> resolve(String groupId, String artifactId) {
    return new IteratorProvider<>(core.get().resolve(groupId, artifactId));
  }

  public Iterable<Pair<Repository, Model>> resolve(String groupId, String artifactId, String version) {
    return new IteratorProvider<>(core.get().resolve(groupId, artifactId, version));
  }
}
