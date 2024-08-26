package org.folio.mr.service;

import java.util.Map;
import java.util.Set;

import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;

public interface UserService {
  User fetchUser(String id);
  Map<String, User> fetchUsers(Set<String> ids);
  UserGroup fetchUserGroup(String id);
  Map<String, UserGroup> fetchUserGroups(Set<String> ids);
}
