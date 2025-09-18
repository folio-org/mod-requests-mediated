package org.folio.mr.support;

import static org.folio.mr.domain.type.ErrorCode.DUPLICATE_BATCH_REQUEST_ID;
import static org.folio.mr.domain.type.ErrorCode.UNKNOWN_CONSTRAINT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

public class DatabaseConstraintTranslatorTest {

  @Test
  void translate_knownConstraint_returnsMappedErrorCode() {
    var cve = mockConstraintViolationException("pk_batch_request");
    var errorCode = DatabaseConstraintTranslator.translate(cve);
    assertEquals(DUPLICATE_BATCH_REQUEST_ID, errorCode);
  }

  @Test
  void translate_unknownConstraint_returnsUnknownErrorCode() {
    var cve = mockConstraintViolationException("some_other_constraint");
    var errorCode = DatabaseConstraintTranslator.translate(cve);
    assertEquals(UNKNOWN_CONSTRAINT, errorCode);
  }

  private ConstraintViolationException mockConstraintViolationException(String constraintName) {
    return new ConstraintViolationException("msg", null, constraintName);
  }
}
