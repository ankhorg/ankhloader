package org.inksnow.asteroid.internal;

import org.inksnow.asteroid.AkClassLoader;

public final class EnvironmentConfigure extends EnvironmentUcl {
  public EnvironmentConfigure(String name) {
    super(name, new AkClassLoader());
  }

  @Override
  public AkClassLoader classLoader() {
    return (AkClassLoader) super.classLoader();
  }
}
