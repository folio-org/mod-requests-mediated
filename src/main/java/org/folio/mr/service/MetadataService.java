package org.folio.mr.service;

import java.util.List;

public interface MetadataService {
  <T> T updateMetadata(T obj);
  <T> void updateMetadata(List<T> objects);
}
