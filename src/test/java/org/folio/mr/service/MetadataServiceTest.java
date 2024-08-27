package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Metadata;
import org.folio.mr.exception.MetadataUpdateException;
import org.folio.mr.service.impl.MetadataServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {
  private static final UUID USER_ID = UUID.randomUUID();
  @Mock
  private FolioExecutionContext context;
  @InjectMocks
  private MetadataServiceImpl metadataService;

  @Test
  void metadataIsCreated() {
    when(context.getUserId()).thenReturn(USER_ID);
    MediatedRequest object = new MediatedRequest();
    assertNull(object.getMetadata());
    metadataService.updateMetadata(object);
    Metadata metadata = object.getMetadata();
    assertNotNull(metadata);
    assertNotNull(metadata.getCreatedDate());
    assertNotNull(metadata.getUpdatedDate());
    assertEquals(metadata.getCreatedDate(), metadata.getUpdatedDate());
    assertEquals(USER_ID.toString(), metadata.getCreatedByUserId());
    assertEquals(USER_ID.toString(), metadata.getUpdatedByUserId());
  }

  @Test
  void metadataIsUpdated() {
    when(context.getUserId()).thenReturn(USER_ID);
    String oldUserId = UUID.randomUUID().toString();
    Date oldDate = Date.from(new Date().toInstant().minusSeconds(99));

    MediatedRequest object = new MediatedRequest()
      .metadata(new Metadata()
        .createdDate(oldDate)
        .updatedDate(oldDate)
        .createdByUserId(oldUserId)
        .updatedByUserId(oldUserId));

    metadataService.updateMetadata(object);
    Metadata metadata = object.getMetadata();
    assertEquals(metadata.getCreatedDate(), oldDate);
    assertTrue(metadata.getUpdatedDate().after(metadata.getCreatedDate()));
    assertEquals(oldUserId, metadata.getCreatedByUserId());
    assertEquals(USER_ID.toString(), metadata.getUpdatedByUserId());
  }

  @Test
  void metadataUpdateFailsWhenObjectDoesNotSupportMetadata() {
    Object object = new Object();
    MetadataUpdateException exception = assertThrows(MetadataUpdateException.class,
      () -> metadataService.updateMetadata(object));
    assertNotNull(exception.getCause());
    assertInstanceOf(NoSuchMethodException.class, exception.getCause());
  }

}