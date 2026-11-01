package org.mato.archive.document;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.mato.archive.user.UserRepository;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.mariadb.MariaDBFilterExpressionConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CVE-2026-22730 demonstration. The {@link MariaDBFilterExpressionConverter}
 * shipped with spring-ai-mariadb-store 1.0.3 wraps string filter values in
 * single quotes without escaping, so a value containing {@code '} breaks out of
 * the SQL string literal and injects arbitrary SQL into the WHERE clause.
 *
 * In a real Spring AI deployment this WHERE fragment would be appended to the
 * vector-store similarity-search SELECT. We do not need vectors for the CTF —
 * the SQL injection is identical without them.
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    /** Spring AI 1.0.3's converter takes the metadata column name. The same
     *  column appears in our `document` table. */
    private final MariaDBFilterExpressionConverter converter =
            new MariaDBFilterExpressionConverter("metadata");

    public DocumentController(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    public record SearchRequest(String query, String department) {}

    public record DocumentDto(Long id, String title, String content, String metadata) {}

    @PostMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public List<DocumentDto> search(@RequestBody SearchRequest req,
                                    @AuthenticationPrincipal UserDetails principal) {
        Filter.Expression expr = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("department"),
                new Filter.Value(req.department()));

        // Vulnerable: convertExpression() wraps the user-controlled value in
        // single quotes without escaping.
        String whereFragment = converter.convertExpression(expr);

        // Spring AI's converter emits a JSON_VALUE expression against a
        // `metadata` column, which is exactly how we store it here.
        // The employee's department comes from the server-side user record. A
        // plain request for another department therefore returns no rows.
        // Deliberately vulnerable: the unescaped filter can inject an OR that
        // escapes this AND condition (SQL's AND has higher precedence).
        String sql = "SELECT id, title, content, metadata FROM document WHERE "
                + "JSON_VALUE(metadata, '$.department') = ? AND " + whereFragment;

        return jdbc.query(sql, (rs, i) -> new DocumentDto(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("metadata")), allowedDepartment(principal));
    }

    /** A safe comparison for the live demo: both values are JDBC parameters. */
    @PostMapping("/search-safe")
    @PreAuthorize("hasRole('USER')")
    public List<DocumentDto> searchSafe(@RequestBody SearchRequest req,
                                        @AuthenticationPrincipal UserDetails principal) {
        String sql = "SELECT id, title, content, metadata FROM document WHERE "
                + "JSON_VALUE(metadata, '$.department') = ? AND "
                + "JSON_VALUE(metadata, '$.department') = ?";
        return jdbc.query(sql, (rs, i) -> new DocumentDto(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("metadata")), allowedDepartment(principal), req.department());
    }

    private String allowedDepartment(UserDetails principal) {
        return users.findById(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user is missing"))
                .getDepartment();
    }

    @PostMapping("/search-debug")
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> searchDebug(@RequestBody SearchRequest req) {
        Filter.Expression expr = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("department"),
                new Filter.Value(req.department()));
        return Map.of("whereFragment", converter.convertExpression(expr));
    }
}
