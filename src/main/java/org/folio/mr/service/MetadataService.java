package org.folio.mr.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.folio.mr.domain.dto.Metadata;

public interface MetadataService {
//  void initMetadata(Consumer<Metadata> metadataConsumer);
  <T> T updateMetadata(T obj);
}
