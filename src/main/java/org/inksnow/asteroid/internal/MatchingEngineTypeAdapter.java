package org.inksnow.asteroid.internal;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.hrakaroo.glob.GlobPattern;
import com.hrakaroo.glob.MatchingEngine;
import lombok.val;

import java.io.IOException;

public class MatchingEngineTypeAdapter extends TypeAdapter<MatchingEngine> {
  @Override
  public void write(JsonWriter out, MatchingEngine value) throws IOException {
    out.nullValue();
  }

  @Override
  public MatchingEngine read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      return null;
    } else {
      return GlobPattern.compile(in.nextString());
    }
  }
}
