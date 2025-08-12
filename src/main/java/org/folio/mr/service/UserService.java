package org.folio.mr.service;

import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;

public interface UserService {
  User fetchUser(String id);
  UserGroup fetchUserGroup(String id);
  User create(User user);
  boolean isInactive(String userId);
}
