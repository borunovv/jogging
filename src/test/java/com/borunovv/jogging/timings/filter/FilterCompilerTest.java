package com.borunovv.jogging.timings.filter;

import com.borunovv.core.testing.AbstractTest;
import org.junit.Test;

import javax.inject.Inject;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class FilterCompilerTest extends AbstractTest {

    @Test
    public void emptyFilter() {
        assertEquals("", filterCompiler.compileFilterToSQL(""));
    }

    @Test
    public void oneComparisonWithDate() {
        assertEquals("`date` > '2000-05-09'", filterCompiler.compileFilterToSQL("date gt '2000-05-09'"));
    }

    @Test
    public void oneComparisonWithInt() {
        assertEquals("distance <> 2000", filterCompiler.compileFilterToSQL("distance ne 2000"));
    }

    @Test
    public void allBooleanOperations() {
        String sql = filterCompiler.compileFilterToSQL("(date gt '2018-01-02') OR (distance le 1000) AND (distance gt 100)");
        System.out.println(sql);
        assertEquals("(`date` > '2018-01-02') OR (distance <= 1000) AND (distance > 100)", sql);
    }

    @Test
    public void allCompareOperations() {
        assertEquals("distance > 1 AND distance >= 2 AND distance < 3 AND distance <= 4 AND distance = 5 AND distance <> 6",
                filterCompiler.compileFilterToSQL(
                        "distance gt 1 AND distance ge 2 AND distance lt 3 AND distance le 4 AND distance eq 5 AND distance ne 6"));
    }

    @Test
    public void brackets() {
        assertEquals("(distance > 1) AND ((distance >= 2 AND distance < 3)) OR (((distance <= 4))) AND (distance = 5 OR distance <> 6)",
                filterCompiler.compileFilterToSQL(
                        "(distance gt 1) AND ((distance ge 2 AND distance lt 3)) OR (((distance le 4))) AND (distance eq 5 OR distance ne 6)"));
    }

    @Test
    public void failsOnBadDate() {
        expectError(
                "(date gt 'bad_date')",
                "Column 'date' has type 'date', and expected format is 'YYYY-MM-DD'. Your actual value: 'bad_date'");
    }

    @Test
    public void failsOnBadInt() {
        expectError(
                "(distance gt bad_int)",
                "Column 'distance' has type 'int'. Your actual value: bad_int");
    }

    @Test
    public void failsWithoutPairedBracket() {
        expectError(
                "(distance gt 1",
                "Filter syntax error. Unexpected end of string.");
    }

    @Test
    public void failsWithExtraBracket() {
        expectError(
                "(distance gt 1))",
                "Filter syntax error. There are redundant symbols at the end, expected end of string.");
    }

    @Test
    public void failsWithExtraSymbolsAtTheEnd() {
        expectError(
                "(distance gt 1) extra_symbols",
                "Filter syntax error. There are redundant symbols at the end, expected end of string.");
    }

    @Test
    public void failsWithBadComparison() {
        expectError(
                "(distance gt)",
                "Filter syntax error. Expected value to compare with.");

        expectError(
                "(distance )",
                "Filter syntax error. Undefined compare operation: ')'. Please use: [gt,lt,ge,le,eq,ne].");
    }

    private void expectError(String filter, String expectedErrorMsg) {
        try {
            filterCompiler.compileFilterToSQL(filter);
            fail("Expected error: [" + expectedErrorMsg + "]");
        } catch (RuntimeException e) {
            assertEquals(expectedErrorMsg, e.getMessage());
        }
    }

    @Inject
    private FilterCompiler filterCompiler;
}