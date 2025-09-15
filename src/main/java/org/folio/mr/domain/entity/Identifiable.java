package org.folio.mr.domain.entity;

public interface Identifiable<T> {

  T getId();

  void setId(T id);
}
