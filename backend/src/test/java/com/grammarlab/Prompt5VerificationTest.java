package com.grammarlab;

import com.grammarlab.dto.LL1Conflict;
import com.grammarlab.dto.LL1ParseResponse;
import com.grammarlab.dto.LL1ParseStep;
import com.grammarlab.dto.LL1TableResponse;
import com.grammarlab.dto.LL1TableRowDto;
import com.grammarlab.dto.FirstFollowResponse;
import com.grammarlab.parser.GrammarParser;
import com.grammarlab.service.FirstFollowService;
import com.grammarlab.service.GrammarService;
import com.grammarlab.service.LL1Service;
import com.grammarlab.validator.GrammarValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prompt 5 verification tests.
 *
 * Test 1 grammar:  S -> a A   /   A -> b | ε
 * Test 2 grammar:  S -> a A | a B   /   A -> b   /   B -> c
 *
 * Also covers: epsilon handling, empty-cell parse errors, trace shape,
 * and frontend-to-backend wiring (smoke-tested via the controller tests).
 */
class Prompt5VerificationTest {

    // ── Grammar strings ────────────────────────────────────────────────────
    private static final String GRAMMAR_1 =
            "S -> a A\nA -> b | ε";

    private static final String GRAMMAR_2 =
            "S -> a A | a B\nA -> b\nB -> c";

    // ── Service under test ────────────────────────────────────────────────
    private FirstFollowService ffService;
    private LL1Service ll1Service;

