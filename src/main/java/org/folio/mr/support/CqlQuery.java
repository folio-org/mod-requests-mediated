package org.folio.mr.support;

import static java.lang.String.format;

public record CqlQuery(String query) {
  public static CqlQuery exactMatch(String index, String value) {
    return new CqlQuery(format("%s==\"%s\"", index, value));
  }

  @Override
  public String toString() {
    return query;
  }
}
