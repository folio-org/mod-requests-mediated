package org.folio.mr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import org.folio.mr.client.UserTenantsClient;
import org.folio.mr.domain.dto.GetUserTenantsResponse;
import org.folio.mr.domain.dto.UserTenant;
import org.folio.mr.service.impl.ConsortiumServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsortiumServiceImplTest {

  @Mock
  private UserTenantsClient userTenantsClient;

  @InjectMocks
  private ConsortiumServiceImpl consortiumService;

  @Test
  void shouldReturnCentralTenantId() {
    // given
    var expectedCentralTenantId = "central-tenant";
    var userTenant = new UserTenant()
      .centralTenantId(expectedCentralTenantId)
      .tenantId("member-tenant");

    var userTenants = new GetUserTenantsResponse().userTenants(List.of(userTenant));

    when(userTenantsClient.getUserTenants(1)).thenReturn(userTenants);

    // when
    var result = consortiumService.getCentralTenantId();

    // then
    assertThat(result).isEqualTo(expectedCentralTenantId);
    verify(userTenantsClient).getUserTenants(1);
  }

  @Test
  void shouldThrowExceptionWhenNoCentralTenantFound() {
    // given
    var userTenants = new GetUserTenantsResponse().userTenants(List.of());
    when(userTenantsClient.getUserTenants(anyInt())).thenReturn(userTenants);

    // when & then
    assertThatThrownBy(() -> consortiumService.getCentralTenantId())
      .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void shouldReturnCurrentTenantId() {
    // given
    var expectedTenantId = "current-tenant";
    var userTenant = new UserTenant()
      .tenantId(expectedTenantId)
      .centralTenantId("central-tenant");

    var userTenants = new GetUserTenantsResponse().userTenants(List.of(userTenant));

    when(userTenantsClient.getUserTenants(1)).thenReturn(userTenants);

    // when
    var result = consortiumService.getCurrentTenantId();

    // then
    assertThat(result).isEqualTo(expectedTenantId);
    verify(userTenantsClient).getUserTenants(1);
  }

  @Test
  void shouldThrowExceptionWhenNoCurrentTenantFound() {
    // given
    var userTenants = new GetUserTenantsResponse().userTenants(List.of());
    when(userTenantsClient.getUserTenants(anyInt())).thenReturn(userTenants);

    // when & then
    assertThatThrownBy(() -> consortiumService.getCurrentTenantId())
      .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void shouldReturnCentralTenantIdForGivenTenant() {
    // given
    var tenantId = "member-tenant";
    var expectedCentralTenantId = "central-tenant";
    var userTenant = new UserTenant()
      .tenantId(tenantId)
      .centralTenantId(expectedCentralTenantId);

    var userTenants = new GetUserTenantsResponse().userTenants(List.of(userTenant));

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(userTenants);

    // when
    var result = consortiumService.getCentralTenantId(tenantId);

    // then
    assertThat(result).isPresent().contains(expectedCentralTenantId);
    verify(userTenantsClient).getUserTenants(tenantId);
  }

  @Test
  void shouldReturnEmptyWhenNoTenantsFoundForGivenTenant() {
    // given
    var tenantId = "non-existent-tenant";
    var userTenants = new GetUserTenantsResponse().userTenants(List.of());

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(userTenants);

    // when
    var result = consortiumService.getCentralTenantId(tenantId);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenUserTenantsClientReturnsNull() {
    // given
    var tenantId = "any-tenant";
    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(null);

    // when
    var result = consortiumService.getCentralTenantId(tenantId);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenUserTenantsListIsNull() {
    // given
    var tenantId = "any-tenant";
    var userTenants = new GetUserTenantsResponse().userTenants(null);
    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(userTenants);

    // when & then
    assertThatThrownBy(() -> consortiumService.getCentralTenantId(tenantId))
      .isInstanceOf(NullPointerException.class);
  }
}
