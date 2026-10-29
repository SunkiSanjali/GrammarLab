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
 * Unit tests for LR(0) parser generation, conflict detection, and string simulation in LRService.
 */
class LRServiceTest {

    private LRService lrService;

    // ── Test Grammars ──────────────────────────────────────────────────────
    // G1: Deterministic LR(0) grammar without conflicts
    //   S -> a A
    //   A -> a A | b
    private static final String LR0_DETERMINISTIC_GRAMMAR = """
            S -> a A
            A -> a A | b
            """;

    // G2: Grammar with Shift/Reduce conflict in LR(0)
    //   S -> a A | a
    //   A -> b
    private static final String LR0_SR_CONFLICT_GRAMMAR = """
            S -> a A | a
            A -> b
            """;

    // G3: Grammar with Reduce/Reduce conflict in LR(0)
    //   S -> A | B
    //   A -> a
    //   B -> a
    private static final String LR0_RR_CONFLICT_GRAMMAR = """
            S -> A | B
            A -> a
            B -> a
            """;

    // G4: Grammar with epsilon in LR(0)
    //   S -> a A
    //   A -> ε
    private static final String LR0_EPSILON_GRAMMAR = """
            S -> a A
            A -> ε
            """;

    @BeforeEach
    void setUp() {
        GrammarParser parser = new GrammarParser();
        GrammarValidator validator = new GrammarValidator();
        GrammarService grammarService = new GrammarService(parser, validator);
        FirstFollowService firstFollowService = new FirstFollowService(grammarService);
        lrService = new LRService(grammarService, firstFollowService);
    }

    // ── 1. Canonical Item Sets & Closures ───────────────────────────────────

    @Test
    @DisplayName("LR(0) canonical collection correctly builds states, closure, and GOTO transitions")
    void testLR0CanonicalCollection() {
        LRTableResponse resp = lrService.buildLR0Table(LR0_DETERMINISTIC_GRAMMAR, null);

        assertNotNull(resp);
        assertTrue(resp.isValid());
        assertEquals("LR(0)", resp.getParserType());
        assertEquals("S", resp.getStartSymbol());
        assertEquals("S'", resp.getAugmentedStartSymbol());
        assertTrue(resp.isDeterministic(), "Grammar should be conflict-free in LR(0)");
        assertTrue(resp.getConflicts().isEmpty(), "No conflicts expected for deterministic grammar");

        List<LRItemSetDto> states = resp.getItemSets();
        assertFalse(states.isEmpty(), "States must not be empty");

        // State 0 (I0) must contain the initial augmented item S' -> • S and S -> • a A
        LRItemSetDto i0 = states.stream().filter(s -> s.getId() == 0).findFirst().orElseThrow();
        assertEquals("I0", i0.getName());
        boolean hasAugmentedStart = i0.getItems().stream().anyMatch(it -> it.getRule().contains("S' -> • S"));
        boolean hasInitialS = i0.getItems().stream().anyMatch(it -> it.getRule().contains("S -> • a A"));
        assertTrue(hasAugmentedStart, "I0 must contain S' -> • S");
        assertTrue(hasInitialS, "I0 must contain S -> • a A");

        // GOTO transitions from I0 must include transition on 'a' and 'S'
        boolean hasGotoS = i0.getGotos().stream().anyMatch(g -> g.getSymbol().equals("S"));
        boolean hasGotoA = i0.getGotos().stream().anyMatch(g -> g.getSymbol().equals("a"));
        assertTrue(hasGotoS, "I0 must transition on S");
        assertTrue(hasGotoA, "I0 must transition on a");
    }

    // ── 2. Table Construction & Headers ────────────────────────────────────

    @Test
    @DisplayName("LR(0) ACTION/GOTO parsing table headers and rows are properly formatted")
    void testLR0TableStructure() {
        LRTableResponse resp = lrService.buildLR0Table(LR0_DETERMINISTIC_GRAMMAR, null);

        // Action headers must include terminals and end marker '$'
        assertTrue(resp.getActionHeaders().contains("a"));
        assertTrue(resp.getActionHeaders().contains("b"));
        assertTrue(resp.getActionHeaders().contains("$"));

        // Goto headers must include original non-terminals S and A, but not S'
        assertTrue(resp.getGotoHeaders().contains("S"));
        assertTrue(resp.getGotoHeaders().contains("A"));
        assertFalse(resp.getGotoHeaders().contains("S'"));

        // Table rows
        List<Map<String, Object>> rows = resp.getTableRows();
        assertEquals(resp.getItemSets().size(), rows.size(), "Each state must have one table row");

        // At least one row must contain accept 'acc' on '$'
        boolean foundAcc = rows.stream().anyMatch(row -> {
            @SuppressWarnings("unchecked")
            Map<String, String> actions = (Map<String, String>) row.get("actions");
            return actions != null && "acc".equals(actions.get("$"));
        });
        assertTrue(foundAcc, "Table must have an 'acc' action on '$'");
    }

    // ── 3. Conflict Detection ──────────────────────────────────────────────

