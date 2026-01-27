package org.folio.mr.service.impl;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.mr.domain.dto.Metadata;
import org.folio.mr.exception.ExceptionFactory;
import org.folio.mr.service.MetadataService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@AllArgsConstructor
@Log4j2
public class MetadataServiceImpl implements MetadataService {
  private final FolioExecutionContext context;

  @Override
  public <T> T updateMetadata(T obj) {
    try {
      final Method getMetadata = obj.getClass().getMethod("getMetadata");
      final Metadata existingMetadata = (Metadata) getMetadata.invoke(obj);
      final Date now = new Date();
      final String userId = context.getUserId().toString();

      if (existingMetadata != null) {
        log.info("updateMetadata:: updating metadata");
        existingMetadata.updatedDate(now).updatedByUserId(userId);
        log.info("updateMetadata:: metadata updated");
      } else {
        log.info("updateMetadata:: initializing metadata");
        Method setMetadata = obj.getClass().getMethod("setMetadata", Metadata.class);
        Metadata newMetadata = new Metadata()
          .createdDate(now)
          .createdByUserId(userId)
          .updatedDate(now)
          .updatedByUserId(userId);
        setMetadata.invoke(obj, newMetadata);
      }
    } catch (Exception e) {
      log.error("updateMetadata:: failed to update metadata", e);
      throw ExceptionFactory.unexpectedError("failed to update metadata", e);
    }
    return obj;
  }

  @Override
  public <T> void updateMetadata(List<T> objects) {
    objects.forEach(this::updateMetadata);
  }
}
