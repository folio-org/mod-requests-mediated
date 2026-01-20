package org.folio.mr.domain.entity.projection;

public interface BatchRequestStats {

  Integer getTotal();

  Integer getPending();

  Integer getCompleted();

  Integer getInProgress();

  Integer getFailed();
}
