package org.folio.mr.service.flow.splits;

import static org.folio.mr.domain.BatchRequestSplitStatus.IN_PROGRESS;

import org.springframework.stereotype.Component;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchSplitFlowInitializer extends AbstractBatchSplitStage {

  private final MediatedBatchRequestSplitService batchRequestSplitService;

  @Override
  public void execute(BatchSplitContext context) {
    try (var ignored = new FolioExecutionContextSetter(moduleMetadata, context.getOkapiHeaders())) {
      var splitRequestId = context.getBatchSplitRequestId();
      batchRequestSplitService.updateStatusById(splitRequestId, IN_PROGRESS);
    }
  }
}
