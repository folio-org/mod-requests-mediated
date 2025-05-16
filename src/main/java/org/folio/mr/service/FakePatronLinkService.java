package org.folio.mr.service;

import java.util.Optional;

import org.folio.mr.domain.entity.FakePatronLink;

public interface FakePatronLinkService {
  Optional<FakePatronLink> getFakePatronLink(String realUserId);
}
