package org.folio.mr.domain.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDtoItemRequestsInner;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;


class MediatedBatchRequestMapperTest {

  private final MediatedBatchRequestMapper mapper = new MediatedBatchRequestMapperImpl();

  @Test
  void shouldMapPostDtoToEntity() {
    var batchId = UUID.randomUUID();
    var postDto = new MediatedBatchRequestPostDto().batchId(batchId.toString());
    var entity = mapper.mapPostDtoToEntity(postDto);

    assertEquals(batchId, entity.getId());
    assertEquals(BatchRequestStatus.PENDING, entity.getStatus());
    assertNotNull(entity.getRequestDate());
  }

  @Test
  void shouldMapPostItemsDtoToSplitEntity() {
    var itemId = UUID.randomUUID();
    var itemDto = new MediatedBatchRequestPostDtoItemRequestsInner().itemId(itemId.toString());
    var splitEntity = mapper.mapPostItemsDtoToSplitEntity(itemDto);

    assertEquals(BatchRequestSplitStatus.PENDING, splitEntity.getStatus());
    assertEquals(itemId, splitEntity.getItemId());
  }

  @Test
  void shouldMapBatchRequestToDto() {
    var batchId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var username = "user";
    var entity = new MediatedBatchRequest();
    entity.setId(batchId);
    entity.setStatus(BatchRequestStatus.PENDING);
    entity.setCreatedDate(Timestamp.from(Instant.now()));
    entity.setCreatedByUserId(userId);
    entity.setCreatedByUsername(username);
    entity.setUpdatedDate(Timestamp.from(Instant.now()));
    entity.setUpdatedByUserId(userId);
    entity.setUpdatedByUsername(username);

    var dto = mapper.toDto(entity);

    assertEquals(batchId.toString(), dto.getBatchId());
    assertEquals(MediatedBatchRequestDto.MediatedRequestStatusEnum.PENDING, dto.getMediatedRequestStatus());
    assertEquals(userId.toString(), dto.getMetadata().getCreatedByUserId());
    assertEquals(username, dto.getMetadata().getCreatedByUsername());
    assertEquals(userId.toString(), dto.getMetadata().getUpdatedByUserId());
    assertEquals(username, dto.getMetadata().getUpdatedByUsername());
  }

  @Test
  void shouldMapToDtoDetail() {
    var batchId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var username = "user";
    var batchEntity = new MediatedBatchRequest();
    batchEntity.setId(batchId);
    var splitEntity = new MediatedBatchRequestSplit();
    splitEntity.setMediatedBatchRequest(batchEntity);
    splitEntity.setStatus(BatchRequestSplitStatus.PENDING);
    splitEntity.setCreatedDate(Timestamp.from(Instant.now()));
    splitEntity.setCreatedByUserId(userId);
    splitEntity.setCreatedByUsername(username);
    splitEntity.setUpdatedDate(Timestamp.from(Instant.now()));
    splitEntity.setUpdatedByUserId(userId);
    splitEntity.setUpdatedByUsername(username);

    var detailDto = mapper.toDto(splitEntity);

    assertEquals(batchId.toString(), detailDto.getBatchId());
    assertEquals(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.PENDING, detailDto.getMediatedRequestStatus());
    assertEquals(userId.toString(), detailDto.getMetadata().getCreatedByUserId());
    assertEquals(username, detailDto.getMetadata().getCreatedByUsername());
    assertEquals(userId.toString(), detailDto.getMetadata().getUpdatedByUserId());
    assertEquals(username, detailDto.getMetadata().getUpdatedByUsername());
  }

  @Test
  void shouldMapToBatchRequestDtoList() {
    var entity1 = new MediatedBatchRequest();
    entity1.setId(UUID.randomUUID());
    entity1.setStatus(BatchRequestStatus.PENDING);
    var entity2 = new MediatedBatchRequest();
    entity2.setId(UUID.randomUUID());
    entity2.setStatus(BatchRequestStatus.PENDING);

    var dtoList = mapper.toDtoList(List.of(entity1, entity2));

    assertEquals(2, dtoList.size());
    assertThat(dtoList)
      .extracting(MediatedBatchRequestDto::getBatchId)
        .containsExactly(entity1.getId().toString(), entity2.getId().toString());
  }

