package org.folio.mr.service;

public interface InterimServicePointService {

  /**
   * Ensures that the Interim service point exists in the Central tenant. Duplicate-ID/already-exists
   * responses are treated as success; any other error is propagated.
   */
  void createInterimServicePoint();
}