    @Test
    @DisplayName("LR(0) detects Shift/Reduce conflict when state has both shift and reduce")
    void testLR0ShiftReduceConflict() {
        LRTableResponse resp = lrService.buildLR0Table(LR0_SR_CONFLICT_GRAMMAR, null);

        assertFalse(resp.isDeterministic(), "Grammar with S -> a A | a must not be deterministic in LR(0)");
        assertFalse(resp.getConflicts().isEmpty(), "Conflicts must be detected");

        // Verify Shift / Reduce conflict is detected on terminal 'b'
        LRConflictDto srConflict = resp.getConflicts().stream()
                .filter(c -> "Shift / Reduce Conflict".equals(c.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(srConflict, "Must report a Shift / Reduce conflict");
        assertEquals("b", srConflict.getTerminal(), "Conflict should trigger on lookahead 'b'");
        assertTrue(srConflict.getRules().stream().anyMatch(r -> r.startsWith("s")), "Conflicting actions must include a shift");
        assertTrue(srConflict.getRules().stream().anyMatch(r -> r.startsWith("r")), "Conflicting actions must include a reduce");
    }

    @Test
    @DisplayName("LR(0) detects Reduce/Reduce conflict when state has two completed items")
    void testLR0ReduceReduceConflict() {
        LRTableResponse resp = lrService.buildLR0Table(LR0_RR_CONFLICT_GRAMMAR, null);

        assertFalse(resp.isDeterministic(), "Grammar with S -> A | B, A -> a, B -> a must have Reduce/Reduce conflict in LR(0)");
        assertFalse(resp.getConflicts().isEmpty(), "Conflicts must be detected");

        boolean foundRR = resp.getConflicts().stream()
                .anyMatch(c -> "Reduce / Reduce Conflict".equals(c.getType()));

        assertTrue(foundRR, "Must detect a Reduce / Reduce Conflict");
    }

    // ── 4. String Parsing Simulation ───────────────────────────────────────

    @Test
    @DisplayName("LR(0) simulation accepts valid input string 'a b' with complete trace")
    void testLR0ParseAccept_ab() {
        LRParseResponse parseRes = lrService.parseLR0(LR0_DETERMINISTIC_GRAMMAR, null, "a b");

        assertTrue(parseRes.isAccepted(), "'a b' must be accepted by deterministic LR(0) grammar");
        assertEquals("String accepted.", parseRes.getMessage());
        assertFalse(parseRes.getSteps().isEmpty(), "Trace must record steps");

        // First step starts at state 0 with '$' symbol stack
        LRParseStep first = parseRes.getSteps().getFirst();
        assertEquals(1, first.getStep());
        assertEquals("0", first.getStateStack());
        assertEquals("$", first.getSymbolStack());
        assertTrue(first.getAction().startsWith("Shift"));

        // Last step is Accept
        LRParseStep last = parseRes.getSteps().getLast();
        assertEquals("Accept", last.getAction());
    }

    @Test
    @DisplayName("LR(0) simulation accepts longer valid string 'a a b'")
    void testLR0ParseAccept_aab() {
        LRParseResponse parseRes = lrService.parseLR0(LR0_DETERMINISTIC_GRAMMAR, null, "a a b");

        assertTrue(parseRes.isAccepted(), "'a a b' must be accepted");
        LRParseStep last = parseRes.getSteps().getLast();
        assertEquals("Accept", last.getAction());
    }

    @Test
    @DisplayName("LR(0) simulation rejects syntax-violating input 'b' with graceful error and trace")
    void testLR0ParseReject_b() {
        LRParseResponse parseRes = lrService.parseLR0(LR0_DETERMINISTIC_GRAMMAR, null, "b");

        assertFalse(parseRes.isAccepted(), "'b' must be rejected");
        assertNotNull(parseRes.getMessage());
        assertTrue(parseRes.getMessage().contains("Syntax Error"));
        assertFalse(parseRes.getSteps().isEmpty(), "Trace must contain steps up to syntax error");
        assertTrue(parseRes.getSteps().getLast().getAction().contains("Error"));
    }

    @Test
    @DisplayName("LR(0) simulation rejects empty input string when grammar requires leading token")
    void testLR0ParseReject_empty() {
        LRParseResponse parseRes = lrService.parseLR0(LR0_DETERMINISTIC_GRAMMAR, null, "");

        assertFalse(parseRes.isAccepted(), "Empty input should be rejected when grammar requires tokens");
        assertTrue(parseRes.getMessage().contains("Syntax Error"));
    }

    @Test
    @DisplayName("LR(0) simulation rejects trailing tokens 'a b b'")
    void testLR0ParseReject_extraTokens() {
        LRParseResponse parseRes = lrService.parseLR0(LR0_DETERMINISTIC_GRAMMAR, null, "a b b");

        assertFalse(parseRes.isAccepted(), "'a b b' has extra tokens and must be rejected");
        assertTrue(parseRes.getMessage().contains("Syntax Error"));
    }

    @Test
    @DisplayName("LR(0) handles epsilon productions gracefully during parsing")
    void testLR0ParseEpsilonGrammar() {
        LRParseResponse parseRes = lrService.parseLR0(LR0_EPSILON_GRAMMAR, null, "a");

        assertTrue(parseRes.isAccepted(), "'a' must be accepted for S -> a A where A -> ε");
        assertEquals("Accept", parseRes.getSteps().getLast().getAction());
    }

    @Test
    @DisplayName("LR(0) rejects unrecognized terminals without crashing")
    void testLR0UnrecognizedTerminal() {
        LRParseResponse parseRes = lrService.parseLR0(LR0_DETERMINISTIC_GRAMMAR, null, "a INVALID b");

        assertFalse(parseRes.isAccepted());
        assertTrue(parseRes.getMessage().contains("Unrecognized terminal"));
    }

    @Test
    @DisplayName("LR(0) throws GrammarParseException on malformed grammar syntax")
    void testLR0MalformedGrammar() {
        assertThrows(GrammarParseException.class, () ->
                lrService.buildLR0Table("INVALID GRAMMAR NO ARROWS", null)
        );
    }
}
