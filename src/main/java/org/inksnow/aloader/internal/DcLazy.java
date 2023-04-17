package org.inksnow.aloader.internal;

import java.util.concurrent.Callable;

public abstract class DcLazy<T> {
  private static final Object NO_INIT = new Object();

  @SuppressWarnings("unchecked")
  // Stores the managed object.
  private volatile T object = (T) NO_INIT;

  public static <T> DcLazy<T> of(Callable<T> supplier) {
    return new CallableInitializer<>(supplier);
  }

  public T get() {
    // use a temporary variable to reduce the number of reads of the
    // volatile field
    T result = object;

    if (result == NO_INIT) {
      synchronized (this) {
        result = object;
        if (result == NO_INIT) {
          object = result = callInitialize();
        }
      }
    }

    return result;
  }

  private <E extends Throwable> T callInitialize() throws E {
    try {
      return initialize();
    } catch (Throwable e) {
      throw (E) e;
    }
  }

  protected abstract T initialize() throws Throwable;

  private static class CallableInitializer<T> extends DcLazy<T> {
    private final Callable<T> supplier;

    private CallableInitializer(Callable<T> supplier) {
      this.supplier = supplier;
    }

    @Override
    protected T initialize() throws Throwable {
      return supplier.call();
    }
  }
}