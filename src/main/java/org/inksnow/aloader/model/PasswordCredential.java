package org.inksnow.aloader.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@Value
@lombok.Builder(toBuilder = true)
@AllArgsConstructor(staticName = "of")
public class PasswordCredential {
  private static final PasswordCredential empty = new PasswordCredential(null, null);

  @Getter @lombok.Builder.Default
  private final String username = null;
  @Getter @lombok.Builder.Default
  private final String password = null;

  public static PasswordCredential empty() {
    return PasswordCredential.empty;
  }

  @Override
  public PasswordCredential clone() {
    return new PasswordCredential(this.username, this.password);
  }

  public static class Builder {
    private Builder() {
      //
    }
  }
}
