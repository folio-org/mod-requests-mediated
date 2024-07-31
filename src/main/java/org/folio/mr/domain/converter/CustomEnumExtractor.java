package org.folio.mr.domain.converter;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

public class CustomEnumExtractor<T> extends BasicExtractor<T> {

  private final Function<String, Object> stringToEnumConverterFunction;

  public CustomEnumExtractor(JavaType<T> javaType, JdbcType jdbcType,
    Function<String, Object> stringToEnumConverterFunction) {

    super(javaType, jdbcType);
    this.stringToEnumConverterFunction = stringToEnumConverterFunction;
  }

  @Override
  protected T doExtract(ResultSet resultSet, int i, WrapperOptions wrapperOptions) throws SQLException {
    return this.getJavaType().wrap(stringToEnumConverterFunction.apply(
      resultSet.getObject(i).toString()), wrapperOptions);
  }

  @Override
  protected T doExtract(CallableStatement callableStatement, int i, WrapperOptions wrapperOptions) throws SQLException {
    return this.getJavaType().wrap(stringToEnumConverterFunction.apply(
      callableStatement.getObject(i).toString()), wrapperOptions);
  }

  @Override
  protected T doExtract(CallableStatement callableStatement, String s, WrapperOptions wrapperOptions) throws SQLException {
    return this.getJavaType().wrap(stringToEnumConverterFunction.apply(
      callableStatement.getObject(s).toString()), wrapperOptions);
  }

}
