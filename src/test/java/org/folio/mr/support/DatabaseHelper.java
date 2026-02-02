package org.folio.mr.support;

import org.folio.spring.FolioModuleMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

public class DatabaseHelper {

  private final FolioModuleMetadata metadata;
  private final JdbcTemplate jdbcTemplate;

  public DatabaseHelper(FolioModuleMetadata metadata, JdbcTemplate jdbcTemplate) {
    this.metadata = metadata;
    this.jdbcTemplate = jdbcTemplate;
  }

  public String getDbPath(String tenantId, String basePath) {
    return metadata.getDBSchemaName(tenantId) + "." + basePath;
  }

  public int countRows(String tableName, String tenant) {
    return JdbcTestUtils.countRowsInTable(jdbcTemplate, getDbPath(tenant, tableName));
  }

  public int countRowsWhere(String tableName, String tenant, String whereClause) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, getDbPath(tenant, tableName), whereClause);
  }
}
