package org.folio.mr.domain.converter;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

public class CustomEnumExtractor<T> extends BasicExtractor<T> {

  private final transient Function<String, Object> stringToEnumConverterFunction;

  public CustomEnumExtractor(JavaType<T> javaType, JdbcType jdbcType,
    Function<String, Object> stringToEnumConverterFunction) {

    super(javaType, jdbcType);
    this.stringToEnumConverterFunction = stringToEnumConverterFunction;
  }

  @Override
  protected T doExtract(ResultSet resultSet, int index, WrapperOptions wrapperOptions)
    throws SQLException {

    return Optional.ofNullable(resultSet.getObject(index))
      .map(Object::toString)
      .map(stringToEnumConverterFunction)
      .map(obj -> this.getJavaType().wrap(obj, wrapperOptions))
      .orElse(null);
  }

  @Override
  protected T doExtract(CallableStatement callableStatement, int index,
    WrapperOptions wrapperOptions) throws SQLException {

    return this.getJavaType().wrap(stringToEnumConverterFunction.apply(
      callableStatement.getObject(index).toString()), wrapperOptions);
  }

  @Override
  protected T doExtract(CallableStatement callableStatement, String name,
    WrapperOptions wrapperOptions) throws SQLException {

    return this.getJavaType().wrap(stringToEnumConverterFunction.apply(
      callableStatement.getObject(name).toString()), wrapperOptions);
  }

}
