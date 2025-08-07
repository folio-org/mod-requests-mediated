package org.folio.mr.service.impl;

import org.apache.commons.lang3.BooleanUtils;
import org.folio.mr.client.UserClient;
import org.folio.mr.client.UserGroupClient;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.service.UserService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserServiceImpl implements UserService {

  private final UserClient userClient;
  private final UserGroupClient userGroupClient;

  @Override
  public User fetchUser(String id) {
    log.info("fetchUser:: fetching user {}", id);
    return userClient.get(id).orElse(null);
  }

  @Override
  public UserGroup fetchUserGroup(String id) {
    log.info("fetchUserGroup:: fetching user group {}", id);
    return userGroupClient.get(id).orElse(null);
  }

  @Override
  public User create(User user) {
    log.info("create:: creating user {}", user.getId());
    return userClient.postUser(user);
  }

  @Override
  public boolean isInactive(String userId) {
    log.info("isInactive:: checking if user {} is active", userId);

    return userClient.get(userId)
      .map(User::getActive)
      .map(BooleanUtils::negate)
      .orElseGet(() -> {
        log.warn("isInactive:: user {} not found", userId);
        return true;
      });
  }
}
