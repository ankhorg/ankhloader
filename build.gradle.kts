import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.java

plugins {
  id("java")
  id("java-library")
  id("maven-publish")
}

allprojects {
  apply(plugin = "java")
  apply(plugin = "java-library")
  apply(plugin = "maven-publish")

  group = "org.inksnow.asteroid"
  version = "1.0-SNAPSHOT"

  repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://r.irepo.space/maven/")
  }

  dependencies {
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withSourcesJar()
    withJavadocJar()
  }

  afterEvaluate {
    publishing {
      repositories {
        System.getenv().forEach{
          logger.error("{}: {}", it.key, it.value)
        }
        if (System.getenv("CI")?.isNotEmpty() == true) {
          if (project.version.toString().endsWith("-SNAPSHOT")) {
            maven("https://repo.inker.bot/repository/maven-snapshots/") {
              credentials {
                username = System.getenv("NEXUS_USERNAME")
                password = System.getenv("NEXUS_PASSWORD")
              }
            }
          } else {
            maven("https://repo.inker.bot/repository/maven-releases/") {
              credentials {
                username = System.getenv("NEXUS_USERNAME")
                password = System.getenv("NEXUS_PASSWORD")
              }
            }
            maven("https://s0.blobs.inksnow.org/maven/") {
              credentials {
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
              }
            }
          }
        } else {
          maven(rootProject.buildDir.resolve("publish"))
        }
      }

      publications {
        create<MavenPublication>("mavenJar") {
          artifactId = project.path
            .removePrefix(":")
            .replace(':', '-')
            .ifEmpty { "core" }

          pom {
            name.set("Asteroid Package Tool ${project.name}")
            description.set("A bukkit plugin loader named Asteroid Package Tool, use to load bukkit plugin from maven")
            url.set("https://github.com/ankhorg/asteroid")
            properties.set(mapOf())
            licenses {
              license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
              }
            }
            developers {
              developer {
                id.set("inkerbot")
                name.set("InkerBot")
                email.set("im@inker.bot")
              }
            }
            scm {
              connection.set("scm:git:git://github.com/ankhorg/asteroid.git")
              developerConnection.set("scm:git:ssh://github.com/ankhorg/asteroid.git")
              url.set("https://github.com/ankhorg/asteroid")
            }
          }

          if(project.ext.has("publishAction")){
            (project.ext["publishAction"] as Action<MavenPublication>)(this)
          }else{
            from(components["java"])
          }
        }
      }
    }
  }

  tasks.javadoc {
    options.encoding = "UTF-8"
  }

  tasks.compileJava {
    options.encoding = "UTF-8"
  }
}

dependencies {
  val mavenResolverVersion = "1.9.10"
  val mavenVersion = "3.9.2"
  val handleModuleDependency = Action<ExternalModuleDependency> {
    exclude(group = "org.slf4j", module = "slf4j-api")
    exclude(group = "org.slf4j", module = "jcl-over-slf4j")
  }

  api("org.apache.maven.resolver:maven-resolver-api:$mavenResolverVersion", handleModuleDependency)
  api("org.apache.maven.resolver:maven-resolver-spi:$mavenResolverVersion", handleModuleDependency)
  api("org.apache.maven.resolver:maven-resolver-util:$mavenResolverVersion", handleModuleDependency)
  api("org.apache.maven.resolver:maven-resolver-impl:$mavenResolverVersion", handleModuleDependency)
  api(
    "org.apache.maven.resolver:maven-resolver-connector-basic:$mavenResolverVersion",
    handleModuleDependency
  )
  api(
    "org.apache.maven.resolver:maven-resolver-transport-file:$mavenResolverVersion",
    handleModuleDependency
  )
  api(
    "org.apache.maven.resolver:maven-resolver-transport-http:$mavenResolverVersion",
    handleModuleDependency
  )
  api("org.apache.maven:maven-resolver-provider:$mavenVersion", handleModuleDependency)

  api("org.slf4j:slf4j-api:1.7.36")
  implementation("org.slf4j:jcl-over-slf4j:1.7.36")

  // gson
  api("com.google.code.gson:gson:2.10.1")

  // glob
  api("com.hrakaroo:glob:0.9.0")

  // compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
  // compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")

  api("org.ow2.asm:asm:9.5")
  // compileOnly("bot.inker.ulmc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
  // compileOnly("bot.inker.ulmc.paper:paper-server:1.19.4-R0.1-SNAPSHOT:mojang-mapped")
}