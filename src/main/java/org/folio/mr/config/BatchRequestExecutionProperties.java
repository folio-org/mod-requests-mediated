package org.folio.mr.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


@Data
@Validated
@Component
@ConfigurationProperties(prefix = "folio.batch-requests")
public class BatchRequestExecutionProperties {

  /**
   * Defines if batch request must print results after each execution.
   */
  private Boolean printResult = false;

  /**
   * Number of fork-join pool thread.
   */
  private int threadPoolSize = 4;

  /**
   * Defines maximum execution timeout, after which the execution is stopped
   */
  @NotNull
  private Duration executionTimeout = Duration.ofMinutes(30);

  /**
   * Provides a maximum amount of execution statuses to preserve in memory for the latest request executions
   */
  @Positive
  private int lastExecutionsStatusCacheSize = 25;

  /**
   * Provides a maximum amount of stale requests to be selected for recovery.
   */
  @Positive
  private int staleRequestsQueryLimit = 10;

  /**
   * Defines the threshold for selecting stale requests for recovery. Requests with the last
   * update time before the threshold will be considered stale and selected for recovery.
   *
   * <p>Default: 1 hour</p>
   */
  private Duration staleRequestThreshold = Duration.ofHours(1);
}
