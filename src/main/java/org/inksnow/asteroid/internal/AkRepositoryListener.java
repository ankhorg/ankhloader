package org.inksnow.asteroid.internal;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;

@Slf4j
public final class AkRepositoryListener implements RepositoryListener {
  @Override
  public void artifactDescriptorInvalid(RepositoryEvent event) {
    logger.trace("artifact descriptor invalid: {}", event);
  }

  @Override
  public void artifactDescriptorMissing(RepositoryEvent event) {
    logger.trace("artifact descriptor missing: {}", event);
  }

  @Override
  public void metadataInvalid(RepositoryEvent event) {
    logger.trace("metadata invalid: {}", event);
  }

  @Override
  public void artifactResolving(RepositoryEvent event) {
    logger.trace("artifact resolving: {}", event);
  }

  @Override
  public void artifactResolved(RepositoryEvent event) {
    logger.debug("artifact resolved: {}", event);
  }

  @Override
  public void metadataResolving(RepositoryEvent event) {
    logger.trace("metadata resolving: {}", event);
  }

  @Override
  public void metadataResolved(RepositoryEvent event) {
    logger.debug("metadata resolved: {}", event);
  }

  @Override
  public void artifactDownloading(RepositoryEvent event) {
    logger.trace("artifact downloading: {}", event);
  }

  @Override
  public void artifactDownloaded(RepositoryEvent event) {
    logger.debug("artifact downloaded: {}", event);
  }

  @Override
  public void metadataDownloading(RepositoryEvent event) {
    logger.trace("metadata downloading: {}", event);
  }

  @Override
  public void metadataDownloaded(RepositoryEvent event) {
    logger.debug("metadata downloaded: {}", event);
  }

  @Override
  public void artifactInstalling(RepositoryEvent event) {
    logger.trace("artifact installing: {}", event);
  }

  @Override
  public void artifactInstalled(RepositoryEvent event) {
    logger.debug("artifact installed: {}", event);
  }

  @Override
  public void metadataInstalling(RepositoryEvent event) {
    logger.trace("metadata installing: {}", event);
  }

  @Override
  public void metadataInstalled(RepositoryEvent event) {
    logger.debug("metadata installed: {}", event);
  }

  @Override
  public void artifactDeploying(RepositoryEvent event) {
    logger.trace("artifact deploying: {}", event);
  }

  @Override
  public void artifactDeployed(RepositoryEvent event) {
    logger.debug("artifact deployed: {}", event);
  }

  @Override
  public void metadataDeploying(RepositoryEvent event) {
    logger.trace("metadata deploying: {}", event);
  }

  @Override
  public void metadataDeployed(RepositoryEvent event) {
    logger.debug("metadata deployed: {}", event);
  }
}
