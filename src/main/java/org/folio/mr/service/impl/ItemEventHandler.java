package org.folio.mr.service.impl;

import static org.folio.mr.support.kafka.EventType.UPDATE;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.KafkaEventHandler;
import org.folio.mr.support.kafka.KafkaEvent;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Service
@Log4j2
public class ItemEventHandler implements KafkaEventHandler<Item> {

  private final MediatedRequestsRepository mediatedRequestsRepository;

  @Override
  public void handle(KafkaEvent<Item> event) {
    log.info("handle:: processing item event: {}", event::getId);
    if (event.getGenericType() == UPDATE) {
      handleUpdateEvent(event);
    } else {
      log.info("handle:: ignoring event {} of unsupported type: {}", event::getId, event::getGenericType);
    }
    log.info("handle:: item event processed: {}", event::getId);
  }

  private void handleUpdateEvent(KafkaEvent<Item> event) {
    Item oldItem = event.getOldVersion();
    Item newItem = event.getNewVersion();

    if (oldItem == null || newItem == null) {
      log.warn("handleUpdateEvent:: event update message is missing either old or new item info. " +
        "Old item is null: {}. New item is null: {}.", oldItem == null, newItem == null);
      return;
    }

    if (StringUtils.isBlank(oldItem.getBarcode()) && !StringUtils.isBlank(newItem.getBarcode())) {
      log.info("handleUpdateEvent:: item without a barcode updated, new barcode: {}", newItem.getBarcode());
      handleAddedBarcodeEvent(event.getNewVersion());
    }

    log.info("handleUpdateEvent:: ignoring item update - barcode info hasn't changed. " +
      "Item ID: {}", newItem::getId);
  }

  private void handleAddedBarcodeEvent(Item item) {
    var mediatedRequests = mediatedRequestsRepository.findByItemId(UUID.fromString(item.getId()));
    if (mediatedRequests.isEmpty()) {
      log.info("handleAddedBarcodeEvent:: no mediated requests found for ID: {}", item.getId());
    }

    mediatedRequests.ifPresent(list -> {
      log.info("handleAddedBarcodeEvent:: {} mediated requests found, updating with barcode {}",
        list.size(), item.getBarcode());
      list.forEach(mr -> mr.setItemBarcode(item.getBarcode()));
      mediatedRequestsRepository.saveAll(list);
    });
  }

}
