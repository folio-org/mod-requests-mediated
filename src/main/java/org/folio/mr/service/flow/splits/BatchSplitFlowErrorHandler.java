package org.folio.mr.service.flow.splits;

import org.springframework.stereotype.Component;
import org.folio.flow.api.Stage;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchSplitFlowErrorHandler implements Stage<BatchSplitContext> {

  private final FolioModuleMetadata moduleMetadata;
  private final BatchSplitFlowHelper batchSplitFlowHelper;

  @Override
  public void execute(BatchSplitContext context) {
    try (var ignored = new FolioExecutionContextSetter(moduleMetadata, context.getOkapiHeaders())) {
      batchSplitFlowHelper.handleExecutionError(context);
    } catch (Exception error) {
      log.error("Failed to finalize batch split flow for: {}", context.getBatchSplitRequestId(), error);
    }
  }
}
