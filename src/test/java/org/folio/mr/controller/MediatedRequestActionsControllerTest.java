package org.folio.mr.controller;

import static java.util.UUID.randomUUID;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequest;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestWorkflowLog;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.folio.mr.domain.MediatedRequestContext;
import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponse;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
import org.folio.mr.domain.dto.SendItemInTransitResponse;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserPersonal;
import org.folio.mr.domain.dto.UserPersonalAddressesInner;
import org.folio.mr.domain.entity.MediatedRequestWorkflowLog;
import org.folio.mr.service.MediatedRequestActionsService;
import org.folio.mr.service.impl.StaffSlipContextService;
import org.folio.mr.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class MediatedRequestActionsControllerTest {

  private static final String DATE_PATTERN = "dd-MMM-yyyy";
  private static final String DATE = "1-Jan-2024";

  @Mock
  private MediatedRequestActionsService mediatedRequestActionsService;

  @Mock
  private StaffSlipContextService staffSlipContextService;

  @Mock
  private UserServiceImpl userService;

  @InjectMocks
  private MediatedRequestActionsController requestsController;

  @Test
  @SneakyThrows
  void workflowLogGenerateActionDateWhenConfirmItemArrivalTest() {
    //given
    MediatedRequestWorkflowLog log = buildMediatedRequestWorkflowLog(DATE_PATTERN, DATE);
    MediatedRequest mediatedRequest = buildMediatedRequest(OPEN_IN_TRANSIT_FOR_APPROVAL);

    //mock
    when(mediatedRequestActionsService.saveMediatedRequestWorkflowLog(any()))
      .thenReturn(log);
    when(mediatedRequestActionsService.confirmItemArrival(any()))
      .thenReturn(mediatedRequest);

    //when
    String barcode = mediatedRequest.getItem().getBarcode();
    Date arrivalDate = Objects.requireNonNull(requestsController.confirmItemArrival(
      new ConfirmItemArrivalRequest(barcode)).getBody()).getArrivalDate();

    //then
    verify(mediatedRequestActionsService, times(1))
      .saveMediatedRequestWorkflowLog(any());
    assertThat(log.getActionDate(), is(arrivalDate));
  }

  @Test
  @SneakyThrows
  void workflowLogGenerateActionDateWhenSendItemInTransitTest() {
    //given
    MediatedRequestWorkflowLog log = buildMediatedRequestWorkflowLog(DATE_PATTERN, DATE);
    MediatedRequest mediatedRequest = buildMediatedRequest(OPEN_ITEM_ARRIVED);

    //mock
    when(mediatedRequestActionsService.saveMediatedRequestWorkflowLog(any()))
      .thenReturn(log);
    when(mediatedRequestActionsService.sendItemInTransit(any()))
      .thenReturn(new MediatedRequestContext(mediatedRequest));

    //when
    String barcode = mediatedRequest.getItem().getBarcode();
    Date inTransitDate = Objects.requireNonNull(requestsController.sendItemInTransit(
      new SendItemInTransitRequest(barcode)).getBody()).getInTransitDate();

    //then
    verify(mediatedRequestActionsService, times(1))
      .saveMediatedRequestWorkflowLog(any());
    assertThat(log.getActionDate(), is(inTransitDate));
  }

  @Test
  void confirmItemArrivalTest() {
    // given
    MediatedRequestWorkflowLog log = buildMediatedRequestWorkflowLog(DATE_PATTERN, DATE);
    MediatedRequest mediatedRequest = buildMediatedRequest(OPEN_ITEM_ARRIVED);
    String itemBarcode = mediatedRequest.getItem().getBarcode();
    when(mediatedRequestActionsService.confirmItemArrival(itemBarcode))
      .thenReturn(mediatedRequest);
    when(mediatedRequestActionsService.saveMediatedRequestWorkflowLog(any()))
      .thenReturn(log);

    // when
    var responseEntity = requestsController.confirmItemArrival(
      new ConfirmItemArrivalRequest(itemBarcode)
    );

    // then
    assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    ConfirmItemArrivalResponse response = responseEntity.getBody();
    assertThat(response, notNullValue());
    assertThat(response.getArrivalDate(), notNullValue());
    assertThat(response.getInstance().getId(), is(UUID.fromString("69640328-788e-43fc-9c3c-af39e243f3b7")));
    assertThat(response.getInstance().getTitle(), is("ABA Journal"));
    assertThat(response.getItem().getId(), is(UUID.fromString("9428231b-dd31-4f70-8406-fe22fbdeabc2")));
    assertThat(response.getItem().getBarcode(), is("A14837334314"));
    assertThat(response.getItem().getCallNumberComponents().getPrefix(), is("PFX"));
    assertThat(response.getItem().getCallNumberComponents().getCallNumber(), is("CN"));
    assertThat(response.getItem().getCallNumberComponents().getSuffix(), is("SFX"));
    assertThat(response.getMediatedRequest().getId(), is(UUID.fromString(mediatedRequest.getId())));
    assertThat(response.getMediatedRequest().getStatus(), is("Open - Item arrived"));
    assertThat(response.getRequester().getId(), is(UUID.fromString("9812e24b-0a66-457a-832c-c5e789797e35")));
    assertThat(response.getRequester().getBarcode(), is("111"));
    assertThat(response.getRequester().getFirstName(), is("Requester"));
    assertThat(response.getRequester().getMiddleName(), is("X"));
    assertThat(response.getRequester().getLastName(), is("Mediated"));
  }

  @Test
  void successfulMediatedRequestConfirmation() {
    doNothing().when(mediatedRequestActionsService).confirm(any(UUID.class));
    ResponseEntity<Void> response = requestsController.confirmMediatedRequest(randomUUID());
    assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
  }

  @Test
  void sendItemInTransitTest() {
    // given
    MediatedRequestWorkflowLog log = buildMediatedRequestWorkflowLog(DATE_PATTERN, DATE);
    MediatedRequest mediatedRequest = buildMediatedRequest(OPEN_ITEM_ARRIVED);
    String itemBarcode = mediatedRequest.getItem().getBarcode();
    MediatedRequestContext context = new MediatedRequestContext(mediatedRequest);
    context.setRequester(new User()
      .id(mediatedRequest.getRequesterId())
      .personal(new UserPersonal()
        .addAddressesItem(new UserPersonalAddressesInner()
          .addressLine1("test addressLine 1")
          .addressLine2("test addressLine 2")
          .city("city")
          .postalCode("test postal code")
          .region("test region")
          .countryId("US"))));
    when(mediatedRequestActionsService.sendItemInTransit(itemBarcode))
      .thenReturn(context);
    when(mediatedRequestActionsService.saveMediatedRequestWorkflowLog(any()))
      .thenReturn(log);

    // when
    var responseEntity = requestsController.sendItemInTransit(
      new SendItemInTransitRequest(itemBarcode)
    );

    // then
    assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    SendItemInTransitResponse response = responseEntity.getBody();
    assertThat(response, notNullValue());
    assertThat(response.getInTransitDate(), notNullValue());
    assertThat(response.getInstance().getId(), is(UUID.fromString("69640328-788e-43fc-9c3c-af39e243f3b7")));
    assertThat(response.getInstance().getTitle(), is("ABA Journal"));
    assertThat(response.getItem().getId(), is(UUID.fromString("9428231b-dd31-4f70-8406-fe22fbdeabc2")));
    assertThat(response.getItem().getBarcode(), is("A14837334314"));
    assertThat(response.getItem().getCallNumberComponents().getPrefix(), is("PFX"));
    assertThat(response.getItem().getCallNumberComponents().getCallNumber(), is("CN"));
    assertThat(response.getItem().getCallNumberComponents().getSuffix(), is("SFX"));
    assertThat(response.getMediatedRequest().getId(), is(UUID.fromString(mediatedRequest.getId())));
    assertThat(response.getMediatedRequest().getStatus(), is("Open - Item arrived"));
    assertThat(response.getRequester().getId(), is(UUID.fromString("9812e24b-0a66-457a-832c-c5e789797e35")));
    assertThat(response.getRequester().getBarcode(), is("111"));
    assertThat(response.getRequester().getFirstName(), is("Requester"));
    assertThat(response.getRequester().getMiddleName(), is("X"));
    assertThat(response.getRequester().getLastName(), is("Mediated"));
    assertThat(response.getRequester().getAddressLine1(), is("test addressLine 1"));
    assertThat(response.getRequester().getAddressLine2(), is("test addressLine 2"));
    assertThat(response.getRequester().getCity(), is("city"));
    assertThat(response.getRequester().getPostalCode(), is("test postal code"));
    assertThat(response.getRequester().getRegion(), is("test region"));
    assertThat(response.getRequester().getCountryId(), is("US"));
  }

  @Test
  void successfulMediatedRequestDecline() {
    doNothing().when(mediatedRequestActionsService).decline(any(UUID.class));
    ResponseEntity<Void> response = requestsController.declineMediatedRequest(randomUUID());
    assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
  }
}
