package com.grammarlab;

import com.grammarlab.dto.LRConflictDto;
import com.grammarlab.dto.LRItemSetDto;
import com.grammarlab.dto.LRParseResponse;
import com.grammarlab.dto.LRParseStep;
import com.grammarlab.dto.LRTableResponse;
import com.grammarlab.exception.GrammarParseException;
import com.grammarlab.parser.GrammarParser;
import com.grammarlab.service.FirstFollowService;
import com.grammarlab.service.GrammarService;
import com.grammarlab.service.LRService;
import com.grammarlab.validator.GrammarValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prompt 6 verification tests: LR(0) parsing table, canonical item sets, conflict detection,
 * and string simulation traces.
 */
class Prompt6VerificationTest {

    private static final String GRAMMAR_LR0_CLEAN = """
            S -> a A
            A -> a A | b
            """;

    private static final String GRAMMAR_LR0_SR = """
            S -> a A | a
            A -> b
            """;

    private static final String GRAMMAR_LR0_RR = """
            S -> A | B
            A -> a
            B -> a
            """;

    private static final String GRAMMAR_LR0_EPS = """
            S -> a A
            A -> ε
            """;

    private LRService lrService;

    @BeforeEach
    void setUp() {
        GrammarParser parser = new GrammarParser();
        GrammarValidator validator = new GrammarValidator();
        GrammarService grammarService = new GrammarService(parser, validator);
        FirstFollowService firstFollowService = new FirstFollowService(grammarService);
        lrService = new LRService(grammarService, firstFollowService);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TEST 1 – Deterministic LR(0) Grammar (S -> a A, A -> a A | b)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("P6-T1-COLLECTION: canonical item sets have initial state, closures, and GOTO edges")
    void test1_canonicalCollectionStructure() {
        LRTableResponse table = lrService.buildLR0Table(GRAMMAR_LR0_CLEAN, null);

        assertTrue(table.isValid());
        assertTrue(table.isDeterministic(), "S -> a A / A -> a A | b must be deterministic in LR(0)");
        assertTrue(table.getConflicts().isEmpty(), "No conflicts expected");

        List<LRItemSetDto> itemSets = table.getItemSets();
        assertFalse(itemSets.isEmpty());

        // State 0 must have kernel item S' -> • S and expanded S -> • a A
        LRItemSetDto i0 = itemSets.getFirst();
        assertEquals("I0", i0.getName());
        assertTrue(i0.getItems().stream().anyMatch(it -> it.getRule().equals("S' -> • S")));
        assertTrue(i0.getItems().stream().anyMatch(it -> it.getRule().equals("S -> • a A")));

        // State 0 transitions
        assertTrue(i0.getGotos().stream().anyMatch(g -> g.getSymbol().equals("S")));
        assertTrue(i0.getGotos().stream().anyMatch(g -> g.getSymbol().equals("a")));
    }

    @Test
    @DisplayName("P6-T1-TABLE: ACTION and GOTO table rows include shift, reduce, and acc")
    void test1_tableEntries() {
        LRTableResponse table = lrService.buildLR0Table(GRAMMAR_LR0_CLEAN, null);

        List<Map<String, Object>> rows = table.getTableRows();
        assertFalse(rows.isEmpty());

        // Verify accept on '$'
        boolean hasAcc = false;
        boolean hasShift = false;
        boolean hasReduce = false;

        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            Map<String, String> actions = (Map<String, String>) row.get("actions");
            if (actions != null) {
                if ("acc".equals(actions.get("$"))) hasAcc = true;
                for (String act : actions.values()) {
                    if (act.startsWith("s")) hasShift = true;
                    if (act.startsWith("r")) hasReduce = true;
                }
            }
        }

        assertTrue(hasAcc, "Table must have accept action 'acc'");
        assertTrue(hasShift, "Table must have shift actions");
        assertTrue(hasReduce, "Table must have reduce actions");
    }

    @Test
    @DisplayName("P6-T1-PARSE-ACCEPT: 'a b' accepted with complete trace shape")
    void test1_parseAccept_ab() {
        LRParseResponse res = lrService.parseLR0(GRAMMAR_LR0_CLEAN, null, "a b");

        assertTrue(res.isAccepted(), "'a b' must be accepted");
        assertFalse(res.getSteps().isEmpty(), "Trace must not be empty");

        for (LRParseStep step : res.getSteps()) {
            assertNotNull(step.getStateStack(), "stateStack must not be null at step " + step.getStep());
            assertNotNull(step.getSymbolStack(), "symbolStack must not be null at step " + step.getStep());
            assertNotNull(step.getInputBuffer(), "inputBuffer must not be null at step " + step.getStep());
            assertNotNull(step.getAction(), "action must not be null at step " + step.getStep());
        }

        assertEquals("Accept", res.getSteps().getLast().getAction());
    }

