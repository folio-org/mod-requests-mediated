package org.folio.mr.service.flow.splits;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import org.folio.mr.domain.BatchSplitContext;
import org.folio.spring.scope.FolioExecutionContextSetter;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchSplitFlowInitializer extends AbstractBatchSplitStage {

  private final BatchSplitFlowHelper batchSplitFlowHelper;

  @Override
  public void execute(BatchSplitContext context) {
    try (var ignored = new FolioExecutionContextSetter(moduleMetadata, context.getOkapiHeaders())) {
      batchSplitFlowHelper.initializeFlowExecution(context);
    }
  }
}
