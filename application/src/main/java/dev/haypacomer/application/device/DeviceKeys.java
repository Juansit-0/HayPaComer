package dev.haypacomer.application.device;

import dev.haypacomer.application.auth.OpaqueTokens;
import java.util.Objects;

final class DeviceKeys {

  static final String PREFIX = "hpc_dev_";

  private final OpaqueTokens opaqueTokens;

  DeviceKeys(OpaqueTokens opaqueTokens) {
    this.opaqueTokens = Objects.requireNonNull(opaqueTokens, "opaqueTokens");
  }

  String generate() {
    return PREFIX + opaqueTokens.generate();
  }

  String hash(String rawKey) {
    return opaqueTokens.hash(rawKey);
  }
}
