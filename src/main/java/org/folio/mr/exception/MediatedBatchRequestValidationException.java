package org.folio.mr.exception;

import java.util.List;
import java.util.UUID;
import org.folio.mr.domain.dto.Parameter;
import org.folio.mr.domain.type.ErrorCode;

public class MediatedBatchRequestValidationException extends ValidationException {

  private MediatedBatchRequestValidationException(ErrorCode code, Parameter... parameters) {
    super(code, parameters);
  }

  private MediatedBatchRequestValidationException(ErrorCode code, List<Parameter> parameters) {
    super(code, parameters);
  }

  public static MediatedBatchRequestValidationException requestExistsException(UUID batchId) {
    return new MediatedBatchRequestValidationException(ErrorCode.DUPLICATE_BATCH_REQUEST_ID,
      new Parameter().key("batchId").value(batchId.toString()));
  }

  public static MediatedBatchRequestValidationException invalidPickupServicePoint(String batchId, String servicePointId, String itemId) {
    return new MediatedBatchRequestValidationException(ErrorCode.INVALID_SERVICE_POINT_FOR_BATCH_REQUEST_ENTITY,
      new Parameter().key("servicePointId").value(servicePointId),
      new Parameter().key("batchId").value(batchId),
      new Parameter().key("itemId").value(itemId));
  }

  public static MediatedBatchRequestValidationException duplicateBatchRequestItems() {
    return new MediatedBatchRequestValidationException(ErrorCode.DUPLICATE_BATCH_REQUEST_ITEM_IDS, List.of());
  }

  public static MediatedBatchRequestValidationException itemsCountExceedMaxLimit(int requestedItemsCount, int maxAllowedItems) {
    return new MediatedBatchRequestValidationException(ErrorCode.BATCH_REQUEST_ITEM_IDS_COUNT_EXCEEDS_MAX_LIMIT,
      new Parameter().key("requestedItemsCount").value(String.valueOf(requestedItemsCount)),
      new Parameter().key("maxAllowedItems").value(String.valueOf(maxAllowedItems)));
  }
}
