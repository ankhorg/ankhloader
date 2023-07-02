package org.inksnow.asteroid.bukkit;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.java.JavaPlugin;
import org.inksnow.asteroid.*;
import org.inksnow.asteroid.internal.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarFile;

@Slf4j
public final class EnvironmentBukkit extends EnvironmentUcl {
  private final AkConfigLoader configLoader = new AkConfigLoader("bukkit");
  private final List<AkArtifact> pluginArtifacts = new ArrayList<>();

  private EnvironmentBukkit() {
    super("bukkit", (URLClassLoader) AkLoaderPlugin.class.getClassLoader());
  }

  public static void install(){
    val bukkitEnv = new EnvironmentBukkit();
    val oldBukkitEnv = AkLoader.putEnvironment("bukkit", bukkitEnv);
    if(oldBukkitEnv != null){
      throw new IllegalStateException("Bukkit environment have been set");
    }
    val systemConsole = System.console();
    if(systemConsole != null){
      val writer = systemConsole.writer();
      writer.write("asteroid logo print\n" +
          "  █████  ███████ ████████ ███████ ██████   ██████  ██ ██████  \n" +
          " ██   ██ ██         ██    ██      ██   ██ ██    ██ ██ ██   ██ \n" +
          " ███████ ███████    ██    █████   ██████  ██    ██ ██ ██   ██ \n" +
          " ██   ██      ██    ██    ██      ██   ██ ██    ██ ██ ██   ██ \n" +
          " ██   ██ ███████    ██    ███████ ██   ██  ██████  ██ ██████  \n" +
          "        asteroid package tool 1.0-SNAPSHOT deploying          \n");
      writer.flush();
    }else{
      logger.info("asteroid package tool 1.0-SNAPSHOT deploying");
    }
    bukkitEnv.deploy();
  }

  @Override
  protected void onDeploy(AkArtifact artifact) {
    try{
      onDeployImpl(artifact);
    }catch (Exception e){
      logger.error("Could not load '{}', load as library", artifact.key(), e);
    }
  }

  private void onDeployImpl(AkArtifact artifact) {
    if (!artifact.file().getName().endsWith(".jar")) {
      logger.warn("can't load jar file");
      return;
    }
    boolean isPlugin = false;
    try (val jarFile = new JarFile(artifact.file())) {
      isPlugin = isBukkitPlugin(jarFile);
    } catch (IOException e) {
      logger.error("Failed to test file is bukkit plugin");
    }
    if (isPlugin) {
      pluginArtifacts.add(artifact);
      return;
    }
    super.onDeploy(artifact);
  }

  private void installPluginImpl(AkArtifact artifact){
    try {
      ConfigPlugin akPluginConfig = null;
      try (val jarFile = new JarFile(artifact.file())) {
        akPluginConfig = ConfigPlugin.loadAkPluginConfig(jarFile, "asteroid-plugin.json");
      } catch (IOException e) {
        logger.error("Failed to load asteroid plugin config", e);
      }
      val pendingPluginClassLoaderActions = new LinkedList<Consumer<ClassLoader>>();
      if (akPluginConfig != null) {
        for (val envConfig : akPluginConfig.environments().entrySet()) {
          val rawInternalEnvironment = AkLoader.getOrCreateEnvironment(envConfig.getKey());
          if (!(rawInternalEnvironment instanceof EnvironmentConfigure)) {
            throw new IllegalStateException("Illegal internal environment name '" + envConfig.getKey() + "'");
          }
          val internalEnvironment = (EnvironmentConfigure) rawInternalEnvironment;
          if(envConfig.getValue().delegates() != null) {
            envConfig.getValue()
                .delegates()
                .forEach(it->applyDelegateConfig(internalEnvironment, it, pendingPluginClassLoaderActions));
          }
          if(envConfig.getValue().resourceMaps() != null){
            envConfig.getValue()
                .resourceMaps()
                .forEach(it -> internalEnvironment.classLoader()
                    .addResourceMap(it.priority(), it.from(), it.to()));
          }
          for (val entry : envConfig.getValue().dependencies()) {
            internalEnvironment.add(AkDependency.fillDependencyDefault(artifact, entry));
          }
          internalEnvironment.deploy();
        }
      }
      logger.info("asteroid package tool load bukkit plugin {}", artifact.key());
      val pluginInstance = (JavaPlugin) Bukkit.getServer()
          .getPluginManager()
          .loadPlugin(artifact.file());
      val pluginClassLoader = pluginInstance.getClass().getClassLoader();
      pendingPluginClassLoaderActions.forEach(it->it.accept(pluginClassLoader));
      if (akPluginConfig != null) {
        invokeInjectMethod(artifact, pluginInstance);
      }
    } catch (InvalidPluginException | InvalidDescriptionException e) {
      logger.error("Could not load '{}', load as library", artifact.key(), e);
      super.onDeploy(artifact);
    }
  }

  private static void applyDelegateConfig(
      EnvironmentConfigure internalEnvironment,
      ConfigPlugin.DelegateConfig delegate,
      List<Consumer<ClassLoader>> pendingPluginClassLoaderActions
  ) {
    ClassLoader classLoader;
    boolean isPluginClassLoader = false;
    switch (delegate.name()) {
      case "plugin": {
        classLoader = null;
        isPluginClassLoader = true;
        break;
      }
      case "system": {
        classLoader = null;
        break;
      }
      default: {
        classLoader = AkLoader.getOrCreateEnvironment(delegate.name()).classLoader();
        break;
      }
    }
    if (isPluginClassLoader) {
      pendingPluginClassLoaderActions.add(pluginClassLoader -> internalEnvironment.classLoader()
          .addDelegate(delegate.priority(), pluginClassLoader, delegate.whitelist(), delegate.blacklist()));
    } else {
      internalEnvironment.classLoader()
          .addDelegate(delegate.priority(), classLoader, delegate.whitelist(), delegate.blacklist());
    }
  }

  private static void invokeInjectMethod(AkArtifact artifact, Object pluginInstance){
    for (val method : pluginInstance.getClass().getDeclaredMethods()) {
      if ("onInject".equals(method.getName()) && method.getParameterCount() == 0) {
        try {
          BootstrapUtil.lookup()
              .unreflect(method)
              .bindTo(pluginInstance)
              .invoke();
        } catch (Throwable e){
          logger.error("Could not load '{}', load as library", artifact.key(), e);
        }
      }
    }
  }

  private static boolean isBukkitPlugin(JarFile jarFile) throws IOException {
    return jarFile.getEntry("plugin.yml") != null
        || jarFile.getEntry("paper-plugin.yml") != null;
  }

  @Override
  public AkDeployResult deploy() {
    selfResolver().dependencies().clear();
    configLoader.applyToEnvironment(this);
    logger.info("deploy bukkit dependencies");
    if (super.deploy() == AkDeployResult.SUCCESS) {
      logger.info("deploy plugin in bukkit dependencies");
      pluginArtifacts.forEach(this::installPluginImpl);
      return AkDeployResult.SUCCESS;
    }else{
      return AkDeployResult.NEED_RESTART;
    }
  }
}
