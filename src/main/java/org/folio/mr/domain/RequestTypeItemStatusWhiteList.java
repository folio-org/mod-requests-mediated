package org.folio.mr.domain;

import java.util.EnumSet;

import org.folio.mr.domain.dto.ItemStatus;

public class RequestTypeItemStatusWhiteList {
  private final static EnumSet<ItemStatus.NameEnum> STATUSES_ALLOWED_FOR_PAGE = EnumSet.of(
    ItemStatus.NameEnum.AVAILABLE,
    ItemStatus.NameEnum.RESTRICTED);

  private final static EnumSet<ItemStatus.NameEnum> STATUSES_ALLOWED_FOR_HOLD = EnumSet.of(
    ItemStatus.NameEnum.CHECKED_OUT,
    ItemStatus.NameEnum.AWAITING_PICKUP,
    ItemStatus.NameEnum.AWAITING_DELIVERY,
    ItemStatus.NameEnum.IN_TRANSIT,
    ItemStatus.NameEnum.MISSING,
    ItemStatus.NameEnum.PAGED,
    ItemStatus.NameEnum.ON_ORDER,
    ItemStatus.NameEnum.IN_PROCESS,
    ItemStatus.NameEnum.RESTRICTED);

  private final static EnumSet<ItemStatus.NameEnum> STATUSES_ALLOWED_FOR_RECALL = EnumSet.of(
    ItemStatus.NameEnum.CHECKED_OUT,
    ItemStatus.NameEnum.AWAITING_PICKUP,
    ItemStatus.NameEnum.AWAITING_DELIVERY,
    ItemStatus.NameEnum.IN_TRANSIT,
    ItemStatus.NameEnum.PAGED,
    ItemStatus.NameEnum.ON_ORDER,
    ItemStatus.NameEnum.IN_PROCESS,
    ItemStatus.NameEnum.RESTRICTED);

  public static boolean isItemStatusAllowedForRequestType(ItemStatus itemStatus,
    RequestType requestType) {

    return (switch (requestType) {
      case PAGE -> STATUSES_ALLOWED_FOR_PAGE;
      case HOLD -> STATUSES_ALLOWED_FOR_HOLD;
      case RECALL -> STATUSES_ALLOWED_FOR_RECALL;
    }).contains(itemStatus.getName());
  }
}
