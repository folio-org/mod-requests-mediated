package org.folio.mr.support;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
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

  @Test
  void toStream_positive() {
    var actual = ServiceUtils.toStream(List.of(1, 2)).toList();
    assertThat(actual).containsExactly(1, 2);
  }

  @Test
  void toStream_positive_nullValue() {
    var actual = ServiceUtils.toStream(null).toList();
    assertThat(actual).isEmpty();
  }

  @Test
  void toStream_positive_emptyCollection() {
    var actual = ServiceUtils.toStream(emptyList()).toList();
    assertThat(actual).isEmpty();
  }

  @Test
  void mapItems_positive() {
    var actual = ServiceUtils.mapItems(List.of(1, 2, 3), String::valueOf);
    assertThat(actual).isEqualTo(List.of("1", "2", "3"));
  }
}
