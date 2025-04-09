package org.folio.mr.repository;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.mr.api.BaseIT;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@IntegrationTest
public class MediatedRequestsRepositoryTest extends BaseIT {

  @Autowired
  private MediatedRequestsRepository repository;

  @Test
  void caseInsensitiveSearchTest() {
    var request = buildMediatedRequestEntity(OPEN_NOT_YET_FILLED);
    request.setItemBarcode("Java");
    repository.save(request);
    var actual = repository.findByCql("itemBarcode==java", PageRequest.of(0, 1)).stream()
      .map(MediatedRequestEntity::getItemBarcode)
      .findFirst()
      .orElse("default");
    assertEquals("Java", actual);
  }
}
