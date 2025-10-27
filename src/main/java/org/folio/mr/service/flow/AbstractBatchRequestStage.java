package org.folio.mr.service.flow;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.folio.flow.api.Stage;
import org.folio.mr.domain.BatchContext;

@Slf4j
public abstract class AbstractBatchRequestStage implements Stage<BatchContext> {

  @Override
  public void onError(BatchContext context, Exception exception) {
    var errorMessage = Optional.ofNullable(exception.getCause())
      .map(cause -> exception.getMessage() + ", cause: " + cause.getMessage())
      .orElse(exception.getMessage());

    context.setBatchRequestFailedMessage(errorMessage);
    log.error("onError:: Batch request with id: {} has failed with error: {}",
      context.getBatchRequestId(), errorMessage);
  }
}