    @BeforeEach
    void setUp() {
        GrammarParser parser   = new GrammarParser();
        GrammarValidator val   = new GrammarValidator();
        GrammarService gs      = new GrammarService(parser, val);
        ffService              = new FirstFollowService(gs);
        ll1Service             = new LL1Service(gs, ffService);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TEST 1 – S -> a A  /  A -> b | ε
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("T1-FF: FIRST(S)={a}, FOLLOW(S)={$}, FIRST(A)={b,ε}, FOLLOW(A)={$}")
    void test1_firstFollowSets() {
        FirstFollowResponse res = ffService.compute(GRAMMAR_1, null);

        assertTrue(res.isValid());

        // FIRST(S) = {a} — no epsilon, no b leaking through
        List<String> firstS = res.getFirstSets().get("S");
        assertEquals(List.of("a"), firstS,
                "FIRST(S) must be exactly {a}");

        // FIRST(A) = {b, ε}
        List<String> firstA = res.getFirstSets().get("A");
        assertTrue(firstA.contains("b"),  "FIRST(A) must contain b");
        assertTrue(firstA.contains("ε"),  "FIRST(A) must contain ε");
        assertEquals(2, firstA.size(),    "FIRST(A) must be exactly {b, ε}");

        // FOLLOW(S) = {$}
        List<String> followS = res.getFollowSets().get("S");
        assertEquals(List.of("$"), followS,
                "FOLLOW(S) must be exactly {$}");

        // FOLLOW(A) = {$}  (A is the last symbol in S -> a A, so FOLLOW(S) propagates)
        List<String> followA = res.getFollowSets().get("A");
        assertEquals(List.of("$"), followA,
                "FOLLOW(A) must be exactly {$}");

        // Nullability: only A is nullable
        assertFalse(res.getNullableNonTerminals().contains("S"), "S must NOT be nullable");
        assertTrue(res.getNullableNonTerminals().contains("A"),  "A must be nullable");
    }

    @Test
    @DisplayName("T1-LL1: grammar is LL(1), no conflicts")
    void test1_isLL1NoConflicts() {
        LL1TableResponse table = ll1Service.buildTable(GRAMMAR_1, null);

        assertTrue(table.isLL1(),               "S -> a A  /  A -> b | ε  must be LL(1)");
        assertTrue(table.getConflicts().isEmpty(), "No conflicts expected");
    }

    @Test
    @DisplayName("T1-TABLE: M[S,a]=S->aA, M[A,b]=A->b, M[A,$]=A->ε, all other cells empty")
    void test1_tableEntries() {
        LL1TableResponse table = ll1Service.buildTable(GRAMMAR_1, null);

        LL1TableRowDto rowS = rowFor(table, "S");
        LL1TableRowDto rowA = rowFor(table, "A");

        // M[S, a] = S -> a A
        assertEquals("S -> a A", rowS.getCells().get("a"),
                "M[S,a] must be S -> a A");

        // M[S, b] must be empty (b is not in FIRST(S))
        assertTrue(isEmptyCell(rowS.getCells().get("b")),
                "M[S,b] must be empty");

        // M[S, $] must be empty (S is not nullable)
        assertTrue(isEmptyCell(rowS.getCells().get("$")),
                "M[S,$] must be empty");

        // M[A, b] = A -> b
        assertEquals("A -> b", rowA.getCells().get("b"),
                "M[A,b] must be A -> b");

        // M[A, $] = A -> ε  ($ is in FOLLOW(A))
        assertEquals("A -> ε", rowA.getCells().get("$"),
                "M[A,$] must be A -> ε");

        // M[A, a] must be empty (a ∉ FIRST(A) and a ∉ FOLLOW(A))
        assertTrue(isEmptyCell(rowA.getCells().get("a")),
                "M[A,a] must be empty");
    }

    @Test
    @DisplayName("T1-PARSE-ACCEPT: 'a b' is accepted with correct trace shape")
    void test1_parseAccept_ab() {
        LL1ParseResponse res = ll1Service.simulateParse(GRAMMAR_1, null, "a b");

        assertTrue(res.isAccepted(), "'a b' must be accepted");
        assertFalse(res.getSteps().isEmpty(), "Trace must not be empty");

        // Every step must have stack, inputBuffer and action populated
        for (LL1ParseStep step : res.getSteps()) {
            assertNotNull(step.getStack(),       "stack must not be null at step " + step.getStep());
            assertNotNull(step.getInputBuffer(), "inputBuffer must not be null at step " + step.getStep());
            assertNotNull(step.getAction(),      "action must not be null at step " + step.getStep());
        }

        // Last step is Accept
        LL1ParseStep last = res.getSteps().getLast();
        assertEquals("Accept", last.getAction(), "Last action must be Accept");
    }

    @Test
    @DisplayName("T1-PARSE-ACCEPT: 'a' alone is accepted (A derives ε)")
    void test1_parseAccept_aOnly() {
        LL1ParseResponse res = ll1Service.simulateParse(GRAMMAR_1, null, "a");

        assertTrue(res.isAccepted(),
                "'a' must be accepted because A -> ε allows S -> a A -> a");
        assertEquals("Accept", res.getSteps().getLast().getAction());
    }

    @Test
    @DisplayName("T1-PARSE-REJECT: 'b' alone is rejected (no M[S,b])")
    void test1_parseReject_bOnly() {
        LL1ParseResponse res = ll1Service.simulateParse(GRAMMAR_1, null, "b");

        assertFalse(res.isAccepted(), "'b' must be rejected");
        assertNotNull(res.getMessage());
        assertTrue(res.getMessage().contains("Syntax Error"),
                "Rejection message must contain 'Syntax Error'");
        assertFalse(res.getSteps().isEmpty(), "Steps must be recorded even on rejection");
    }

    @Test
    @DisplayName("T1-PARSE-REJECT: 'a b b' is rejected (extra b after A is consumed)")
    void test1_parseReject_abExtraB() {
        LL1ParseResponse res = ll1Service.simulateParse(GRAMMAR_1, null, "a b b");

        assertFalse(res.isAccepted(), "'a b b' must be rejected");
        assertTrue(res.getMessage().contains("Syntax Error"),
                "Rejection message must contain 'Syntax Error'");
    }

    @Test
    @DisplayName("T1-PARSE-REJECT: empty string '' is rejected (S requires at least 'a')")
    void test1_parseReject_empty() {
        LL1ParseResponse res = ll1Service.simulateParse(GRAMMAR_1, null, "");

        assertFalse(res.isAccepted(), "Empty string must be rejected for this grammar");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TEST 2 – S -> a A | a B  /  A -> b  /  B -> c
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("T2-LL1: grammar is NOT LL(1) — First/First conflict in M[S,a]")
    void test2_notLL1_firstFirstConflict() {
        LL1TableResponse table = ll1Service.buildTable(GRAMMAR_2, null);

        assertFalse(table.isLL1(),
                "S -> a A | a B has a First/First conflict and must NOT be LL(1)");
        assertFalse(table.getConflicts().isEmpty(),
                "At least one conflict must be reported");
    }

    @Test
    @DisplayName("T2-CONFLICT-DETAIL: conflict is First-First in M[S,a] with both S->aA and S->aB")
    void test2_conflictDetails() {
        LL1TableResponse table = ll1Service.buildTable(GRAMMAR_2, null);

        LL1Conflict conflict = table.getConflicts().stream()
                .filter(c -> c.getNonTerminal().equals("S") && c.getTerminal().equals("a"))
                .findFirst()
                .orElse(null);

        assertNotNull(conflict, "Must find a conflict on M[S, a]");
        assertEquals("First-First Conflict", conflict.getType(),
                "Conflict type must be First-First Conflict");
        assertTrue(conflict.getRules().contains("S -> a A"),
                "Conflicting rules must include S -> a A");
        assertTrue(conflict.getRules().contains("S -> a B"),
                "Conflicting rules must include S -> a B");
    }

    @Test
    @DisplayName("T2-FF: FIRST(S)={a}, FIRST(A)={b}, FIRST(B)={c} — no epsilon anywhere")
    void test2_firstSets() {
        FirstFollowResponse res = ffService.compute(GRAMMAR_2, null);

        assertEquals(List.of("a"), res.getFirstSets().get("S"), "FIRST(S) = {a}");
        assertEquals(List.of("b"), res.getFirstSets().get("A"), "FIRST(A) = {b}");
        assertEquals(List.of("c"), res.getFirstSets().get("B"), "FIRST(B) = {c}");
        assertTrue(res.getNullableNonTerminals().isEmpty(), "No nullable non-terminals");
    }

    @Test
    @DisplayName("T2-TABLE-CONFLICT-CELL: M[S,a] cell contains 'Conflict' marker")
    void test2_tableCellMarkedConflict() {
        LL1TableResponse table = ll1Service.buildTable(GRAMMAR_2, null);

        LL1TableRowDto rowS = rowFor(table, "S");
        String cellSa = rowS.getCells().get("a");

        assertNotNull(cellSa, "M[S,a] must not be null");
        assertTrue(cellSa.contains("Conflict"),
                "M[S,a] must contain 'Conflict' marker; actual: " + cellSa);
        assertTrue(cellSa.contains("S -> a A"),
                "M[S,a] must list S -> a A; actual: " + cellSa);
        assertTrue(cellSa.contains("S -> a B"),
                "M[S,a] must list S -> a B; actual: " + cellSa);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CROSS-CUTTING: epsilon, empty cells, trace fields
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("EMPTY-CELL: empty table cell causes Syntax Error, not an exception")
    void test_emptyTableCellCausesSyntaxError() {
        // M[S, b] is empty for GRAMMAR_1; parsing 'b' must give a graceful error
        LL1ParseResponse res = ll1Service.simulateParse(GRAMMAR_1, null, "b");

        assertFalse(res.isAccepted());
        assertNotNull(res.getMessage());
        assertDoesNotThrow(() -> ll1Service.simulateParse(GRAMMAR_1, null, "b"),
                "Empty table cell must not throw; it must return a failure response");
    }

    @Test
    @DisplayName("TRACE-FIELDS: every step in a rejected trace has stack, inputBuffer, and action")
    void test_traceStepsHaveRequiredFields() {
        LL1ParseResponse res = ll1Service.simulateParse(GRAMMAR_1, null, "a b b");

        assertFalse(res.getSteps().isEmpty());
        for (LL1ParseStep step : res.getSteps()) {
            assertNotNull(step.getStack(),       "stack at step " + step.getStep());
            assertNotNull(step.getInputBuffer(), "inputBuffer at step " + step.getStep());
            assertNotNull(step.getAction(),      "action at step " + step.getStep());
        }
    }

    @Test
    @DisplayName("INVALID-GRAMMAR: grammar with parse error returns a clean exception, not 500")
    void test_invalidGrammarThrowsCleanly() {
        // Missing arrow — should throw GrammarParseException, not NPE/500
        assertThrows(
                com.grammarlab.exception.GrammarParseException.class,
                () -> ll1Service.buildTable("E E + T", null),
                "Malformed grammar must throw GrammarParseException"
        );
    }

    @Test
    @DisplayName("UNRECOGNIZED-TOKEN: unrecognized terminal in input returns failure, not crash")
    void test_unrecognizedTokenGracefulFailure() {
        LL1ParseResponse res = ll1Service.simulateParse(GRAMMAR_1, null, "a UNKNOWN b");

        assertFalse(res.isAccepted());
        assertNotNull(res.getMessage());
        assertTrue(res.getMessage().contains("unrecognized") || res.getMessage().contains("Syntax Error"),
                "Message must explain the unrecognized token; actual: " + res.getMessage());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private LL1TableRowDto rowFor(LL1TableResponse table, String nonTerminal) {
        return table.getRows().stream()
                .filter(r -> r.getNonTerminal().equals(nonTerminal))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No row for non-terminal: " + nonTerminal));
    }

    /** Empty cell is stored as "" or null in the formatted cells map. */
    private boolean isEmptyCell(String value) {
        return value == null || value.isEmpty();
    }
}
