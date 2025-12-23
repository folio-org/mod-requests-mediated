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
  DUPLICATE_BATCH_REQUEST_ID("103", "Mediated batch request with the given 'id' already exists."),
  INVALID_BATCH_REQUEST_INITIAL_STATUS("104", "Invalid initial status for mediated batch request."),
  INVALID_SERVICE_POINT_FOR_BATCH_REQUEST_ENTITY("105", "Not allowed to create Request for the given service point id."),
  DUPLICATE_BATCH_REQUEST_ITEM_IDS("106", "Mediated Batch Request contains duplicate item IDs."),
  BATCH_REQUEST_ITEM_IDS_COUNT_EXCEEDS_MAX_LIMIT("107", "The maximum number of items per batch request has been exceeded.");

  private final String code;
  private final String message;
}
