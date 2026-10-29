package com.grammarlab;

import com.grammarlab.dto.LL1Conflict;
import com.grammarlab.dto.LL1ParseResponse;
import com.grammarlab.dto.LL1ParseStep;
import com.grammarlab.dto.LL1TableResponse;
import com.grammarlab.parser.GrammarParser;
import com.grammarlab.service.FirstFollowService;
import com.grammarlab.service.GrammarService;
import com.grammarlab.service.LL1Service;
import com.grammarlab.validator.GrammarValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LL1ServiceTest {

    private LL1Service ll1Service;

    @BeforeEach
    void setUp() {
        GrammarParser parser = new GrammarParser();
        GrammarValidator validator = new GrammarValidator();
        GrammarService grammarService = new GrammarService(parser, validator);
        FirstFollowService firstFollowService = new FirstFollowService(grammarService);
        ll1Service = new LL1Service(grammarService, firstFollowService);
    }

    @Test
    @DisplayName("Grammar 1: Factored Arithmetic Expression Grammar is LL(1) with 0 conflicts")
    void testValidLL1Grammar() {
        String grammar = """
                E -> T E'
                E' -> + T E' | ε
                T -> F T'
                T' -> * F T' | ε
                F -> ( E ) | id
                """;

        LL1TableResponse table = ll1Service.buildTable(grammar, null);

        assertNotNull(table);
        assertTrue(table.isValid());
        assertTrue(table.isLL1(), "Factored expression grammar must be LL(1)");
        assertTrue(table.getConflicts().isEmpty(), "No conflicts should exist for LL(1) grammar");

        // Verify presence of end-marker column '$'
        assertTrue(table.getHeaders().contains("$"));

        // Verify deterministic rows
        assertFalse(table.getRows().isEmpty());

        // Test valid input string parsing trace
        LL1ParseResponse parseValid = ll1Service.simulateParse(grammar, null, "id + id * id");
        assertTrue(parseValid.isAccepted(), "Valid expression 'id + id * id' must be accepted");
        assertFalse(parseValid.getSteps().isEmpty());

        // Verify final step is Accept
        LL1ParseStep lastStep = parseValid.getSteps().getLast();
        assertEquals("Accept", lastStep.getAction());

        // Test invalid input string parsing rejection
        LL1ParseResponse parseInvalid = ll1Service.simulateParse(grammar, null, "id + * id");
        assertFalse(parseInvalid.isAccepted(), "Malformed expression 'id + * id' must be rejected");
        assertTrue(parseInvalid.getMessage().contains("Syntax Error"));
    }

    @Test
    @DisplayName("Grammar 2: Grammar with First-First Conflict (S -> a A | a B)")
    void testFirstFirstConflict() {
        String grammar = """
                S -> a A | a B
                A -> c
                B -> d
                """;

        LL1TableResponse table = ll1Service.buildTable(grammar, null);

        assertNotNull(table);
        assertFalse(table.isLL1(), "Grammar with common prefix 'a' has First-First conflict and cannot be LL(1)");
        assertFalse(table.getConflicts().isEmpty());

        LL1Conflict conflict = table.getConflicts().getFirst();
        assertEquals("First-First Conflict", conflict.getType());
        assertEquals("S", conflict.getNonTerminal());
        assertEquals("a", conflict.getTerminal());
        assertTrue(conflict.getRules().contains("S -> a A"));
        assertTrue(conflict.getRules().contains("S -> a B"));
    }

    @Test
    @DisplayName("Grammar 3: Grammar with First-Follow Conflict (S -> A a b, A -> a | ε)")
    void testFirstFollowConflict() {
        String grammar = """
                S -> A a b
                A -> a | ε
                """;

        LL1TableResponse table = ll1Service.buildTable(grammar, null);

        assertNotNull(table);
        assertFalse(table.isLL1(), "Grammar where FIRST(A) overlaps FOLLOW(A) has First-Follow conflict and cannot be LL(1)");
        assertFalse(table.getConflicts().isEmpty());

        boolean foundFirstFollow = table.getConflicts().stream()
                .anyMatch(c -> c.getType().equals("First-Follow Conflict") && c.getNonTerminal().equals("A") && c.getTerminal().equals("a"));
        assertTrue(foundFirstFollow, "Must detect First-Follow conflict on M[A, a]");
    }

    @Test
    @DisplayName("Grammar 4: Grammar with Epsilon correctly placing rule into FOLLOW columns")
    void testEpsilonTablePlacement() {
        String grammar = """
                S -> A b
                A -> a | ε
                """;

        LL1TableResponse table = ll1Service.buildTable(grammar, null);

        assertTrue(table.isLL1());
        assertTrue(table.getConflicts().isEmpty());

        // In row A, column b (which is FOLLOW(A)), A -> ε must be placed
        var rowA = table.getRows().stream().filter(r -> r.getNonTerminal().equals("A")).findFirst().orElseThrow();
        assertEquals("A -> ε", rowA.getCells().get("b"));
        assertEquals("A -> a", rowA.getCells().get("a"));
    }

    @Test
    @DisplayName("Reject input strings with unrecognized tokens")
    void testUnrecognizedTokenRejection() {
        String grammar = """
                S -> a
                """;

        LL1ParseResponse parseUnknown = ll1Service.simulateParse(grammar, null, "a unknown a");
        assertFalse(parseUnknown.isAccepted());
        assertTrue(parseUnknown.getMessage().contains("unrecognized"));
    }
}
