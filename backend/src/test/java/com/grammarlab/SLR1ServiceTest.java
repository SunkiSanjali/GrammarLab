package com.grammarlab;

import com.grammarlab.dto.LRConflictDto;
import com.grammarlab.dto.LRParseResponse;
import com.grammarlab.dto.LRParseStep;
import com.grammarlab.dto.LRTableResponse;
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
 * Unit tests for SLR(1) table construction, FOLLOW-restricted reductions,
 * LR(0) conflict resolution, and string simulation.
 */
class SLR1ServiceTest {

    private LRService lrService;

    // Grammar with a Shift/Reduce conflict in LR(0) that SLR(1) resolves
    // In LR(0): state after 'a' has S -> a • A, S -> a •, A -> • b
    //   LR(0) places reduction for S -> a • on ALL terminals (including b), causing S/R conflict with shift on b.
    // In SLR(1): reduction is restricted to FOLLOW(S) = {$}, so shift is on b and reduction is on $, resolving the conflict!
    private static final String SLR1_RESOLVES_LR0_GRAMMAR = """
            S -> a A | a
            A -> b
            """;

    // Assignment grammar used for table structure and expression parsing
    private static final String ASSIGN_GRAMMAR = """
            S -> L = R | R
            L -> * R | id
            R -> L
            """;

    // Classic expression grammar
    private static final String EXPR_GRAMMAR = """
            E -> E + T | T
            T -> T * F | F
            F -> ( E ) | id
            """;

    @BeforeEach
    void setUp() {
        GrammarParser parser = new GrammarParser();
        GrammarValidator validator = new GrammarValidator();
        GrammarService grammarService = new GrammarService(parser, validator);
        FirstFollowService firstFollowService = new FirstFollowService(grammarService);
        lrService = new LRService(grammarService, firstFollowService);
    }

    @Test
    @DisplayName("SLR(1) resolves LR(0) Shift/Reduce conflict on 'S -> a A | a'")
    void testSLR1ResolvesLR0Conflict() {
        // First verify that LR(0) DOES have a conflict on this grammar
        LRTableResponse lr0Resp = lrService.buildLR0Table(SLR1_RESOLVES_LR0_GRAMMAR, null);
        assertFalse(lr0Resp.isDeterministic(), "LR(0) must have conflicts on this grammar");
        assertFalse(lr0Resp.getConflicts().isEmpty(), "LR(0) must detect Shift/Reduce conflict");

        // Now verify that SLR(1) resolves the conflict and is completely deterministic
        LRTableResponse slr1Resp = lrService.buildSLR1Table(SLR1_RESOLVES_LR0_GRAMMAR, null);
        assertNotNull(slr1Resp);
        assertTrue(slr1Resp.isValid());
        assertEquals("SLR(1)", slr1Resp.getParserType());
        assertTrue(slr1Resp.isDeterministic(), "SLR(1) must resolve the conflict and be deterministic");
        assertTrue(slr1Resp.getConflicts().isEmpty(), "SLR(1) must have 0 conflicts");
    }

    @Test
    @DisplayName("SLR(1) correctly constructs table rows with FOLLOW-constrained reductions")
    void testSLR1TableStructure() {
        LRTableResponse slr1Resp = lrService.buildSLR1Table(ASSIGN_GRAMMAR, null);

        // Action headers must include terminals and '$'
        assertTrue(slr1Resp.getActionHeaders().contains("="));
        assertTrue(slr1Resp.getActionHeaders().contains("*"));
        assertTrue(slr1Resp.getActionHeaders().contains("id"));
        assertTrue(slr1Resp.getActionHeaders().contains("$"));

        // Goto headers must include original non-terminals S, L, R
        assertTrue(slr1Resp.getGotoHeaders().contains("S"));
        assertTrue(slr1Resp.getGotoHeaders().contains("L"));
        assertTrue(slr1Resp.getGotoHeaders().contains("R"));

        // Must have accept on '$'
        boolean hasAcc = slr1Resp.getTableRows().stream().anyMatch(row -> {
            @SuppressWarnings("unchecked")
            Map<String, String> actions = (Map<String, String>) row.get("actions");
            return actions != null && "acc".equals(actions.get("$"));
        });
        assertTrue(hasAcc, "SLR(1) table must contain 'acc' on '$'");
    }

    @Test
    @DisplayName("SLR(1) successfully parses valid input string '* id = id'")
    void testSLR1ParseAccept() {
        LRParseResponse res = lrService.parseSLR1(ASSIGN_GRAMMAR, null, "* id = id");

        assertTrue(res.isAccepted(), "'* id = id' must be accepted by SLR(1)");
        assertEquals("String accepted.", res.getMessage());
        assertFalse(res.getSteps().isEmpty());

        LRParseStep last = res.getSteps().getLast();
        assertEquals("Accept", last.getAction());
    }

    @Test
    @DisplayName("SLR(1) successfully parses valid expression on arithmetic grammar")
    void testSLR1ParseArithmetic() {
        LRParseResponse res = lrService.parseSLR1(EXPR_GRAMMAR, null, "id + id * id");

        assertTrue(res.isAccepted(), "'id + id * id' must be accepted by SLR(1)");
        assertEquals("Accept", res.getSteps().getLast().getAction());
    }

    @Test
    @DisplayName("SLR(1) rejects invalid input string 'id =' with syntax error")
    void testSLR1ParseReject() {
        LRParseResponse res = lrService.parseSLR1(ASSIGN_GRAMMAR, null, "id =");

        assertFalse(res.isAccepted(), "'id =' is incomplete and must be rejected");
        assertNotNull(res.getMessage());
        assertTrue(res.getMessage().contains("Syntax Error"));
        assertFalse(res.getSteps().isEmpty());
    }
}
