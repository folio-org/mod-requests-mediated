package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.mr.client.UserTenantsClient;
import org.folio.mr.config.TenantConfig;
import org.folio.mr.domain.dto.GetUserTenantsResponse;
import org.folio.mr.domain.dto.UserTenant;
import org.folio.mr.service.impl.TenantSupportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class TenantSupportServiceTest {

  @Mock
  private UserTenantsClient userTenantsClient;
  @Mock
  private TenantConfig tenantConfig;

  @InjectMocks
  private TenantSupportServiceImpl service;

  @Test
  void isCentralTenant_shouldReturnTrue_whenCentralTenantIdMatches() {
    var tenantId = "central";
    var userTenant = new UserTenant();
    userTenant.setCentralTenantId(tenantId);
    var response = new GetUserTenantsResponse();
    response.setUserTenants(List.of(userTenant));

    when(userTenantsClient.getUserTenants(1)).thenReturn(response);

    assertTrue(service.isCentralTenant(tenantId));
  }

  @Test
  void isCentralTenant_shouldReturnFalse_whenCentralTenantIdDoesNotMatch() {
    var tenantId = "tenant1";
    var userTenant = new UserTenant();
    userTenant.setCentralTenantId("central");
    var response = new GetUserTenantsResponse();
    response.setUserTenants(List.of(userTenant));

    when(userTenantsClient.getUserTenants(1)).thenReturn(response);

    assertFalse(service.isCentralTenant(tenantId));
  }

  @Test
  void isCentralTenant_shouldReturnFalse_whenNoUserTenants() {
    var tenantId = "tenant1";
    var response = new GetUserTenantsResponse();
    response.setUserTenants(List.of());

    when(userTenantsClient.getUserTenants(1)).thenReturn(response);

    assertFalse(service.isCentralTenant(tenantId));
  }

  @Test
  void isCentralTenant_shouldReturnFalse_whenResponseIsNull() {
    var tenantId = "tenant1";
    when(userTenantsClient.getUserTenants(1)).thenReturn(null);

    assertFalse(service.isCentralTenant(tenantId));
  }

  @Test
  void isSecureTenant_shouldReturnTrue_whenTenantIdMatchesSecureTenantId() {
    var tenantId = "secure";
    when(tenantConfig.getSecureTenantId()).thenReturn(tenantId);

    assertTrue(service.isSecureTenant(tenantId));
  }

  @Test
  void isSecureTenant_shouldReturnFalse_whenTenantIdDoesNotMatchSecureTenantId() {
    when(tenantConfig.getSecureTenantId()).thenReturn("secure");

    assertFalse(service.isSecureTenant("other"));
  }

  @Test
  void isSecureTenant_shouldReturnFalse_whenSecureTenantIdIsBlank() {
    when(tenantConfig.getSecureTenantId()).thenReturn("  ");

    assertFalse(service.isSecureTenant("any"));
  }
}
