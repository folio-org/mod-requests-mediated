package org.folio.mr.service.impl;

import org.folio.mr.client.UserClient;
import org.folio.mr.client.UserGroupClient;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
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
    return userClient.get(id);
  }

  @Override
  public UserGroup fetchUserGroup(String id) {
    log.info("fetchUserGroup:: fetching user group {}", id);
    return userGroupClient.get(id);
  }
}
