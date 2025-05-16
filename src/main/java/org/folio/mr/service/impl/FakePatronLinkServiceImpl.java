package org.folio.mr.service.impl;

import java.util.Optional;
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
  public Optional<FakePatronLink> getFakePatronLink(String realUserId) {
    log.info("getFakePatronLink:: fetching fake patron link for real user {}", realUserId);
    Optional<FakePatronLink> result = fakePatronLinkRepository.findByUserId(UUID.fromString(realUserId));
    log.info("getFakePatronLink:: fake patron link found: {}", result::isPresent);
    return result;
  }
}
