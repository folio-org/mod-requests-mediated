package org.folio.mr.client;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Service
@Log4j2
public class BulkFetcher {
  private static final int MAX_ID_COUNT = 80;

  public <T> List<T> get(GetByQueryClient<T> client, Set<String> ids) {
    return getAsStream(client, ids)
      .toList();
  }

  public <T> Map<String, T> getMapped(GetByQueryClient<T> client, Set<String> ids,
    Function<T, String> idExtractor) {

    return getAsStream(client, ids)
      .collect(toMap(idExtractor, identity()));
  }

  private <T> Stream<T> getAsStream(GetByQueryClient<T> client, Set<String> ids) {
    log.debug("getAsStream:: ids={}", ids);
    return Lists.partition(new ArrayList<>(ids), MAX_ID_COUNT)
      .stream()
      .map(BulkFetcher::toCqlQuery)
      .map(client::getByQuery)
      .flatMap(Collection::stream);
  }

  private static String toCqlQuery(List<String> ids) {
    return String.format("id==(%s)", Strings.join(ids, " or "));
  }

}
