package org.folio.mr.service;

import java.util.List;
import java.util.Optional;

import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.support.CqlQuery;

public interface UserService {
  User fetchUser(String id);
  UserGroup fetchUserGroup(String id);
  User create(User user);
  Optional<User> fetchUserByBarcode(String barcode);
  List<User> fetchUsers(CqlQuery query, int limit);
}
