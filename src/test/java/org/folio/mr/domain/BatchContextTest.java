package org.folio.mr.domain;

import static java.util.Collections.emptyMap;
import static org.folio.mr.domain.BatchContext.PARAM_BATCH_ID;
import static org.folio.mr.domain.BatchContext.PARAM_BATCH_PROCESS_FAILED_MESSAGE;
import static org.folio.mr.domain.BatchContext.PARAM_BATCH_SPLIT_ENTITY_IDS;
import static org.folio.mr.domain.BatchContext.PARAM_DEPLOYMENT_ENV_TYPE;
import static org.folio.mr.domain.BatchContext.PARAM_OKAPI_HEADERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.flow.api.StageContext;
import org.folio.mr.service.flow.EnvironmentType;
import org.junit.jupiter.api.Test;

class BatchContextTest {

  @Test
  void getBatchRequestId_positive() {
    var batchRequestId = UUID.randomUUID();
    var batchContext = getBatchContext(Map.of(PARAM_BATCH_ID, batchRequestId), emptyMap());

    var actual = batchContext.getBatchRequestId();

    assertEquals(batchRequestId, actual);
  }

  @Test
  void getDeploymentEnvType_positive() {
    var batchContext = getBatchContext(Map.of(PARAM_DEPLOYMENT_ENV_TYPE, EnvironmentType.ECS), emptyMap());

    var actual = batchContext.getDeploymentEnvType();

    assertEquals(EnvironmentType.ECS, actual);
  }

  @Test
  void withBatchSplitEntityIds_positive() {
    var batchContext = getBatchContext(emptyMap(), emptyMap());
    var batchSplitIds = List.of(UUID.randomUUID(), UUID.randomUUID());

    var actual = batchContext.withBatchSplitEntityIds(batchSplitIds);
    var savedBatchSplitIds = batchContext.<List<UUID>>get(PARAM_BATCH_SPLIT_ENTITY_IDS);

    assertEquals(batchSplitIds, savedBatchSplitIds);
    assertSame(batchContext, actual);
  }

  @Test
  void getBatchSplitEntityIds_positive() {
    var batchSplitIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    var batchContext = getBatchContext(emptyMap(), Map.of(PARAM_BATCH_SPLIT_ENTITY_IDS, batchSplitIds));

    var actual = batchContext.getBatchSplitEntityIds();

    assertEquals(batchSplitIds, actual);
  }

  @Test
  void setBatchRequestFailedMessage_positive() {
    var batchContext = getBatchContext(emptyMap(), emptyMap());
    var failureMessage = "Batch processing failed";

    batchContext.setBatchRequestFailedMessage(failureMessage);

    var savedFailureMessage = batchContext.<String>get(PARAM_BATCH_PROCESS_FAILED_MESSAGE);
    assertEquals(failureMessage, savedFailureMessage);
  }

  @Test
  void getBatchRequestFailedMessage_positive() {
    var batchContext = getBatchContext(emptyMap(), Map.of(PARAM_BATCH_PROCESS_FAILED_MESSAGE, "Error message"));

    var actual = batchContext.getBatchRequestFailedMessage();

    assertEquals("Error message", actual);
  }

  @Test
  void getBatchRequestFailedMessage_positive_nonString() {
    var batchContext = getBatchContext(emptyMap(), Map.of(PARAM_BATCH_PROCESS_FAILED_MESSAGE, 123));

    var actual = batchContext.getBatchRequestFailedMessage();

    assertEquals("123", actual);
  }

  @Test
  void getBatchRequestFailedMessage_positive_emptyWhenMissing() {
    var batchContext = getBatchContext(emptyMap(), emptyMap());

    var actual = batchContext.getBatchRequestFailedMessage();

    assertTrue(actual.isEmpty());
  }

  @Test
  void getOkapiHeaders_positive() {
    Map<String, Collection<String>> headers = Map.of("x-okapi-tenant", List.of("diku"));
    var batchContext = getBatchContext(Map.of(PARAM_OKAPI_HEADERS, headers), emptyMap());

    var actual = batchContext.getOkapiHeaders();

    assertEquals(headers, actual);
  }

  private static BatchContext getBatchContext(Map<String, Object> flowParams, Map<String, Object> contextMap) {
    var stageContext = StageContext.of("test", flowParams, contextMap);
    return new BatchContext(stageContext);
  }
}
