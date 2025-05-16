package org.folio.mr.service.impl;

import java.util.Collection;
import java.util.UUID;

import org.folio.mr.domain.entity.FakePatronLink;
import org.folio.mr.repository.FakePatronLinkRepository;
import org.folio.mr.service.FakePatronLinkService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class FakePatronLinkServiceImpl implements FakePatronLinkService {

  private final FakePatronLinkRepository fakePatronLinkRepository;

  @Override
  public Collection<FakePatronLink> getFakePatronLinks(String realUserId) {
    log.info("getFakePatronLink:: fetching fake patron links for real user {}", realUserId);
    Collection<FakePatronLink> links = fakePatronLinkRepository.findByUserId(UUID.fromString(realUserId));
    log.info("getFakePatronLink:: {} fake patron links found", links::size);
    return links;
  }
}
