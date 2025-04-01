package org.folio.mr.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.test.TestUtils.asJsonString;

import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.spring.integration.XOkapiHeaders;

import com.github.tomakehurst.wiremock.WireMockServer;

public class MockHelper {
  private static final String ITEM_SEARCH_URL = "/search/consortium/item";

  private final WireMockServer wireMockServer;

  public MockHelper(WireMockServer wireMockServer) {
    this.wireMockServer = wireMockServer;
  }

  public void mockItemSearch(String tenantId, String itemId, ConsortiumItem mockItem) {
    wireMockServer.stubFor(get(urlPathEqualTo(ITEM_SEARCH_URL + "/" + itemId))
      .withHeader(XOkapiHeaders.TENANT, equalTo(tenantId))
      .willReturn(okJson(asJsonString(mockItem))));
  }

}
