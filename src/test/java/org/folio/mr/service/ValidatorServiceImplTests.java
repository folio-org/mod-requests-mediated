package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.exception.ValidationException;
import org.folio.mr.service.impl.ValidatorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidatorServiceImplTests {
  @InjectMocks
  private ValidatorServiceImpl validatorService;

  @Mock
  private UserService userService;

  @Test
  void validateRequesterForSaveShouldThrowExceptionWhenInactiveRequesterIsProvided() {
    MediatedRequest entity = new MediatedRequest();
    String requesterId = UUID.randomUUID().toString();
    entity.setRequesterId(requesterId);
    when(userService.isInactive(requesterId)).thenReturn(true);
    ValidationException ex = assertThrows(ValidationException.class,
      () -> validatorService.validateRequesterForSave(entity));
    assertEquals("Mediated request cannot be saved for inactive patron", ex.getMessage());
    assertEquals("MEDIATED_REQUEST_SAVE_NOT_ALLOWED_FOR_INACTIVE_PATRON",
      ex.getCode().getValue());
    assertEquals(1, ex.getParameters().size());
    assertEquals("requesterId", ex.getParameters().getFirst().getKey());
    assertEquals(requesterId, ex.getParameters().getFirst().getValue());
  }

  @Test
  void validateRequesterForSaveShouldNotThrowExceptionWhenActiveRequesterIsProvided() {
    MediatedRequest entity = new MediatedRequest();
    String requesterId = UUID.randomUUID().toString();
    entity.setRequesterId(requesterId);
    when(userService.isInactive(requesterId)).thenReturn(false);
    assertDoesNotThrow(() -> validatorService.validateRequesterForSave(entity));
  }

  @Test
  void validateRequesterForConfirmShouldThrowExceptionWhenInactiveRequesterIsProvided() {
    MediatedRequestEntity entity = new MediatedRequestEntity();
    UUID requesterId = UUID.randomUUID();
    entity.setRequesterId(requesterId);
    when(userService.isInactive(requesterId.toString())).thenReturn(true);
    ValidationException ex = assertThrows(ValidationException.class,
      () -> validatorService.validateRequesterForConfirm(entity));
    assertEquals(1, ex.getParameters().size());
    assertEquals("requesterId", ex.getParameters().getFirst().getKey());
    assertEquals(requesterId.toString(), ex.getParameters().getFirst().getValue());
    assertEquals("Mediated request cannot be confirmed for inactive patron", ex.getMessage());
    assertEquals("MEDIATED_REQUEST_CONFIRM_NOT_ALLOWED_FOR_INACTIVE_PATRON",
      ex.getCode().getValue());
  }

  @Test
  void validateRequesterForConfirmShouldNotThrowExceptionWhenActiveRequesterIsProvided() {
    MediatedRequestEntity entity = new MediatedRequestEntity();
    UUID requesterId = UUID.randomUUID();
    entity.setRequesterId(requesterId);
    when(userService.isInactive(requesterId.toString())).thenReturn(false);
    assertDoesNotThrow(() -> validatorService.validateRequesterForConfirm(entity));
  }
}
