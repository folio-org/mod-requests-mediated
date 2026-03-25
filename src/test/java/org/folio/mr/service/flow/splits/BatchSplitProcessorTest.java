package org.folio.mr.service.flow.splits;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.service.flow.EnvironmentType;
import org.folio.spring.FolioModuleMetadata;

@ExtendWith(MockitoExtension.class)
class BatchSplitProcessorTest {

  @InjectMocks private BatchSplitProcessor processor;
  @Mock private EcsRequestHelper ecsRequestHelper;
  @Mock private SingleTenantRequestHelper singleTenantRequestHelper;
  @Mock private SecureTenantRequestHelper secureTenantRequestHelper;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @Mock private BatchSplitContext context;

  @BeforeEach
  void setUp() {
    processor.setModuleMetadata(folioModuleMetadata);
  }

  @Test
  void execute_positive_ecsRequest() {
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.ECS);
    processor.execute(context);
    verify(ecsRequestHelper).createRequest(context);
  }

  @Test
  void execute_positive_singleTenantRequest() {
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.SINGLE_TENANT);
    processor.execute(context);
    verify(singleTenantRequestHelper).createRequest(context);
  }

  @Test
  void execute_positive_secureTenantRequest() {
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.SECURE_TENANT);
    processor.execute(context);
    verify(secureTenantRequestHelper).createRequest(context);
  }

  @Test
  void execute_negative_nullDeploymentEnvType() {
    when(context.getDeploymentEnvType()).thenReturn(null);
    assertThrows(NullPointerException.class, () -> processor.execute(context));
    verifyNoInteractions(secureTenantRequestHelper, ecsRequestHelper, singleTenantRequestHelper);
  }

  @Test
  void onError_positive() {
    var exception = new RuntimeException("Error");
    processor.onError(context, exception);
    verify(context).setExecutionError(exception);
  }
}
