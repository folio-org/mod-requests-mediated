package org.folio.mr.support;

import static org.folio.mr.domain.type.ErrorCode.DUPLICATE_BATCH_REQUEST_ID;
import static org.folio.mr.domain.type.ErrorCode.UNKNOWN_CONSTRAINT;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.folio.mr.domain.type.ErrorCode;
import org.hibernate.exception.ConstraintViolationException;

@UtilityClass
public class DatabaseConstraintTranslator {

  private static final Map<String, ErrorCode> DB_CONSTRAINTS_I18N_MAP = Map.of(
    "pk_batch_request", DUPLICATE_BATCH_REQUEST_ID
  );

  public static ErrorCode translate(ConstraintViolationException cve) {
    return DB_CONSTRAINTS_I18N_MAP.getOrDefault(cve.getConstraintName(), UNKNOWN_CONSTRAINT);
  }
}
