package org.inksnow.asteroid;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class AkResourceMapEntry implements Comparable<AkResourceMapEntry> {
  private final int priority;
  private final String from;
  private final String to;

  @Override
  public int compareTo(AkResourceMapEntry o) {
    return Integer.compare(o.priority, priority);
  }
}
