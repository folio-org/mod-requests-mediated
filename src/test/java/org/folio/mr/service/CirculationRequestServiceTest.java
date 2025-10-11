package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.mr.client.CirculationClient;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.service.impl.CirculationRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CirculationRequestServiceTest {

  private CirculationClient circulationClient;
  private CirculationRequestServiceImpl service;

  @BeforeEach
  void setUp() {
    circulationClient = mock(CirculationClient.class);
    service = new CirculationRequestServiceImpl(circulationClient);
  }

  @Test
  void get_positive_shouldReturnRequest() {
    var id = "123";
    var expected = mock(Request.class);
    when(circulationClient.getRequest(id)).thenReturn(expected);

    var result = service.get(id);

    assertSame(expected, result);
    verify(circulationClient).getRequest(id);
  }

  @Test
  void create_positive_shouldCreateRequest() {
    var request = mock(Request.class);
    var expected = mock(Request.class);
    when(circulationClient.createRequest(request)).thenReturn(expected);

    var result = service.create(request);

    assertSame(expected, result);
    verify(circulationClient).createRequest(request);
  }

  @Test
  void update_positive_shouldUpdate() {
    var request = mock(Request.class);
    var id = "id-1";
    when(request.getId()).thenReturn(id);
    var expected = mock(Request.class);
    when(circulationClient.updateRequest(id, request)).thenReturn(expected);

    var result = service.update(request);

    assertSame(expected, result);
    verify(circulationClient).updateRequest(id, request);
  }

  @Test
  void getItemRequestAllowedServicePoints_positive_shouldReturnAllowedServicePoints() {
    var requesterId = UUID.randomUUID();
    var itemId = UUID.randomUUID();
    var expected = mock(CirculationClient.AllowedServicePoints.class);

    when(circulationClient.allowedServicePointsByItem(
      requesterId.toString(), "create", itemId.toString()))
      .thenReturn(expected);

    var result = service.getItemRequestAllowedServicePoints(requesterId, itemId);

    assertSame(expected, result);
    verify(circulationClient).allowedServicePointsByItem(
      requesterId.toString(), "create", itemId.toString());
  }

  @Test
  void getItemRequestAllowedServicePoints_negative_shouldThrowOnNullArgs() {
    var itemId = UUID.randomUUID();
    assertThrows(IllegalArgumentException.class,
      () -> service.getItemRequestAllowedServicePoints(null, itemId));
    var requesterId = UUID.randomUUID();
    assertThrows(IllegalArgumentException.class,
      () -> service.getItemRequestAllowedServicePoints(requesterId, null));
  }
}
