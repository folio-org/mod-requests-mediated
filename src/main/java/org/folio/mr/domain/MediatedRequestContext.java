package org.folio.mr.domain;

import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedRequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public final class MediatedRequestContext {
  private final MediatedRequest request;
  private Item item;
  private String lendingTenantId;
}
