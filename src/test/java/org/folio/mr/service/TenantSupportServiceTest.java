package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.folio.mr.config.TenantConfig;
import org.folio.mr.service.impl.TenantSupportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class TenantSupportServiceTest {

  @Mock
  private ConsortiumService consortiumService;
  @Mock
  private TenantConfig tenantConfig;

  @InjectMocks
  private TenantSupportServiceImpl service;

  @Test
  void isCentralTenant_shouldReturnTrue_whenCentralTenantIdMatches() {
    var tenantId = "central";

    when(consortiumService.getCentralTenantId(tenantId)).thenReturn(Optional.of(tenantId));

    assertTrue(service.isCentralTenant(tenantId));
  }

  @Test
  void isCentralTenant_shouldReturnFalse_whenCentralTenantIdDoesNotMatch() {
    var tenantId = "tenant1";

    when(consortiumService.getCentralTenantId(tenantId)).thenReturn(Optional.of("central"));

    assertFalse(service.isCentralTenant(tenantId));
  }

  @Test
  void isCentralTenant_shouldReturnFalse_whenNoUserTenants() {
    var tenantId = "tenant1";

    when(consortiumService.getCentralTenantId(tenantId)).thenReturn(Optional.empty());

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
