package org.inksnow.asteroid;

import com.hrakaroo.glob.MatchingEngine;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class AkClassDelegateEntry implements Comparable<AkClassDelegateEntry> {
  private final int priority;
  private final ClassLoader classLoader;
  private final List<MatchingEngine> whitelist;
  private final List<MatchingEngine> blacklist;

  @Override
  public int compareTo(AkClassDelegateEntry o) {
    return Integer.compare(o.priority, priority);
  }
}