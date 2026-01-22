package org.folio.mr.domain.entity.projection;

import lombok.Data;

@Data
public class BatchRequestStatsImpl implements BatchRequestStats {

  private Integer total;

  private Integer pending;

  private Integer inProgress;

  private Integer completed;

  private Integer failed;
}
