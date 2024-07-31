package org.folio.mr.domain.converter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.function.Function;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcType;

public class CustomEnumBinder<T> extends BasicBinder<T> {

  private final Function<Object, String> enumToStringConverterFunction;

  public CustomEnumBinder(JavaType<T> javaType, JdbcType jdbcType,
    Function<Object, String> enumToStringConverterFunction) {

    super(javaType, jdbcType);
    this.enumToStringConverterFunction = enumToStringConverterFunction;
  }

  @Override
  protected void doBind(PreparedStatement preparedStatement, Object enumObject, int index,
    WrapperOptions wrapperOptions) throws SQLException {

    preparedStatement.setObject(index, enumToStringConverterFunction.apply(enumObject),
      Types.OTHER);
  }

  @Override
  protected void doBind(CallableStatement callableStatement, Object enumObject, String paramName,
    WrapperOptions wrapperOptions) throws SQLException {

    callableStatement.setObject(paramName, enumToStringConverterFunction.apply(enumObject),
      Types.OTHER);
  }

}
