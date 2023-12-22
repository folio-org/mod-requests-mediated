package org.folio.mr.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecureRequestsApiTest extends BaseIT {
  private static final String URI_TEMPLATE_REQUESTS = "/secure-requests/";

  @Test
  void getSecureRequestByIdNotFound() throws Exception {
    mockMvc.perform(
        get(URI_TEMPLATE_REQUESTS + UUID.randomUUID())
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }
}
