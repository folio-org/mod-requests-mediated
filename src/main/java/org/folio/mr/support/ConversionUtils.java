package org.folio.mr.support;

import java.util.Optional;
import java.util.UUID;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConversionUtils {

  public static String asString(UUID uuid) {
    return Optional.ofNullable(uuid)
      .map(UUID::toString)
      .orElse(null);
  }

}
