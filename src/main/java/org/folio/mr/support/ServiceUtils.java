package org.folio.mr.support;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;
import org.folio.mr.domain.entity.Identifiable;

@UtilityClass
public class ServiceUtils {

  public static <E extends Identifiable<UUID>> void initId(E identifiable) {
    if (identifiable.getId() == null) {
      identifiable.setId(UUID.randomUUID());
    }
  }

  /**
   * Returns a stream from nullable collection.
   *
   * @param source - nullable {@link Collection} object
   * @param <T> - generic type for collection element
   * @return a stream from nullable collection
   */
  public static <T> Stream<T> toStream(Collection<T> source) {
    return emptyIfNull(source).stream();
  }

  /**
   * Converts elements in a nullable collection using mapper function.
   *
   * @param source - nullable {@link Collection} object
   * @param mapper - value mapper {@link Function} object
   * @param <T> - generic type of collection element
   * @param <R> - generic type for response list element
   * @return a list with converted items
   */
  public static <T, R> List<R> mapItems(Collection<T> source, Function<? super T, ? extends R> mapper) {
    return toStream(source).map(mapper).collect(toList());
  }
}
