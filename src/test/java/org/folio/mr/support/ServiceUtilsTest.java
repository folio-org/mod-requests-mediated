package org.folio.mr.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.junit.jupiter.api.Test;

class ServiceUtilsTest {

  @Test
  void shouldInitIdentifiableEntityId() {
    var randomId = UUID.randomUUID();
    var entity1 = MediatedBatchRequest.builder().id(randomId).build();
    var entity2 = new MediatedBatchRequestSplit();

    ServiceUtils.initId(entity1);
    ServiceUtils.initId(entity2);

    assertEquals(randomId, entity1.getId());
    assertNotNull(entity2.getId());
  }
}
