package org.folio.mr.service.impl;

import static org.folio.mr.domain.type.ErrorCode.MEDIATED_REQUEST_CONFIRM_NOT_ALLOWED_FOR_INACTIVE_PATRON;
import static org.folio.mr.domain.type.ErrorCode.MEDIATED_REQUEST_SAVE_NOT_ALLOWED_FOR_INACTIVE_PATRON;

import java.util.List;
import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Parameter;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.exception.ValidationException;
import org.folio.mr.service.UserService;
import org.folio.mr.service.ValidatorService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ValidatorServiceImpl implements ValidatorService {
  private final UserService userService;

  @Override
  public void validateRequesterForSave(MediatedRequest mediatedRequest) {
    log.info("validateRequesterForSave:: validating requester {}", mediatedRequest::getRequesterId);

    String requesterId = mediatedRequest.getRequesterId();

    if (requesterId == null || requesterId.isBlank()) {
      log.warn("validateRequesterForSave:: " +
        "Requester ID is missing (should be validated by schema)");
      return;
    }

    if (userService.isInactive(requesterId)) {
      log.warn("validateRequesterForSave:: {}", MEDIATED_REQUEST_SAVE_NOT_ALLOWED_FOR_INACTIVE_PATRON.getMessage());
      throw new ValidationException(
        MEDIATED_REQUEST_SAVE_NOT_ALLOWED_FOR_INACTIVE_PATRON,
        List.of(new Parameter().key("requesterId").value(requesterId)));
    }
  }

  @Override
  public void validateRequesterForConfirm(MediatedRequestEntity mediatedRequest) {
    log.info("validateRequesterForConfirm:: validating requester {}",
      mediatedRequest::getRequesterId);

    UUID requesterIdUUID = mediatedRequest.getRequesterId();
    if (requesterIdUUID == null) {
      log.warn("validateRequesterForConfirm:: " +
        "Requester ID is missing (should be validated by schema)");
      return;
    }

    String requesterId = requesterIdUUID.toString();
    if (userService.isInactive(requesterId)) {
      log.warn("validateRequesterForConfirm:: {}", MEDIATED_REQUEST_CONFIRM_NOT_ALLOWED_FOR_INACTIVE_PATRON.getMessage());
      throw new ValidationException(
        MEDIATED_REQUEST_CONFIRM_NOT_ALLOWED_FOR_INACTIVE_PATRON,
        List.of(new Parameter().key("requesterId").value(requesterId)));
    }
  }
}

