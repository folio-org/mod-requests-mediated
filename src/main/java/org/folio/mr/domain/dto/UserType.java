package org.folio.mr.domain.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum UserType {
  SHADOW("shadow");

  private final String value;
}
