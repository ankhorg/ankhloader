package org.inksnow.asteroid;

import com.hrakaroo.glob.MatchingEngine;
import lombok.val;
import org.inksnow.asteroid.internal.AkCompoundEnumeration;
import org.inksnow.asteroid.internal.AkIOUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class AkClassLoader extends URLClassLoader {
  static {
    ClassLoader.registerAsParallelCapable();
  }

  private static final ClassLoader nullClassRef = new ClassLoader(null) {};
  private final Set<AkClassDelegateEntry> delegateEntry = new TreeSet<>();
  private final Set<AkResourceMapEntry> resourceMapEntry = new TreeSet<>();

  public AkClassLoader() {
    super(new URL[0], null);

    addDelegate(100, this, null, null);
  }

  public void addDelegate(AkClassDelegateEntry entry) {
    delegateEntry.add(entry);
  }

  public void addDelegate(int priority, ClassLoader classLoader, List<MatchingEngine> whitelist, List<MatchingEngine> blacklist) {
    addDelegate(new AkClassDelegateEntry(priority, classLoader, whitelist, blacklist));
  }

  public void addResourceMap(AkResourceMapEntry entry) {
    resourceMapEntry.add(entry);
  }

  public void addResourceMap(int priority, String from, String to) {
    addResourceMap(new AkResourceMapEntry(priority, from, to));
  }

  @Override
  public void addURL(URL url) {
    super.addURL(url);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      Class<?> c = findLoadedClass(name);
      if (c == null) {
        c = loadClassImpl(name);
      }
      if (c == null) {
        throw new ClassNotFoundException(name);
      }
      if (resolve) {
        resolveClass(c);
      }
      return c;
    }
  }

  private Class<?> loadClassImpl(String name) {
    val path = name.replace('.', '/');
    // load parents
    loopEntry:
    for (val parentEntry : delegateEntry) {
      // handle whitelist
      if (parentEntry.whitelist() != null) {
        boolean whiteListFound = false;
        for (val whiteListEntry : parentEntry.whitelist()) {
          if (whiteListEntry.matches(path)) {
            whiteListFound = true;
            break;
          }
        }
        if (!whiteListFound) {
          continue;
        }
      }
      // handle blacklist
      if (parentEntry.blacklist() != null) {
        for (val blackListEntry : parentEntry.blacklist()) {
          if (blackListEntry.matches(path)) {
            continue loopEntry;
          }
        }
      }
      // try load
      try {
        val entryClassLoader = parentEntry.classLoader();
        if (entryClassLoader == this) {
          return findClass(name);
        } else if (entryClassLoader == null) {
          return nullClassRef.loadClass(name);
        } else {
          return parentEntry.classLoader().loadClass(name);
        }
      } catch (ClassNotFoundException e) {
        //
      }
    }

    return null;
  }

  @Override
  protected Class<?> findClass(final String name)
      throws ClassNotFoundException
  {
    val url = findResource(name.replace('.', '/').concat(".class"));
    if(url == null){
      throw new ClassNotFoundException(name);
    }
    try {
      byte[] b;
      try(val in = url.openStream()) {
        b = AkIOUtils.readAllBytes(in);
      }
      return defineClass(name, b, 0, b.length, new CodeSource(url, (Certificate[]) null));
    } catch (IOException e) {
      throw new ClassNotFoundException(name, e);
    }
  }

  @Override
  public Enumeration<URL> getResources(String path) throws IOException {
    final Enumeration<URL>[] result = new Enumeration[delegateEntry.size()];
    int resultPtr = 0;

    // load parents
    loopEntry:
    for (val parentEntry : delegateEntry) {
      // handle whitelist
      if (parentEntry.whitelist() != null) {
        boolean whiteListFound = false;
        for (val whiteListEntry : parentEntry.whitelist()) {
          if (whiteListEntry.matches(path)) {
            whiteListFound = true;
            break;
          }
        }
        if (!whiteListFound) {
          continue;
        }
      }
      // handle blacklist
      if (parentEntry.blacklist() != null) {
        for (val blackListEntry : parentEntry.blacklist()) {
          if (blackListEntry.matches(path)) {
            continue loopEntry;
          }
        }
      }
      // try load
      val entryClassLoader = parentEntry.classLoader();
      if (entryClassLoader == this) {
        try {
          result[resultPtr++] = findResources(path);
        }catch (IOException e){
          //
        }
      } else if (entryClassLoader == null) {
        try {
          result[resultPtr++] = nullClassRef.getResources(path);
        }catch (IOException e){
          //
        }
      } else {
        try {
          result[resultPtr++] = parentEntry.classLoader().getResources(path);
        }catch (IOException e){
          //
        }
      }
    }

    return new AkCompoundEnumeration<>(result);
  }

  @Override
  public URL getResource(String name) {
    // load parents
    loopEntry:
    for (val parentEntry : delegateEntry) {
      // handle whitelist
      if (parentEntry.whitelist() != null) {
        boolean whiteListFound = false;
        for (val whiteListEntry : parentEntry.whitelist()) {
          if (whiteListEntry.matches(name)) {
            whiteListFound = true;
            break;
          }
        }
        if (!whiteListFound) {
          continue;
        }
      }
      // handle blacklist
      if (parentEntry.blacklist() != null) {
        for (val blackListEntry : parentEntry.blacklist()) {
          if (blackListEntry.matches(name)) {
            continue loopEntry;
          }
        }
      }
      // try load
      val entryClassLoader = parentEntry.classLoader();
      if (entryClassLoader == this) {
        val resource = findResource(name);
        if(resource != null){
          return resource;
        }
      } else if (entryClassLoader == null) {
        val resource = nullClassRef.getResource(name);
        if(resource != null){
          return resource;
        }
      } else {
        val resource = parentEntry.classLoader().getResource(name);
        if(resource != null){
          return resource;
        }
      }
    }

    return null;
  }

  private String mapResourceName(String name) {
    for (val entry : resourceMapEntry) {
      if (name.startsWith(entry.from())) {
        return entry.to() + name.substring(entry.from().length());
      }
    }
    return name;
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    return super.findResources(mapResourceName(name));
  }

  @Override
  public URL findResource(String name) {
    return super.findResource(mapResourceName(name));
  }
}
