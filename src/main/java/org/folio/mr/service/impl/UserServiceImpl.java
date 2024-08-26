package org.folio.mr.service.impl;

import java.util.Map;
import java.util.Set;

import org.folio.mr.client.BulkFetcher;
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
  private final BulkFetcher bulkFetcher;

  @Override
  public User fetchUser(String id) {
    log.info("fetchUser:: fetching user {}", id);
    return userClient.get(id);
  }

  @Override
  public Map<String, User> fetchUsers(Set<String> ids) {
    log.info("fetchUsers:: fetching {} users by IDs", ids::size);
    return bulkFetcher.getMapped(userClient, ids, User::getId);
  }

  @Override
  public UserGroup fetchUserGroup(String id) {
    log.info("fetchUserGroup:: fetching user group {}", id);
    return userGroupClient.get(id);
  }

  @Override
  public Map<String, UserGroup> fetchUserGroups(Set<String> ids) {
    log.info("fetchUserGroups:: fetching {} user groups by IDs", ids::size);
    return bulkFetcher.getMapped(userGroupClient, ids, UserGroup::getId);
  }
}
