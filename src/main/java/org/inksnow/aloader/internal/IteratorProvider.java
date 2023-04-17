package org.inksnow.aloader.internal;

import java.util.Iterator;

public class IteratorProvider<T> implements Iterable<T> {
    private final Iterator<T> iterator;
    private boolean loaded = false;

    public IteratorProvider(Iterator<T> iterator) {
      this.iterator = iterator;
    }

    @Override
    public synchronized Iterator<T> iterator() {
      if(this.loaded){
        throw new IllegalStateException("iterator load twice");
      }
      this.loaded = true;
      return iterator;
    }
  }