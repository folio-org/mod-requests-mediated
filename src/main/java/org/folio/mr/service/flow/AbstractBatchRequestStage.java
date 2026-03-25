package org.folio.mr.service.flow;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.folio.flow.api.Stage;
import org.folio.mr.domain.BatchContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;

@Slf4j
public abstract class AbstractBatchRequestStage implements Stage<BatchContext> {

  @Setter(onMethod_ = @Autowired)
  protected FolioModuleMetadata moduleMetadata;

  @Override
  public void onError(BatchContext context, Exception exception) {
    try (var ignored = new FolioExecutionContextSetter(moduleMetadata, context.getOkapiHeaders())) {
      var errorMessage = Optional.ofNullable(exception.getCause())
        .map(cause -> exception.getMessage() + ", cause: " + cause.getMessage())
        .orElse(exception.getMessage());

      context.setBatchRequestFailedMessage(errorMessage);
      log.error("onError:: Batch request with id: {} has failed with error: {}",
        context.getBatchRequestId(), errorMessage);
    }
  }
}