    @Test
    @DisplayName("P6-T1-PARSE-ACCEPT: 'a a b' accepted with multiple shifts and reduces")
    void test1_parseAccept_aab() {
        LRParseResponse res = lrService.parseLR0(GRAMMAR_LR0_CLEAN, null, "a a b");

        assertTrue(res.isAccepted(), "'a a b' must be accepted");
        assertEquals("Accept", res.getSteps().getLast().getAction());
    }

    @Test
    @DisplayName("P6-T1-PARSE-REJECT: 'b' rejected gracefully")
    void test1_parseReject_b() {
        LRParseResponse res = lrService.parseLR0(GRAMMAR_LR0_CLEAN, null, "b");

        assertFalse(res.isAccepted());
        assertNotNull(res.getMessage());
        assertTrue(res.getMessage().contains("Syntax Error"));
        assertFalse(res.getSteps().isEmpty(), "Steps recorded up to syntax error");
    }

    @Test
    @DisplayName("P6-T1-PARSE-REJECT: empty string rejected")
    void test1_parseReject_empty() {
        LRParseResponse res = lrService.parseLR0(GRAMMAR_LR0_CLEAN, null, "");

        assertFalse(res.isAccepted());
        assertTrue(res.getMessage().contains("Syntax Error"));
    }

    @Test
    @DisplayName("P6-T1-PARSE-REJECT: extra token 'a b b' rejected")
    void test1_parseReject_extraTokens() {
        LRParseResponse res = lrService.parseLR0(GRAMMAR_LR0_CLEAN, null, "a b b");

        assertFalse(res.isAccepted());
        assertTrue(res.getMessage().contains("Syntax Error"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TEST 2 – Shift/Reduce Conflict
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("P6-T2-SR-CONFLICT: detects Shift/Reduce conflict on lookahead 'b'")
    void test2_shiftReduceConflict() {
        LRTableResponse table = lrService.buildLR0Table(GRAMMAR_LR0_SR, null);

        assertFalse(table.isDeterministic());
        assertFalse(table.getConflicts().isEmpty());

        LRConflictDto conflict = table.getConflicts().stream()
                .filter(c -> "Shift / Reduce Conflict".equals(c.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(conflict, "Must find a Shift / Reduce conflict");
        assertEquals("b", conflict.getTerminal());
        assertTrue(conflict.getRules().stream().anyMatch(r -> r.startsWith("s")));
        assertTrue(conflict.getRules().stream().anyMatch(r -> r.startsWith("r")));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TEST 3 – Reduce/Reduce Conflict
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("P6-T3-RR-CONFLICT: detects Reduce/Reduce conflict between A -> a and B -> a")
    void test3_reduceReduceConflict() {
        LRTableResponse table = lrService.buildLR0Table(GRAMMAR_LR0_RR, null);

        assertFalse(table.isDeterministic());
        assertFalse(table.getConflicts().isEmpty());

        boolean hasRR = table.getConflicts().stream()
                .anyMatch(c -> "Reduce / Reduce Conflict".equals(c.getType()));

        assertTrue(hasRR, "Must detect Reduce / Reduce conflict");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TEST 4 – Epsilon, Unrecognized Tokens, and Robustness
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("P6-T4-EPSILON: LR(0) accepts grammar deriving epsilon")
    void test4_epsilonGrammar() {
        LRParseResponse res = lrService.parseLR0(GRAMMAR_LR0_EPS, null, "a");

        assertTrue(res.isAccepted(), "S -> a A / A -> ε must accept 'a'");
        assertEquals("Accept", res.getSteps().getLast().getAction());
    }

    @Test
    @DisplayName("P6-T4-UNKNOWN-TOKEN: returns failure without unhandled exception")
    void test4_unrecognizedToken() {
        LRParseResponse res = lrService.parseLR0(GRAMMAR_LR0_CLEAN, null, "a FOOBAR b");

        assertFalse(res.isAccepted());
        assertTrue(res.getMessage().contains("Unrecognized terminal"));
    }

    @Test
    @DisplayName("P6-T4-MALFORMED: malformed grammar throws GrammarParseException cleanly")
    void test4_malformedGrammarThrows() {
        assertThrows(GrammarParseException.class, () ->
                lrService.buildLR0Table("E -> $", null)
        );
    }
}
