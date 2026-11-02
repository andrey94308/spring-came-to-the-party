package org.orgarchive.archive.document;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentOrganizationMigration implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public DocumentOrganizationMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // Existing lab volumes still have the tenant ID inside metadata.
        jdbc.update("UPDATE document SET organization_uuid = JSON_VALUE(metadata, '$.organizationUuid') "
                + "WHERE organization_uuid IS NULL OR organization_uuid = ''");
        Integer missing = jdbc.queryForObject("SELECT COUNT(*) FROM document "
                + "WHERE organization_uuid IS NULL OR organization_uuid = ''", Integer.class);
        if (missing != null && missing > 0) {
            throw new IllegalStateException("Documents without organization_uuid: " + missing);
        }
        jdbc.update("UPDATE document SET metadata = JSON_REMOVE(metadata, '$.organizationUuid') "
                + "WHERE JSON_VALUE(metadata, '$.organizationUuid') IS NOT NULL");
        String nullable = jdbc.queryForObject("SELECT IS_NULLABLE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'document' "
                + "AND COLUMN_NAME = 'organization_uuid'", String.class);
        if ("YES".equals(nullable)) {
            jdbc.execute("ALTER TABLE document MODIFY organization_uuid VARCHAR(36) NOT NULL");
        }
    }
}
