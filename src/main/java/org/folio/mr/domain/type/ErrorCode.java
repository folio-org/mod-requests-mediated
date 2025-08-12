package org.folio.mr.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  MEDIATED_REQUEST_SAVE_NOT_ALLOWED_FOR_INACTIVE_PATRON(
    "MEDIATED_REQUEST_SAVE_NOT_ALLOWED_FOR_INACTIVE_PATRON"),
  MEDIATED_REQUEST_CONFIRM_NOT_ALLOWED_FOR_INACTIVE_PATRON(
    "MEDIATED_REQUEST_CONFIRM_NOT_ALLOWED_FOR_INACTIVE_PATRON");

  private final String value;
}
