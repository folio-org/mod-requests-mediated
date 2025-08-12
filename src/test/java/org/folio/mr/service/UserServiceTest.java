package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.folio.mr.client.UserClient;
import org.folio.mr.domain.dto.User;
import org.folio.mr.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserServiceImpl userService;

  @Mock
  private UserClient userClient;

  @Test
  void getById() {
    when(userClient.get(any()))
      .thenReturn(Optional.of(new User()));

    var userIsInactive = userService.isInactive(UUID.randomUUID().toString());
    verify(userClient).get(any());
    assertTrue(userIsInactive);
  }
}
