package org.inksnow.aloader.internal;

public class SneakyThrowUtil {
  private SneakyThrowUtil() {
    throw new UnsupportedOperationException();
  }

  public static RuntimeException sneakyThrow(Throwable e) {
    return (RuntimeException) e;
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable, R> R sneakyThrowImpl(Throwable e) throws T {
    throw (T) e;
  }
}
