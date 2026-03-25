package org.folio.mr.service.flow;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.folio.mr.domain.BatchContext;
import org.folio.spring.scope.FolioExecutionContextSetter;

@Component
@RequiredArgsConstructor
public class BatchFlowFinalizer extends AbstractBatchRequestStage {

  private final BatchFlowHelper batchFlowHelper;

  @Override
  public void execute(BatchContext context) {
    try (var ignored = new FolioExecutionContextSetter(moduleMetadata, context.getOkapiHeaders())) {
      batchFlowHelper.finalizeFlowExecution(context);
    }
  }
}
