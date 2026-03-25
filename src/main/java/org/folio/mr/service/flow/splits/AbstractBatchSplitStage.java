package org.folio.mr.service.flow.splits;

import org.folio.flow.api.Stage;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.spring.FolioModuleMetadata;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractBatchSplitStage implements Stage<BatchSplitContext> {

  @Setter(onMethod_ = @Autowired)
  protected FolioModuleMetadata moduleMetadata;

  @Override
  public void onError(BatchSplitContext context, Exception exception) {
    context.setExecutionError(exception);
  }
}
