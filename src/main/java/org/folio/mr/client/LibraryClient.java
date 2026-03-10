package org.folio.mr.client;

import org.folio.mr.domain.dto.Libraries;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "location-units/libraries")
public interface LibraryClient extends GetByQueryParamsClient<Libraries> {
}
