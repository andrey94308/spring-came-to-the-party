package org.orgarchive.archive.document;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.mariadb.MariaDBFilterExpressionConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DocumentRepository {

    private static final String SELECT = "SELECT id, title, content, organization_uuid, metadata FROM document WHERE ";
    private static final String ORGANIZATION = "organization_uuid = ?";
    private static final RowMapper<DocumentDto> DOCUMENT_ROW = (rs, i) -> new DocumentDto(
            rs.getLong("id"), rs.getString("title"), rs.getString("content"),
            rs.getString("organization_uuid"), rs.getString("metadata"));

    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;
    private final MariaDBFilterExpressionConverter converter =
            new MariaDBFilterExpressionConverter("metadata");

    public DocumentRepository(EntityManager entityManager, JdbcTemplate jdbc) {
        this.entityManager = entityManager;
        this.jdbc = jdbc;
    }

    public long count() {
        return entityManager.createQuery("SELECT COUNT(d) FROM Document d", Long.class)
                .getSingleResult();
    }

    @Transactional
    public Document save(Document document) {
        if (document.getId() == null) {
            entityManager.persist(document);
            return document;
        }
        return entityManager.merge(document);
    }

    public List<DocumentDto> search(String allowedOrganizationUuid, String category, Integer minRevision) {
        // CVE-2026-22730: the converter does not escape quotes in a metadata filter.
        // An injected OR can bypass the bound organization condition due to SQL precedence.
        StringBuilder sql = new StringBuilder(SELECT).append(ORGANIZATION);
        if (category != null || minRevision != null) {
            sql.append(" AND ").append(filterSql(category, minRevision));
        }
        return jdbc.query(sql.toString(), DOCUMENT_ROW, allowedOrganizationUuid);
    }

    public List<DocumentDto> searchSafe(String allowedOrganizationUuid, String category, Integer minRevision) {
        StringBuilder sql = new StringBuilder(SELECT).append(ORGANIZATION);
        List<Object> params = new ArrayList<>();
        params.add(allowedOrganizationUuid);
        if (category != null) {
            sql.append(" AND JSON_VALUE(metadata, '$.category') = ?");
            params.add(category);
        }
        if (minRevision != null) {
            sql.append(" AND CAST(JSON_VALUE(metadata, '$.revision') AS SIGNED) >= ?");
            params.add(minRevision);
        }
        return jdbc.query(sql.toString(), DOCUMENT_ROW, params.toArray());
    }

    public String filterSql(String category, Integer minRevision) {
        Filter.Expression expression = null;
        if (category != null) {
            expression = appendFilter(expression, new Filter.Expression(
                    Filter.ExpressionType.EQ, new Filter.Key("category"), new Filter.Value(category)));
        }
        if (minRevision != null) {
            expression = appendFilter(expression, new Filter.Expression(
                    Filter.ExpressionType.GTE, new Filter.Key("revision"), new Filter.Value(minRevision)));
        }
        if (expression == null) {
            throw new IllegalArgumentException("At least one metadata filter is required");
        }
        return converter.convertExpression(expression);
    }

    private static Filter.Expression appendFilter(Filter.Expression current, Filter.Expression next) {
        return current == null ? next : new Filter.Expression(Filter.ExpressionType.AND, current, next);
    }
}
