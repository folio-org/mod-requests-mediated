package org.folio.mr.domain.mapper;

import java.util.List;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDtoItemRequestsInner;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueCheckStrategy;
import org.springframework.data.domain.Page;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface MediatedBatchRequestMapper {

  @Mapping(target = "id", source = "batchId")
  @Mapping(target = "status", expression = "java(org.folio.mr.domain.BatchRequestStatus.PENDING)")
  @Mapping(target = "requestDate", expression = "java(java.sql.Timestamp.from(java.time.Instant.now()))")
  MediatedBatchRequest mapPostDtoToEntity(MediatedBatchRequestPostDto dto);

  @Mapping(target = "status", expression = "java(org.folio.mr.domain.BatchRequestSplitStatus.PENDING)")
  MediatedBatchRequestSplit mapPostItemsDtoToSplitEntity(MediatedBatchRequestPostDtoItemRequestsInner dto);

  @Mapping(target = "batchId", source = "id")
  @Mapping(target = "mediatedRequestStatus", expression = "java(toMediatedRequestStatus(entity.getStatus()))")
  @Mapping(target = "metadata.createdDate", source = "createdDate")
  @Mapping(target = "metadata.createdByUserId", source="createdByUserId")
  @Mapping(target = "metadata.createdByUsername", source = "createdByUsername")
  @Mapping(target = "metadata.updatedDate", source = "updatedDate")
  @Mapping(target = "metadata.updatedByUserId", source="updatedByUserId")
  @Mapping(target = "metadata.updatedByUsername", source = "updatedByUsername")
  MediatedBatchRequestDto toDto(MediatedBatchRequest entity);

  @Mapping(target = "batchId", source = "mediatedBatchRequest.id")
  @Mapping(target = "mediatedRequestStatus", expression = "java(toDetailMediatedRequestStatus(entity.getStatus()))")
  @Mapping(target = "metadata.createdDate", source = "createdDate")
  @Mapping(target = "metadata.createdByUserId", source="createdByUserId")
  @Mapping(target = "metadata.createdByUsername", source = "createdByUsername")
  @Mapping(target = "metadata.updatedDate", source = "updatedDate")
  @Mapping(target = "metadata.updatedByUserId", source="updatedByUserId")
  @Mapping(target = "metadata.updatedByUsername", source = "updatedByUsername")
  MediatedBatchRequestDetailDto toDto(MediatedBatchRequestSplit entity);

  List<MediatedBatchRequestDto> toDtoList(Iterable<MediatedBatchRequest> batchRequestsIterable);

  List<MediatedBatchRequestDetailDto> toDetailsDtoList(Iterable<MediatedBatchRequestSplit> batchRequestDetails);

  default List<MediatedBatchRequestSplit> mapPostDtoToSplitEntities(MediatedBatchRequestPostDto batchDto) {
    return batchDto.getItemRequests().stream()
      .map(this::mapPostItemsDtoToSplitEntity)
      .toList();
  }

  default MediatedBatchRequestDto.MediatedRequestStatusEnum toMediatedRequestStatus(BatchRequestStatus status) {
    return MediatedBatchRequestDto.MediatedRequestStatusEnum.fromValue(status.getValue());
  }

  default MediatedBatchRequestsDto toMediatedBatchRequestsCollection(
    Page<MediatedBatchRequest> batchRequestsIterable) {
    var batchRequestsDtos = toDtoList(batchRequestsIterable.getContent());
    return new MediatedBatchRequestsDto(batchRequestsDtos, (int) batchRequestsIterable.getTotalElements());
  }

  default MediatedBatchRequestDetailsDto toMediatedBatchRequestDetailsCollection(
    Page<MediatedBatchRequestSplit> batchRequestDetails) {
    var batchRequestDetailsDtos = toDetailsDtoList(batchRequestDetails.getContent());
    return new MediatedBatchRequestDetailsDto(batchRequestDetailsDtos, (int) batchRequestDetails.getTotalElements());
  }

  default MediatedBatchRequestDetailDto.MediatedRequestStatusEnum toDetailMediatedRequestStatus(BatchRequestSplitStatus status) {
    return MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.fromValue(status.getValue());
  }
}
