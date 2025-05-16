package org.folio.mr.service;

import java.util.Collection;

import org.folio.mr.domain.entity.FakePatronLink;

public interface FakePatronLinkService {
  Collection<FakePatronLink> getFakePatronLinks(String realUserId);
}
