package org.folio.mr.domain.converter;

import org.folio.mr.domain.MediatedRequestStatus;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;

public class MediatedRequestStatusJdbcType extends PostgreSQLEnumJdbcType {
  @Override
  public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
    return new CustomEnumBinder<>(javaType, this,
      enumObject -> ((MediatedRequestStatus) enumObject).getValue());
  }

  @Override
  public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
    return new CustomEnumExtractor<>(javaType, this, MediatedRequestStatus::fromValue);
  }
}
