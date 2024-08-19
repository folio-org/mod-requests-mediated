package org.folio.mr.domain.context;

import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
@Builder
public class MediatedRequestContext {
  private final MediatedRequest request;
  private final User requester;
  private final UserGroup requesterGroup;
  private final User proxy;
  private final UserGroup proxyGroup;
  private final Item item;
  private final Instance instance;
  private final ServicePoint pickupServicePoint;
  private final Location location;
  private final Library library;
}
