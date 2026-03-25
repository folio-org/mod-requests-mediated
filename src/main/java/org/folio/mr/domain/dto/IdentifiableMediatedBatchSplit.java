package org.folio.mr.domain.dto;

import java.util.UUID;

/**
 * Holder class for {@link MediatedBatchRequestDetailDto}, providing identifier of entity within
 *
 * @param id                   - entity identifier
 * @param mediatedBatchRequest - dto object
 */
public record IdentifiableMediatedBatchSplit(
  UUID id, MediatedBatchRequestDetailDto mediatedBatchRequest) {
}
