package org.inksnow.asteroid.bukkit;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.inksnow.asteroid.internal.AkEmptyJarFile;
import org.inksnow.asteroid.internal.BootstrapUtil;

import java.io.File;
import java.net.URLClassLoader;

@Slf4j
public final class AkLoaderPlugin extends JavaPlugin {
  static {
    try {
      // it should be org/bukkit/plugin/java/PluginClassLoader, but it's package-protected access
      // MethodHandle use invoke, don't use invokeExact, because we can't access class PluginClassLoader
      val pluginClassLoader = (URLClassLoader) AkLoaderPlugin.class.getClassLoader();
      val bukkitClassLoader = (URLClassLoader) Bukkit.class.getClassLoader();
      // val pluginFile = pluginClassLoader.file;
      val pluginFile = (File) BootstrapUtil
          .ofGet("Lorg/bukkit/plugin/java/PluginClassLoader;file:Ljava/io/File;")
          .invoke(pluginClassLoader);
      // bukkitClassLoader.addURL(pluginFile.toURI().toURL());
      BootstrapUtil
          .ofVirtual("Ljava/net/URLClassLoader;addURL(Ljava/net/URL;)V")
          .invoke(bukkitClassLoader, pluginFile.toURI().toURL());
      // pluginClassLoader.jar = new AkEmptyJarFile(pluginFile);
      BootstrapUtil
          .ofSet("Lorg/bukkit/plugin/java/PluginClassLoader;jar:Ljava/util/jar/JarFile;")
          .invoke(pluginClassLoader, new AkEmptyJarFile(pluginFile));
      pluginClassLoader.close();
      val akLoaderClass = Class.forName("org.inksnow.asteroid.AkLoader");
      // AkLoader.class.getClassLoader() == pluginClassLoader
      if (akLoaderClass.getClassLoader() == pluginClassLoader) {
        throw new IllegalArgumentException("AkLoader installed in same class loader");
      }
      // EnvironmentBukkit.install();
      BootstrapUtil
          .ofStatic("Lorg/inksnow/asteroid/bukkit/EnvironmentBukkit;install()V")
          .invoke();
    } catch (Throwable e) {
      logger.error("Failed to enable AkLoader", e);
      Runtime.getRuntime().halt(-1);
    }
  }
}
