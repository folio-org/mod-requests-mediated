package org.folio.mr.support;

import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.mr.domain.entity.Identifiable;

@UtilityClass
public class ServiceUtils {

  public static <E extends Identifiable<UUID>> void initId(E identifiable) {
    if (identifiable.getId() == null) {
      identifiable.setId(UUID.randomUUID());
    }
  }
}
