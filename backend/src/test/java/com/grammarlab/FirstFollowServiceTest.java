package com.grammarlab;

import com.grammarlab.dto.FirstFollowResponse;
import com.grammarlab.parser.GrammarParser;
import com.grammarlab.service.FirstFollowService;
import com.grammarlab.service.GrammarService;
import com.grammarlab.validator.GrammarValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FirstFollowServiceTest {

    private FirstFollowService service;

    @BeforeEach
    void setUp() {
        GrammarParser parser = new GrammarParser();
        GrammarValidator validator = new GrammarValidator();
        GrammarService grammarService = new GrammarService(parser, validator);
        service = new FirstFollowService(grammarService);
    }

    @Test
    @DisplayName("Grammar 1: Classic Left-Recursive Arithmetic Expression Grammar")
    void testClassicExpressionGrammar() {
        String grammar = """
                E -> E + T | T
                T -> T * F | F
                F -> ( E ) | id
                """;

        FirstFollowResponse res = service.compute(grammar, null);
        assertNotNull(res);
        assertTrue(res.isValid());

        // Verify FIRST sets
        assertEquals(List.of("(", "id"), res.getFirstSets().get("E"));
        assertEquals(List.of("(", "id"), res.getFirstSets().get("T"));
        assertEquals(List.of("(", "id"), res.getFirstSets().get("F"));

        // Verify Nullability
        assertTrue(res.getNullableNonTerminals().isEmpty());

        // Verify FOLLOW sets
        // FOLLOW(E) = {+, ), $}
        List<String> followE = res.getFollowSets().get("E");
        assertTrue(followE.contains("+"));
        assertTrue(followE.contains(")"));
        assertTrue(followE.contains("$"));
        assertEquals(3, followE.size());

        // FOLLOW(T) = {+, *, ), $}
        List<String> followT = res.getFollowSets().get("T");
        assertTrue(followT.contains("+"));
        assertTrue(followT.contains("*"));
        assertTrue(followT.contains(")"));
        assertTrue(followT.contains("$"));
        assertEquals(4, followT.size());

        // FOLLOW(F) = {+, *, ), $}
        List<String> followF = res.getFollowSets().get("F");
        assertTrue(followF.contains("+"));
        assertTrue(followF.contains("*"));
        assertTrue(followF.contains(")"));
        assertTrue(followF.contains("$"));
        assertEquals(4, followF.size());
    }

    @Test
    @DisplayName("Grammar 2: Multiple Nullable Symbols and Sequence Epsilon Propagation (S -> A B, A -> a | ε, B -> b | ε)")
    void testMultipleNullableSymbols() {
        String grammar = """
                S -> A B
                A -> a | ε
                B -> b | ε
                """;

        FirstFollowResponse res = service.compute(grammar, null);
        assertNotNull(res);
        assertTrue(res.isValid());

        // FIRST(S) = {a, b, ε}
        List<String> firstS = res.getFirstSets().get("S");
        assertTrue(firstS.contains("a"));
        assertTrue(firstS.contains("b"));
        assertTrue(firstS.contains("ε"));
        assertEquals(3, firstS.size());

        // FIRST(A) = {a, ε}
        List<String> firstA = res.getFirstSets().get("A");
        assertTrue(firstA.contains("a"));
        assertTrue(firstA.contains("ε"));
        assertEquals(2, firstA.size());

        // FIRST(B) = {b, ε}
        List<String> firstB = res.getFirstSets().get("B");
        assertTrue(firstB.contains("b"));
        assertTrue(firstB.contains("ε"));
        assertEquals(2, firstB.size());

        // Nullables
        assertTrue(res.getNullableNonTerminals().contains("S"));
        assertTrue(res.getNullableNonTerminals().contains("A"));
        assertTrue(res.getNullableNonTerminals().contains("B"));

        // FOLLOW(S) = {$}
        assertEquals(List.of("$"), res.getFollowSets().get("S"));

        // FOLLOW(A) = {b, $} (since B can be ε, FOLLOW(S) propagates to A; and FIRST(B)\{ε} includes b)
        List<String> followA = res.getFollowSets().get("A");
        assertTrue(followA.contains("b"));
        assertTrue(followA.contains("$"));
        assertEquals(2, followA.size());

        // FOLLOW(B) = {$} (since B is at end of S -> A B, FOLLOW(S) propagates to B)
        assertEquals(List.of("$"), res.getFollowSets().get("B"));
    }

    @Test
    @DisplayName("Grammar 3: Recursive Grammar with Epsilon (S -> A, A -> a A | ε)")
    void testRecursiveGrammarWithEpsilon() {
        String grammar = """
                S -> A
                A -> a A | ε
                """;

        FirstFollowResponse res = service.compute(grammar, null);
        assertNotNull(res);
        assertTrue(res.isValid());

        // FIRST(S) = {a, ε}
        List<String> firstS = res.getFirstSets().get("S");
        assertTrue(firstS.contains("a"));
        assertTrue(firstS.contains("ε"));
        assertEquals(2, firstS.size());

        // FIRST(A) = {a, ε}
        List<String> firstA = res.getFirstSets().get("A");
        assertTrue(firstA.contains("a"));
        assertTrue(firstA.contains("ε"));
        assertEquals(2, firstA.size());

        // FOLLOW(S) = {$}
        assertEquals(List.of("$"), res.getFollowSets().get("S"));

        // FOLLOW(A) = {$}
        assertEquals(List.of("$"), res.getFollowSets().get("A"));
    }

    @Test
    @DisplayName("Grammar 4: Non-Left-Recursive LL(1) Expression Grammar")
    void testLL1FactoredExpressionGrammar() {
        String grammar = """
                E -> T E'
                E' -> + T E' | ε
                T -> F T'
                T' -> * F T' | ε
                F -> ( E ) | id
                """;

        FirstFollowResponse res = service.compute(grammar, null);
        assertNotNull(res);

        assertEquals(List.of("(", "id"), res.getFirstSets().get("E"));
        assertEquals(List.of("+", "ε"), res.getFirstSets().get("E'"));
        assertEquals(List.of("(", "id"), res.getFirstSets().get("T"));
        assertEquals(List.of("*", "ε"), res.getFirstSets().get("T'"));
        assertEquals(List.of("(", "id"), res.getFirstSets().get("F"));

        // FOLLOW(E) = {), $}
        List<String> followE = res.getFollowSets().get("E");
        assertTrue(followE.contains(")"));
        assertTrue(followE.contains("$"));
        assertEquals(2, followE.size());

        // FOLLOW(E') = {), $}
        List<String> followEPrime = res.getFollowSets().get("E'");
        assertTrue(followEPrime.contains(")"));
        assertTrue(followEPrime.contains("$"));
        assertEquals(2, followEPrime.size());

        // FOLLOW(T) = {+, ), $}
        List<String> followT = res.getFollowSets().get("T");
        assertTrue(followT.contains("+"));
        assertTrue(followT.contains(")"));
        assertTrue(followT.contains("$"));
        assertEquals(3, followT.size());

        // FOLLOW(T') = {+, ), $}
        List<String> followTPrime = res.getFollowSets().get("T'");
        assertTrue(followTPrime.contains("+"));
        assertTrue(followTPrime.contains(")"));
        assertTrue(followTPrime.contains("$"));
        assertEquals(3, followTPrime.size());

        // FOLLOW(F) = {+, *, ), $}
        List<String> followF = res.getFollowSets().get("F");
        assertTrue(followF.contains("+"));
        assertTrue(followF.contains("*"));
        assertTrue(followF.contains(")"));
        assertTrue(followF.contains("$"));
        assertEquals(4, followF.size());
    }

    @Test
    @DisplayName("Grammar 5: 3-Chain Nullable Sequence (S -> A B C)")
    void testThreeChainNullable() {
        String grammar = """
                S -> A B C
                A -> a | ε
                B -> b | ε
                C -> c | ε
                """;

        FirstFollowResponse res = service.compute(grammar, null);

        // FIRST(S) = {a, b, c, ε}
        List<String> firstS = res.getFirstSets().get("S");
        assertTrue(firstS.contains("a"));
        assertTrue(firstS.contains("b"));
        assertTrue(firstS.contains("c"));
        assertTrue(firstS.contains("ε"));

        // FOLLOW(A) = {b, c, $}
        List<String> followA = res.getFollowSets().get("A");
        assertTrue(followA.contains("b"));
        assertTrue(followA.contains("c"));
        assertTrue(followA.contains("$"));

        // FOLLOW(B) = {c, $}
        List<String> followB = res.getFollowSets().get("B");
        assertTrue(followB.contains("c"));
        assertTrue(followB.contains("$"));

        // FOLLOW(C) = {$}
        assertEquals(List.of("$"), res.getFollowSets().get("C"));
    }
}
