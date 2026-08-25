package org.folio.mr.controller;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.controller.delegate.BatchRequestsServiceDelegate;

@ExtendWith(MockitoExtension.class)
class BatchRequestRecoveryControllerTest {

  @InjectMocks private BatchRequestRecoveryController controller;
  @Mock private BatchRequestsServiceDelegate batchRequestsService;

  @Test
  void shouldRecoverBatchMediatedRequests() {
    controller.recoverBatchMediatedRequests();
    verify(batchRequestsService).recoverStaleBatchRequests();
  }
}
