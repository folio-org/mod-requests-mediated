package org.folio.mr.service.impl;

import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;

public interface UserService {
  User fetchUser(String id);
  UserGroup fetchUserGroup(String id);
}
