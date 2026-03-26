package org.folio.mr.domain.converter;

import org.folio.mr.domain.BatchRequestSplitStatus;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

public class BatchRequestSplitStatusJdbcType extends VarcharJdbcType {

  @Override
  public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
    return new CustomEnumBinder<>(javaType, this,
      enumObject -> ((BatchRequestSplitStatus) enumObject).getValue());
  }

  @Override
  public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
    return new CustomEnumExtractor<>(javaType, this, BatchRequestSplitStatus::fromValue);
  }

}
