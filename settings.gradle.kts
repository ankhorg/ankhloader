rootProject.name = "ankhloader"

buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }

  dependencies {
    classpath("com.github.johnrengelman:shadow:8.1.1")
    classpath("com.guardsquare:proguard-gradle:7.3.2")
  }
}
include("test-plugin")
include("bukkit")
