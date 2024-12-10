package org.folio.mr.service;

public interface CloningService<T> {
  T clone(T original);
}
