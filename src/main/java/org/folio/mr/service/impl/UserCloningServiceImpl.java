package org.folio.mr.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserType;
import org.folio.mr.service.UserService;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserCloningServiceImpl {

  private final UserService userService;

  public User cloneUser(User original, String originalTenantId) {
    String id = original.getId();
    log.info("clone:: looking for user {} ", id);
    try {
      User user = userService.fetchUser(id);
      log.info("clone:: user {} already exists", id);
      return user;
    } catch (FeignException.NotFound e) {
      log.info("clone:: user {} not found, creating it", id);
      User clone1 = buildClone(original, originalTenantId);
      User clone = userService.create(clone1);
      log.info("clone:: user {} created", id);
      return clone;
    }
  }

  private User buildClone(User original, String originalTenantId) {
    User clone = new User()
      .id(original.getId())
      .personal(original.getPersonal())
      .patronGroup(original.getPatronGroup())
      .type(UserType.SHADOW.getValue())
      .barcode(original.getBarcode())
      .customFields(Map.of("originaltenantid", originalTenantId))
      .active(true);
    log.debug("buildClone:: result: {}", () -> clone);
    return clone;
  }
}
