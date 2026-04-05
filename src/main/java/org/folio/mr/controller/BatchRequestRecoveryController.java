package org.folio.mr.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.mr.controller.delegate.BatchRequestsServiceDelegate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequiredArgsConstructor
public class BatchRequestRecoveryController {

  private final BatchRequestsServiceDelegate batchRequestsService;

  /**
   * Restores stale batch requests.
   *
   * <p>It could happen if service or flow engine were interrupted during execution:
   * <ul>
   *   <li>Module has been restarted due to maintenance</li>
   *   <li>Module has been restarted due to hosting issues</li>
   *   <li>Module has been restarted due to internal errors, for example {@link OutOfMemoryError}</li>
   *   <li>Flow engine thread(s) stopped due to library errors</li>
   *   </ul>
   * </p>
   */
  @PostMapping("/batch-mediated-requests-recovery")
  public void recoverBatchMediatedRequests() {
    batchRequestsService.recoverStaleBatchRequests();
  }
}
