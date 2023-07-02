package org.inksnow.asteroid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hrakaroo.glob.MatchingEngine;
import org.inksnow.asteroid.internal.EnvironmentConfigure;
import org.inksnow.asteroid.internal.MatchingEngineTypeAdapter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class AkLoader {
  public static final String PLUGIN_ID = "asteroid-package-tool";
  public static final Gson gson = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .registerTypeAdapter(MatchingEngine.class, new MatchingEngineTypeAdapter())
      .create();

  private static final Map<String, AkEnvironment> environments = new LinkedHashMap<>();

  private AkLoader() {
    throw new IllegalStateException();
  }

  public static AkEnvironment bukkit() {
    return getEnvironment("bukkit");
  }

  public static synchronized AkEnvironment getEnvironment(String name) {
    return environments.get(name);
  }

  public static synchronized AkEnvironment putEnvironment(String name, AkEnvironment env) {
    return environments.put(name, env);
  }

  public static synchronized AkEnvironment getOrCreateEnvironment(String name) {
    return environments.computeIfAbsent(name, it -> new EnvironmentConfigure(name));
  }

  public static synchronized AkEnvironment getOrCreateEnvironment(String name, Supplier<AkEnvironment> envSupplier) {
    return environments.computeIfAbsent(name, it -> envSupplier.get());
  }
}