  @Test
  void shouldMapToDetailsDtoList() {
    var batchEntity = new MediatedBatchRequest();
    batchEntity.setId(UUID.randomUUID());
    var split1 = new MediatedBatchRequestSplit();
    split1.setMediatedBatchRequest(batchEntity);
    split1.setStatus(BatchRequestSplitStatus.PENDING);
    var split2 = new MediatedBatchRequestSplit();
    split2.setMediatedBatchRequest(batchEntity);
    split2.setStatus(BatchRequestSplitStatus.PENDING);

    var detailsList = mapper.toDetailsDtoList(List.of(split1, split2));

    assertThat(detailsList.size()).isEqualTo(2);
    assertThat(detailsList)
      .extracting(MediatedBatchRequestDetailDto::getBatchId)
      .containsExactly(batchEntity.getId().toString(), batchEntity.getId().toString());
  }

  @Test
  void shouldMapPostDtoToSplitEntities() {
    var itemId1 = UUID.randomUUID();
    var itemId2 = UUID.randomUUID();
    var item1 = new MediatedBatchRequestPostDtoItemRequestsInner().itemId(itemId1.toString());
    var item2 = new MediatedBatchRequestPostDtoItemRequestsInner().itemId(itemId2.toString());
    var postDto = new MediatedBatchRequestPostDto().itemRequests(List.of(item1, item2));

    var splitEntities = mapper.mapPostDtoToSplitEntities(postDto);

    assertThat(splitEntities.size()).isEqualTo(2);
    assertThat(splitEntities)
      .extracting(MediatedBatchRequestSplit::getItemId)
      .containsExactly(itemId1, itemId2);
  }

  @Test
  void shouldMapToMediatedRequestStatus() {
    var status = mapper.toMediatedRequestStatus(BatchRequestStatus.PENDING);

    assertEquals(MediatedBatchRequestDto.MediatedRequestStatusEnum.PENDING, status);
  }

  @Test
  void shouldMapToMediatedBatchRequestsCollection() {
    var batchId1 = UUID.randomUUID();
    var batchId2 = UUID.randomUUID();
    var entity1 = new MediatedBatchRequest();
    entity1.setId(batchId1);
    entity1.setStatus(BatchRequestStatus.PENDING);
    var entity2 = new MediatedBatchRequest();
    entity2.setId(batchId2);
    entity2.setStatus(BatchRequestStatus.PENDING);

    var page = Mockito.mock(Page.class);
    Mockito.when(page.getContent()).thenReturn(List.of(entity1, entity2));
    Mockito.when(page.getTotalElements()).thenReturn(2L);

    var collection = mapper.toMediatedBatchRequestsCollection(page);

    assertThat(collection.getTotalRecords()).isEqualTo(2);
    assertThat(collection.getMediatedBatchRequests())
      .extracting(MediatedBatchRequestDto::getBatchId)
      .containsExactly(batchId1.toString(), batchId2.toString());
  }

  @Test
  void shouldMapToMediatedBatchRequestDetailsCollection() {
    var batchId = UUID.randomUUID();
    var batchEntity = new MediatedBatchRequest();
    batchEntity.setId(batchId);
    var split1 = new MediatedBatchRequestSplit();
    split1.setMediatedBatchRequest(batchEntity);
    split1.setStatus(BatchRequestSplitStatus.PENDING);
    var split2 = new MediatedBatchRequestSplit();
    split2.setMediatedBatchRequest(batchEntity);
    split2.setStatus(BatchRequestSplitStatus.PENDING);

    var page = Mockito.mock(Page.class);
    Mockito.when(page.getContent()).thenReturn(List.of(split1, split2));
    Mockito.when(page.getTotalElements()).thenReturn(2L);

    var detailsCollection = mapper.toMediatedBatchRequestDetailsCollection(page);

    assertThat(detailsCollection.getTotalRecords()).isEqualTo(2);
    assertThat(detailsCollection.getMediatedBatchRequestDetails())
      .hasSize(2)
      .extracting(MediatedBatchRequestDetailDto::getBatchId)
      .containsExactly(batchId.toString(), batchId.toString());
  }

  @Test
  void shouldMapToDetailMediatedRequestStatus() {
    var status = mapper.toDetailMediatedRequestStatus(BatchRequestSplitStatus.PENDING);

    assertEquals(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.PENDING, status);
  }
}
