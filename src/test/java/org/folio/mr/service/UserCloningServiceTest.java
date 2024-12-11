package org.folio.mr.service;

import feign.FeignException;
import org.folio.mr.domain.dto.User;
import org.folio.mr.service.impl.UserCloningServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCloningServiceTest {

  @Mock
  private UserService userService;

  @InjectMocks
  private UserCloningServiceImpl userCloningService;

  @Test
  void testCloneWhenUserExists() {
    // given
    User user = new User().id(UUID.randomUUID().toString());
    when(userService.fetchUser(user.getId())).thenReturn(user);

    // when
    userCloningService.cloneUser(user, "tenantId");

    // then
    verifyNoMoreInteractions(userService);
  }

  @Test
  void testCloneWhenUserNotFound() {
    // given
    User user = new User().id(UUID.randomUUID().toString());
    when(userService.fetchUser(user.getId())).thenThrow(FeignException.NotFound.class);
    when(userService.create(any())).thenReturn(user);

    // when
    userCloningService.cloneUser(user, "tenantId");

    // then
    verifyNoMoreInteractions(userService);
  }

}
