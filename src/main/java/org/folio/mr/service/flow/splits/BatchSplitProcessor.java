package org.folio.mr.service.flow.splits;

import static org.folio.mr.domain.RequestLevel.ITEM;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import org.folio.mr.client.EcsExternalTlrClient;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.dto.EcsRequestExternal;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.exception.ItemNotFoundException;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.folio.mr.service.SearchService;
import org.folio.mr.service.flow.EnvironmentType;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.service.SystemUserScopedExecutionService;

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
