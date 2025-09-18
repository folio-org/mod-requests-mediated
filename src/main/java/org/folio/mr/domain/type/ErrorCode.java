package org.folio.mr.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  UNKNOWN_CONSTRAINT("-1", "Unknown constraint."),
  MEDIATED_REQUEST_SAVE_NOT_ALLOWED_FOR_INACTIVE_PATRON("100",
    "Mediated request cannot be saved for inactive patron"),
  MEDIATED_REQUEST_CONFIRM_NOT_ALLOWED_FOR_INACTIVE_PATRON("102",
    "Mediated request cannot be confirmed for inactive patron"),
  DUPLICATE_BATCH_REQUEST_ID("103", "Mediated batch request with the given 'id' already exists.");

  private final String code;
  private final String message;
}
