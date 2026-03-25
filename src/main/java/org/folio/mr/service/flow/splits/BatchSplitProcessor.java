package org.folio.mr.service.flow.splits;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import org.folio.mr.domain.BatchSplitContext;
import org.folio.spring.scope.FolioExecutionContextSetter;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchSplitProcessor extends AbstractBatchSplitStage {

  private final EcsRequestHelper ecsRequestHelper;
  private final SingleTenantRequestHelper singleTenantRequestHelper;
  private final SecureTenantRequestHelper secureTenantRequestHelper;

  @Override
  @SuppressWarnings("UnnecessaryDefault")
  public void execute(BatchSplitContext context) {
    try (var ignored = new FolioExecutionContextSetter(moduleMetadata, context.getOkapiHeaders())) {
      var deploymentEnvType = context.getDeploymentEnvType();
      var requestExecutor = switch (deploymentEnvType) {
        case ECS -> ecsRequestHelper;
        case SINGLE_TENANT -> singleTenantRequestHelper;
        case SECURE_TENANT -> secureTenantRequestHelper;
        default -> throw new IllegalStateException("Unexpected value: " + deploymentEnvType);
      };

      requestExecutor.createRequest(context);
    }
  }
}
