package org.inksnow.aloader.model;

import lombok.*;
import lombok.extern.java.Log;
import org.apache.maven.api.model.Model;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.v4.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.inksnow.aloader.internal.DcLazy;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.logging.Level;

@Log
@Value
@lombok.Builder(toBuilder = true)
@AllArgsConstructor(staticName = "of")
public class Repository {
  private static final String HEADER_USER_AGENT = "User-Agent";
  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String ANKH_LOADER_UA = "AnkhLoader/1.0(im@inker.bot)";
  private static final String BASIC_AUTH = "Basic";

  private static final Repository mavenCentral = maven("https://repo1.maven.org/maven2/");
  private static final Repository mavenLocal = flatDir(locateMavenLocal());
  private static final Repository google = maven("https://dl.google.com/dl/android/maven2/");

  @Getter
  private final String type;
  @Getter
  private final String url;
  @lombok.Builder.Default @Getter
  private final PasswordCredential credential = PasswordCredential.empty();

  private final DcLazy<String> baseUrl = DcLazy.of(this::createBaseUrl);

  private final DcLazy<String> authorization = DcLazy.of(this::createAuthorization);

  private String createBaseUrl(){
    int end = url.length();
    while ((end != 0) && ("/".indexOf(url.charAt(end - 1)) != -1)) {
      end--;
    }
    return url.substring(0, end);
  }

  private String createAuthorization(){
    if(credential.username() == null || credential.password() == null){
      return null;
    }
    val authPayload = credential.username() + ":" + credential.password();
    val encodedAuth = Base64.getEncoder().encodeToString(authPayload.getBytes(StandardCharsets.UTF_8));
    return BASIC_AUTH + " " + encodedAuth;
  }

  @SneakyThrows
  public Reader openReader(String rawUrl) {
    val url = new URL(rawUrl);
    val connection = url.openConnection();
    connection.setRequestProperty(HEADER_USER_AGENT, ANKH_LOADER_UA);
    if(authorization.get() != null){
      connection.setRequestProperty(HEADER_AUTHORIZATION, authorization.get());
    }
    connection.connect();
    return new InputStreamReader(connection.getInputStream());
  }

  public Metadata resolve(String groupId, String artifactId) {
    String metadataUrl = baseUrl.get() + "/" +
        groupId.replace('.', '/') + "/" +
        artifactId + "/" + "maven-metadata.xml";
    try(Reader reader = openReader(metadataUrl)) {
      return new MetadataXpp3Reader().read(reader);
    } catch (IOException e) {
      logger.log(Level.FINE, "Failed to request metadata in " + metadataUrl, e);
      return null;
    } catch (XmlPullParserException e) {
      logger.log(Level.SEVERE, "Failed to parse metadata in " + metadataUrl, e);
      return null;
    }
  }

  public Model resolve(String groupId, String artifactId, String version) {
    String pomUrl = baseUrl.get() + "/" +
        groupId.replace('.', '/') + "/" +
        artifactId + "/" +
        version + "/" +
        artifactId + "-" + version + ".pom";
    try(Reader reader = openReader(pomUrl)) {
      return new MavenXpp3Reader().read(reader);
    } catch (IOException e) {
      logger.log(Level.FINE, "Failed to request pom in " + pomUrl, e);
      return null;
    } catch (XmlPullParserException e) {
      logger.log(Level.SEVERE, "Failed to parse pom in " + pomUrl, e);
      return null;
    }
  }

  private static File locateMavenLocal() {
    String localOverride = System.getProperty("maven.repo.local");
    if (localOverride != null) {
      return new File(localOverride).getAbsoluteFile();
    } else {
      return new File(System.getProperty("user.home"), ".m2").getAbsoluteFile();
    }
  }

  public static Repository mavenCentral() {
    return mavenCentral;
  }

  public static Repository mavenCentral(Consumer<Builder> action) {
    Builder builder = mavenCentral.toBuilder();
    action.accept(builder);
    return builder.build();
  }

  public static Repository mavenLocal() {
    return mavenLocal;
  }

  public static Repository mavenLocal(Consumer<Builder> action) {
    Builder builder = mavenCentral.toBuilder();
    action.accept(builder);
    return builder.build();
  }

  public static Repository google() {
    return google;
  }

  public static Repository google(Consumer<Builder> action) {
    Builder builder = mavenCentral.toBuilder();
    action.accept(builder);
    return builder.build();
  }

  public static Repository flatDir(Path directory) {
    return flatDir(directory, null);
  }

  public static Repository flatDir(Path directory, Consumer<Builder> action) {
    return maven(directory.toUri(), action);
  }

  public static Repository flatDir(File directory) {
    return flatDir(directory, null);
  }

  public static Repository flatDir(File directory, Consumer<Builder> action) {
    return maven(directory.toURI(), action);
  }

  public static Repository maven(URI uri) {
    return maven(uri, null);
  }

  @SneakyThrows
  public static Repository maven(URI uri, Consumer<Builder> action) {
    return maven(uri.toURL(), null);
  }

  public static Repository maven(URL url) {
    return maven(url, null);
  }

  public static Repository maven(URL url, Consumer<Builder> action) {
    return maven(url.toString(), null);
  }

  public static Repository maven(String url) {
    return maven(url, null);
  }

  public static Repository maven(String url, Consumer<Builder> action) {
    Builder builder = builder().type("maven").url(url);
    if (action != null) {
      action.accept(builder);
    }
    return builder.build();
  }

  @Override
  public Repository clone() {
    return new Repository(this.type, this.url, this.credential);
  }

  public static class Builder {
    private Builder() {
      //
    }
  }
}
