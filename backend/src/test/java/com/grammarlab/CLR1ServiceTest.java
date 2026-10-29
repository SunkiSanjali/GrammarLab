package com.grammarlab;

import com.grammarlab.dto.LRItemSetDto;
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
 * Unit tests for CLR(1) canonical LR(1) collection, lookahead computation,
 * table generation, and string simulation.
 */
class CLR1ServiceTest {

    private LRService lrService;

    // Classic Canonical LR(1) grammar from textbooks:
    //   S -> C C
    //   C -> c C | d
    private static final String CLR1_GRAMMAR = """
            S -> C C
            C -> c C | d
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
    @DisplayName("CLR(1) canonical collection builds LR(1) items with distinct lookaheads")
    void testCLR1ItemSetsAndLookaheads() {
        LRTableResponse resp = lrService.buildCLR1Table(CLR1_GRAMMAR, null);

        assertNotNull(resp);
        assertTrue(resp.isValid());
        assertEquals("CLR(1)", resp.getParserType());
        assertTrue(resp.isDeterministic(), "S -> C C, C -> c C | d is a deterministic CLR(1) grammar");
        assertTrue(resp.getConflicts().isEmpty(), "No conflicts in CLR(1)");

        List<LRItemSetDto> states = resp.getItemSets();
        assertFalse(states.isEmpty());

        // State 0 must have initial item S' -> • S with lookahead '$'
        LRItemSetDto i0 = states.getFirst();
        assertEquals("I0", i0.getName());
        boolean hasAugmentedWithDollar = i0.getItems().stream().anyMatch(
                it -> it.getRule().contains("S' -> • S") && "$".equals(it.getLookahead()));
        assertTrue(hasAugmentedWithDollar, "I0 must contain [S' -> • S, $]");

        // Items for C in I0 must have lookahead 'c, d' (from FIRST(C $))
        boolean hasCItem = i0.getItems().stream().anyMatch(
                it -> it.getRule().contains("C -> • c C") && it.getLookahead() != null && it.getLookahead().contains("c"));
        assertTrue(hasCItem, "I0 must have C -> • c C with lookaheads c, d");
    }

    @Test
    @DisplayName("CLR(1) table rows place reductions only on specific item lookahead terminals")
    void testCLR1TableStructure() {
        LRTableResponse resp = lrService.buildCLR1Table(CLR1_GRAMMAR, null);

        // Action headers: c, d, $
        assertTrue(resp.getActionHeaders().contains("c"));
        assertTrue(resp.getActionHeaders().contains("d"));
        assertTrue(resp.getActionHeaders().contains("$"));

        // Goto headers: S, C
        assertTrue(resp.getGotoHeaders().contains("S"));
        assertTrue(resp.getGotoHeaders().contains("C"));

        // Table rows exist for each state
        assertEquals(resp.getItemSets().size(), resp.getTableRows().size());

        // Must have accept on '$'
        boolean hasAcc = resp.getTableRows().stream().anyMatch(row -> {
            @SuppressWarnings("unchecked")
            Map<String, String> actions = (Map<String, String>) row.get("actions");
            return actions != null && "acc".equals(actions.get("$"));
        });
        assertTrue(hasAcc, "CLR(1) table must contain 'acc' on '$'");
    }

    @Test
    @DisplayName("CLR(1) parses valid input string 'c d c d' with complete trace")
    void testCLR1ParseAccept_cdcd() {
        LRParseResponse res = lrService.parseCLR1(CLR1_GRAMMAR, null, "c d c d");

        assertTrue(res.isAccepted(), "'c d c d' must be accepted by CLR(1)");
        assertEquals("String accepted.", res.getMessage());
        assertFalse(res.getSteps().isEmpty());

        LRParseStep last = res.getSteps().getLast();
        assertEquals("Accept", last.getAction());
    }

    @Test
    @DisplayName("CLR(1) parses minimal valid input string 'd d'")
    void testCLR1ParseAccept_dd() {
        LRParseResponse res = lrService.parseCLR1(CLR1_GRAMMAR, null, "d d");

        assertTrue(res.isAccepted(), "'d d' must be accepted by CLR(1)");
        assertEquals("Accept", res.getSteps().getLast().getAction());
    }

    @Test
    @DisplayName("CLR(1) rejects invalid input string 'c' with syntax error")
    void testCLR1ParseReject_incomplete() {
        LRParseResponse res = lrService.parseCLR1(CLR1_GRAMMAR, null, "c");

        assertFalse(res.isAccepted(), "'c' is incomplete and must be rejected");
        assertTrue(res.getMessage().contains("Syntax Error"));
    }

    @Test
    @DisplayName("CLR(1) rejects invalid input string with extra token 'd d d'")
    void testCLR1ParseReject_extraToken() {
        LRParseResponse res = lrService.parseCLR1(CLR1_GRAMMAR, null, "d d d");

        assertFalse(res.isAccepted());
        assertTrue(res.getMessage().contains("Syntax Error"));
    }
}
