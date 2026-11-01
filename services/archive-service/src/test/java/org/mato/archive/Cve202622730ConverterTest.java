package org.mato.archive;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.mariadb.MariaDBFilterExpressionConverter;

/**
 * Pins the vulnerable behaviour of CVE-2026-22730 in the
 * {@code spring-ai-mariadb-store} dependency we ship. If the converter ever
 * escapes single quotes, this test will fail and we know our SQL-injection
 * surface is gone.
 */
class Cve202622730ConverterTest {

    private final MariaDBFilterExpressionConverter converter =
            new MariaDBFilterExpressionConverter("metadata");

    @Test
    void converterEmitsRawSingleQuoteFromUserInput() {
        var expr = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("department"),
                new Filter.Value("intel-analysis' OR '1'='1"));

        String out = converter.convertExpression(expr);

        // The raw user-supplied single quote escapes the surrounding string literal.
        assertThat(out).contains("intel-analysis' OR '1'='1");
        // A patched converter would double the embedded single quote.
        assertThat(out).doesNotContain("analyst'' OR ''1''=''1");
    }

    @Test
    void converterAcceptsLegitimateValueUnchanged() {
        var expr = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("department"),
                new Filter.Value("intel-analysis"));
        String out = converter.convertExpression(expr);
        assertThat(out).contains("'intel-analysis'");
    }
}
