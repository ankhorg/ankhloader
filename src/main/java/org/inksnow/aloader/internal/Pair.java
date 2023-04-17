package org.inksnow.aloader.internal;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.io.Serializable;

@Value
@AllArgsConstructor(staticName = "of")
public class Pair<K,V> implements Serializable{
    private K key;
    private V value;
 }

