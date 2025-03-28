package org.folio.mr.domain;

import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedRequest;

public record MediatedRequestContext(MediatedRequest request, Item item) {
}
