package org.folio.mr.domain.context;

import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class MediatedRequestContext {
  private final MediatedRequest request;
  private User requester;
  private UserGroup requesterGroup;
  private User proxy;
  private UserGroup proxyGroup;
  private Item item;
  private Instance instance;
  private ServicePoint pickupServicePoint;
  private Location location;
  private Library library;
}
