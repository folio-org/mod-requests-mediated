package org.folio.mr.config;

import org.folio.mr.client.CheckInClient;
import org.folio.mr.client.CheckOutClient;
import org.folio.mr.client.CirculationClient;
import org.folio.mr.client.CirculationErrorForwardingClient;
import org.folio.mr.client.EcsExternalTlrClient;
import org.folio.mr.client.EcsTlrClient;
import org.folio.mr.client.HoldingClient;
import org.folio.mr.client.InstanceClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.client.LibraryClient;
import org.folio.mr.client.LoanClient;
import org.folio.mr.client.LoanPolicyClient;
import org.folio.mr.client.LoanTypeClient;
import org.folio.mr.client.LocationClient;
import org.folio.mr.client.LocationUnitClient;
import org.folio.mr.client.MaterialTypeClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.client.SearchClient;
import org.folio.mr.client.SearchInstancesClient;
import org.folio.mr.client.ServicePointClient;
import org.folio.mr.client.SettingsClient;
import org.folio.mr.client.TlrErrorForwardingClient;
import org.folio.mr.client.UserClient;
import org.folio.mr.client.UserGroupClient;
import org.folio.mr.client.UserTenantsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfiguration {

  @Bean
  public CheckInClient checkInClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CheckInClient.class);
  }

  @Bean
  public CheckOutClient checkOutClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CheckOutClient.class);
  }

  @Bean
  public CirculationClient circulationClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CirculationClient.class);
  }

  @Bean
  public CirculationErrorForwardingClient circulationErrorForwardingClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CirculationErrorForwardingClient.class);
  }

  @Bean
  public EcsExternalTlrClient ecsExternalTlrClient(HttpServiceProxyFactory factory) {
    return factory.createClient(EcsExternalTlrClient.class);
  }

  @Bean
  public EcsTlrClient ecsTlrClient(HttpServiceProxyFactory factory) {
    return factory.createClient(EcsTlrClient.class);
  }

  @Bean
  public HoldingClient holdingClient(HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingClient.class);
  }

  @Bean
  public InstanceClient instanceClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceClient.class);
  }

  @Bean
  public ItemClient itemClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ItemClient.class);
  }

  @Bean
  public LibraryClient libraryClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LibraryClient.class);
  }

  @Bean
  public LoanClient loanClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LoanClient.class);
  }

  @Bean
  public LoanPolicyClient loanPolicyClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LoanPolicyClient.class);
  }

  @Bean
  public LoanTypeClient loanTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LoanTypeClient.class);
  }

  @Bean
  public LocationClient locationClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LocationClient.class);
  }

  @Bean
  public LocationUnitClient locationUnitClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LocationUnitClient.class);
  }

  @Bean
  public MaterialTypeClient materialTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(MaterialTypeClient.class);
  }

  @Bean
  public RequestStorageClient requestStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(RequestStorageClient.class);
  }

  @Bean
  public SearchClient searchClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SearchClient.class);
  }

  @Bean
  public SearchInstancesClient searchInstancesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SearchInstancesClient.class);
  }

  @Bean
  public ServicePointClient servicePointClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ServicePointClient.class);
  }

  @Bean
  public SettingsClient settingsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SettingsClient.class);
  }

  @Bean
  public TlrErrorForwardingClient tlrErrorForwardingClient(HttpServiceProxyFactory factory) {
    return factory.createClient(TlrErrorForwardingClient.class);
  }

  @Bean
  public UserClient userClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserClient.class);
  }

  @Bean
  public UserGroupClient userGroupClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserGroupClient.class);
  }

  @Bean
  public UserTenantsClient userTenantsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserTenantsClient.class);
  }
}

