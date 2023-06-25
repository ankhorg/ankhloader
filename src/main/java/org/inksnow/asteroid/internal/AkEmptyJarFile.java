package org.inksnow.asteroid.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class AkEmptyJarFile extends JarFile {
  public AkEmptyJarFile(File file) throws IOException {
    super(file);
  }

  @Override
  public JarEntry getJarEntry(String name) {
    return null;
  }

  @Override
  public ZipEntry getEntry(String name) {
    return null;
  }

  @Override
  public Enumeration<JarEntry> entries() {
    return Collections.emptyEnumeration();
  }

  @Override
  public Stream<JarEntry> stream() {
    return Stream.empty();
  }

  // @Override in java8+
  public Stream<JarEntry> versionedStream() {
    return Stream.empty();
  }

  @Override
  public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
    throw new FileNotFoundException(ze.getName());
  }

  @Override
  public Manifest getManifest() throws IOException {
    return super.getManifest();
  }
}
