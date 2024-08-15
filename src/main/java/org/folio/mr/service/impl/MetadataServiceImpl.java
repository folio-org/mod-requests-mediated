package org.folio.mr.service.impl;

import java.lang.reflect.Method;
import java.util.Date;

import org.folio.mr.domain.dto.Metadata;
import org.folio.mr.service.MetadataService;
import org.folio.mr.util.HttpUtils;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@AllArgsConstructor
@Log4j2
public class MetadataServiceImpl implements MetadataService {
//  private final FolioExecutionContext context;

  @Override
  public <T> T updateMetadata(T obj) {
    try {
      final Method getMetadata = obj.getClass().getMethod("getMetadata");
      final Metadata existingMetadata = (Metadata) getMetadata.invoke(obj);
      final Date now = new Date();
      final String userId = HttpUtils.getUserIdFromToken().orElse(null);
      final String username = HttpUtils.getUsernameFromToken().orElse(null);

      if (existingMetadata != null) {
        log.info("updateMetadata:: updating metadata");
        existingMetadata.updatedDate(now)
          .updatedByUserId(userId)
          .updatedByUsername(username);
        log.info("updateMetadata:: metadata updated");
      } else {
        log.info("updateMetadata:: initializing metadata");
        Method setMetadata = obj.getClass().getMethod("setMetadata", Metadata.class);
        Metadata newMetadata = new Metadata()
          .createdDate(now)
          .createdByUserId(userId)
          .createdByUsername(username)
          .updatedDate(now)
          .updatedByUserId(userId)
          .updatedByUsername(username);
        setMetadata.invoke(obj, newMetadata);
        log.info("updateMetadata:: metadata initialized");
      }
    } catch (Exception e) {
      log.error("updateMetadata:: failed to update metadata", e);
      throw new RuntimeException(e);
    }
    return obj;
  }

}
