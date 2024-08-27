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

  public <C, E> Collection<E> get(GetByQueryClient<C> client, Set<String> ids,
    Function<C, Collection<E>> responseTransformer) {

    return getAsStream(client, ids, responseTransformer)
      .toList();
  }

  public <C, E> Map<String, E> getMapped(GetByQueryClient<C> client, Set<String> ids,
    Function<C, Collection<E>> responseTransformer, Function<E, String> idExtractor) {

    return getAsStream(client, ids, responseTransformer)
      .collect(toMap(idExtractor, identity()));
  }

  private <C, E> Stream<E> getAsStream(GetByQueryClient<C> client, Set<String> ids,
    Function<C, Collection<E>> responseTransformer) {

    log.debug("getAsStream:: ids={}", ids);
    return Lists.partition(new ArrayList<>(ids), MAX_ID_COUNT)
      .stream()
      .map(BulkFetcher::toCqlQuery)
      .map(client::getByQuery)
      .map(responseTransformer)
      .flatMap(Collection::stream);
  }

  private static String toCqlQuery(List<String> ids) {
    return String.format("id==(%s)", Strings.join(ids, " or "));
  }

}
