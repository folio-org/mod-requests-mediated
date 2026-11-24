package org.folio.mr.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.folio.mr.client.ServicePointClient;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.service.ConsortiumService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class InterimServicePointServiceImplTest {

  @Mock private ServicePointClient servicePointClient;
  @Mock private ConsortiumService consortiumService;
  @Mock private SystemUserScopedExecutionService executionService;
  @Mock private FolioExecutionContext context;
  @InjectMocks private InterimServicePointServiceImpl service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    when(context.getTenantId()).thenReturn("member");
    when(consortiumService.getCentralTenantId("member")).thenReturn(Optional.of("central"));
    when(executionService.executeSystemUserScoped(any(String.class), any(Callable.class)))
      .thenAnswer(inv -> ((Callable<?>) inv.getArgument(1)).call());
  }

  @Test
  void createsServicePointInCentralTenant() {
    service.createInterimServicePoint();

    verify(executionService).executeSystemUserScoped(eq("central"), any(Callable.class));
    var captor = ArgumentCaptor.forClass(ServicePoint.class);
    verify(servicePointClient).create(captor.capture());
    var sp = captor.getValue();
    assertEquals("32c6f0c7-26e4-4350-8c29-1e11c2e3efc4", sp.getId());
    assertEquals("interimsp", sp.getCode());
    assertEquals(3, sp.getHoldShelfExpiryPeriod().getDuration());
  }

  @Test
  void duplicateIsIgnored() {
    doThrow(error(HttpStatus.UNPROCESSABLE_ENTITY, "id value already exists in table service_point"))
      .when(servicePointClient).create(any());

    service.createInterimServicePoint();
  }

  @Test
  void otherErrorsArePropagated() {
    doThrow(error(HttpStatus.UNPROCESSABLE_ENTITY, "name is required")).when(servicePointClient).create(any());
    assertThrows(HttpClientErrorException.class, () -> service.createInterimServicePoint());

    doThrow(error(HttpStatus.INTERNAL_SERVER_ERROR, "already exists")).when(servicePointClient).create(any());
    assertThrows(Exception.class, () -> service.createInterimServicePoint());
  }

  private static HttpClientErrorException error(HttpStatus status, String body) {
    return HttpClientErrorException.create(status, status.name(), new HttpHeaders(),
      body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
  }
}
