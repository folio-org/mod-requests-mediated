package org.folio.mr.service.impl;

import static org.folio.mr.support.CqlQuery.exactMatch;

import java.util.Optional;

import org.folio.mr.client.UserClient;
import org.folio.mr.client.UserGroupClient;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.service.UserService;
import org.folio.mr.support.CqlQuery;
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
    Optional<User> user = userClient.get(id);
    log.info("fetchUser:: user found: {}", user.isPresent());
    return user.orElse(null);
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
  public Optional<User> fetchUserByBarcode(String barcode) {
    log.info("fetchUserByBarcode:: fetching user by barcode {}", barcode);
    Optional<User> result = userClient.getByQuery(exactMatch("barcode", barcode), 1)
      .getUsers()
      .stream()
      .findFirst();
    log.info("fetchUserByBarcode:: user found: {}", result.isPresent());
    return result;
  }
}
