package org.folio.mr.service.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.folio.mr.domain.BatchContext;
import org.folio.spring.scope.FolioExecutionContextSetter;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchFlowInitializer extends AbstractBatchRequestStage {

  private final BatchFlowHelper batchFlowHelper;

  @Override
  public void execute(BatchContext context) {
    try (var ignored = new FolioExecutionContextSetter(moduleMetadata, context.getOkapiHeaders())) {
      batchFlowHelper.prepareForFlowExecution(context);
    }
  }
}
