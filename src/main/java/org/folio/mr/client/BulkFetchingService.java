package org.folio.mr.client;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
public class BulkFetchingService {
  private static final int MAX_IDS_PER_QUERY = 80;

  public <C, E> Collection<E> fetch(GetByQueryClient<C> client, Set<String> ids,
    Function<C, Collection<E>> collectionExtractor) {

    return getAsStream(client, ids, collectionExtractor)
      .toList();
  }

  public <C, E> Map<String, E> fetch(GetByQueryClient<C> client, Set<String> ids,
    Function<C, Collection<E>> collectionExtractor, Function<E, String> keyMapper) {

    return getAsStream(client, ids, collectionExtractor)
      .collect(toMap(keyMapper, identity()));
  }

  // TODO: make async, but mind the warning about FolioExecutionContext in async code:
  // https://github.com/folio-org/folio-spring-support#execution-context
  private <C, E> Stream<E> getAsStream(GetByQueryClient<C> client, Set<String> ids,
    Function<C, Collection<E>> collectionExtractor) {

    log.debug("getAsStream:: ids={}", ids);
    return Lists.partition(new ArrayList<>(ids), MAX_IDS_PER_QUERY)
      .parallelStream()
      .map(BulkFetchingService::toCqlQuery)
      .map(client::getByQuery)
      .map(collectionExtractor)
      .flatMap(Collection::stream);
  }

  private static String toCqlQuery(List<String> ids) {
    return String.format("id==(%s)", Strings.join(ids, " or "));
  }

}
