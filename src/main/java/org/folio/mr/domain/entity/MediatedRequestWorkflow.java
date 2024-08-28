package org.folio.mr.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MediatedRequestWorkflow {
  PRIVATE_REQUEST("Private request");

  private final String value;
}
