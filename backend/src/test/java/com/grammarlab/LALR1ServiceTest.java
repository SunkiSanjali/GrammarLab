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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LALR(1) state merging, lookahead unioning, merged state info,
 * table generation, and verification of a grammar that is LR(1) but NOT LALR(1).
 */
class LALR1ServiceTest {

    private LRService lrService;

    // Classic LALR(1) grammar where state merging successfully collapses CLR states
    //   S -> C C
    //   C -> c C | d
    private static final String LALR1_VALID_GRAMMAR = """
            S -> C C
            C -> c C | d
            """;

    // Textbook grammar that is CLR(1) (0 conflicts) but NOT LALR(1) (Reduce/Reduce conflict)
    // S -> a A c | b B c | a B d | b A d
    // A -> e
    // B -> e
    // In CLR(1): states for e with lookaheads {c} and {d} are kept separate -> No conflict.
    // In LALR(1): merging states with core e• unions lookaheads to {c, d} -> Reduce/Reduce conflict!
    private static final String LR1_NOT_LALR1_GRAMMAR = """
            S -> a A c | b B c | a B d | b A d
            A -> e
            B -> e
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
    @DisplayName("LALR(1) merges CLR(1) states sharing identical LR(0) cores into fewer states")
    void testLALR1StateMerging() {
        LRTableResponse clrResp = lrService.buildCLR1Table(LALR1_VALID_GRAMMAR, null);
        LRTableResponse lalrResp = lrService.buildLALR1Table(LALR1_VALID_GRAMMAR, null);

        assertNotNull(lalrResp);
        assertTrue(lalrResp.isValid());
        assertEquals("LALR(1)", lalrResp.getParserType());
        assertTrue(lalrResp.isDeterministic(), "S -> C C, C -> c C | d must be deterministic in LALR(1)");
        assertTrue(lalrResp.getConflicts().isEmpty());

        // Merged state count must be strictly less than CLR(1) state count
        int clrStates = clrResp.getItemSets().size();
        int lalrStates = lalrResp.getItemSets().size();
        assertTrue(lalrStates < clrStates,
                "LALR(1) must merge states: LALR(" + lalrStates + ") < CLR(" + clrStates + ")");

        // mergedPairs must describe the merged clusters
        assertNotNull(lalrResp.getMergedPairs());
        assertFalse(lalrResp.getMergedPairs().isEmpty(), "mergedPairs must describe which states were collapsed");
    }

    @Test
    @DisplayName("LALR(1) verifies LR(1)-but-not-LALR(1) grammar: 0 conflicts in CLR(1), Reduce/Reduce conflict in LALR(1)")
    void testLR1ButNotLALR1Grammar() {
        // 1. In CLR(1), grammar has 0 conflicts and is deterministic
        LRTableResponse clrResp = lrService.buildCLR1Table(LR1_NOT_LALR1_GRAMMAR, null);
        assertTrue(clrResp.isDeterministic(), "Grammar must be deterministic in CLR(1)");
        assertTrue(clrResp.getConflicts().isEmpty(), "Grammar must have NO conflicts in CLR(1)");

        // 2. In LALR(1), state merging causes a Reduce/Reduce conflict
        LRTableResponse lalrResp = lrService.buildLALR1Table(LR1_NOT_LALR1_GRAMMAR, null);
        assertFalse(lalrResp.isDeterministic(), "Grammar must NOT be deterministic in LALR(1)");
        assertFalse(lalrResp.getConflicts().isEmpty(), "LALR(1) must detect conflict caused by state merging");

        // Verify that the conflict is Reduce / Reduce
        boolean hasRRConflict = lalrResp.getConflicts().stream()
                .anyMatch(c -> "Reduce / Reduce Conflict".equals(c.getType()));
        assertTrue(hasRRConflict, "Merging must introduce a Reduce / Reduce Conflict in LALR(1)");
    }

    @Test
    @DisplayName("LALR(1) parses valid input string 'c d c d' with complete trace")
    void testLALR1ParseAccept() {
        LRParseResponse res = lrService.parseLALR1(LALR1_VALID_GRAMMAR, null, "c d c d");

        assertTrue(res.isAccepted(), "'c d c d' must be accepted by LALR(1)");
        assertEquals("String accepted.", res.getMessage());
        assertFalse(res.getSteps().isEmpty());

        LRParseStep last = res.getSteps().getLast();
        assertEquals("Accept", last.getAction());
    }

    @Test
    @DisplayName("LALR(1) rejects invalid input string 'c' with syntax error")
    void testLALR1ParseReject() {
        LRParseResponse res = lrService.parseLALR1(LALR1_VALID_GRAMMAR, null, "c");

        assertFalse(res.isAccepted(), "'c' must be rejected by LALR(1)");
        assertTrue(res.getMessage().contains("Syntax Error"));
    }
}
