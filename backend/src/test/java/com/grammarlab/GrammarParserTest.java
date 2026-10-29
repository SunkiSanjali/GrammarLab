package com.grammarlab;

import com.grammarlab.exception.GrammarParseException;
import com.grammarlab.model.Grammar;
import com.grammarlab.model.GrammarSymbol;
import com.grammarlab.model.Production;
import com.grammarlab.parser.GrammarParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GrammarParserTest {

    private GrammarParser parser;

    @BeforeEach
    void setUp() {
        parser = new GrammarParser();
    }

    @Test
    @DisplayName("Parse classic arithmetic expression grammar with preserved production order")
    void testClassicExpressionGrammar() {
        String input = """
                E -> E + T | T
                T -> T * F | F
                F -> ( E ) | id
                """;

        Grammar grammar = parser.parse(input);

        assertNotNull(grammar);
        assertEquals("E", grammar.getStartSymbol().getName());

        // Verify Non-Terminals
        List<String> ntNames = grammar.getNonTerminals().stream().map(GrammarSymbol::getName).toList();
        assertEquals(List.of("E", "T", "F"), ntNames);

        // Verify Terminals
        List<String> tNames = grammar.getTerminals().stream().map(GrammarSymbol::getName).toList();
        assertTrue(tNames.contains("+"));
        assertTrue(tNames.contains("*"));
        assertTrue(tNames.contains("("));
        assertTrue(tNames.contains(")"));
        assertTrue(tNames.contains("id"));
        assertFalse(tNames.contains("$"));

        // Verify Productions count and ordering
        List<Production> productions = grammar.getProductions();
        assertEquals(6, productions.size());
        assertEquals("E -> E + T", productions.get(0).getFormattedRule());
        assertEquals(1, productions.get(0).getId());
        assertEquals("E -> T", productions.get(1).getFormattedRule());
        assertEquals(2, productions.get(1).getId());
        assertEquals("T -> T * F", productions.get(2).getFormattedRule());
        assertEquals("T -> F", productions.get(3).getFormattedRule());
        assertEquals("F -> ( E )", productions.get(4).getFormattedRule());
        assertEquals("F -> id", productions.get(5).getFormattedRule());
    }

    @Test
    @DisplayName("Parse grammar with epsilon productions using ε, epsilon, and #")
    void testEpsilonGrammar() {
        String input = """
                S -> A B
                A -> a | ε
                B -> b | epsilon
                """;

        Grammar grammar = parser.parse(input);
        assertEquals(5, grammar.getProductions().size());

        // A -> ε is production 2
        Production aEps = grammar.getProductions().get(2);
        assertTrue(aEps.isEpsilon());
        assertEquals("A -> ε", aEps.getFormattedRule());

        // B -> epsilon normalized to ε is production 4
        Production bEps = grammar.getProductions().get(4);
        assertTrue(bEps.isEpsilon());
        assertEquals("B -> ε", bEps.getFormattedRule());
    }

    @Test
    @DisplayName("Support multi-character symbols such as id, num, Expr, while")
    void testMultiCharacterSymbols() {
        String input = """
                Stmt -> while ( Cond ) Stmt | id = Expr ;
                Expr -> Expr + Term | num
                Cond -> id == num
                Term -> id
                """;

        Grammar grammar = parser.parse(input);

        List<String> ntNames = grammar.getNonTerminals().stream().map(GrammarSymbol::getName).toList();
        assertEquals(List.of("Stmt", "Expr", "Cond", "Term"), ntNames);

        List<String> tNames = grammar.getTerminals().stream().map(GrammarSymbol::getName).toList();
        assertTrue(tNames.contains("while"));
        assertTrue(tNames.contains("id"));
        assertTrue(tNames.contains("num"));
        assertTrue(tNames.contains("=="));
        assertTrue(tNames.contains("="));
        assertTrue(tNames.contains(";"));
        assertTrue(tNames.contains("+"));
    }

    @Test
    @DisplayName("Allow custom start symbol selection")
    void testCustomStartSymbol() {
        String input = """
                E -> T
                T -> id
                """;

        Grammar grammar = parser.parse(input, "T");
        assertEquals("T", grammar.getStartSymbol().getName());
    }

    @Test
    @DisplayName("Generate augmented grammar S' -> S with ID 0 without mutating original")
    void testAugmentedGrammar() {
        String input = """
                E -> E + T | T
                T -> id
                """;

        Grammar grammar = parser.parse(input);
        Grammar augmented = grammar.getAugmentedGrammar();

        assertEquals("E'", augmented.getStartSymbol().getName());
        assertEquals(4, augmented.getProductions().size());

        Production rootProd = augmented.getProductions().get(0);
        assertEquals(0, rootProd.getId());
        assertEquals("E' -> E", rootProd.getFormattedRule());
        assertTrue(rootProd.isAugmented());

        // Original grammar remains unmutated with 3 productions
        assertEquals(3, grammar.getProductions().size());
        assertEquals("E", grammar.getStartSymbol().getName());
    }

    @Test
    @DisplayName("Produce clean normalized string representation")
    void testNormalizedRepresentation() {
        String input = """
                E -> E + T
                E -> T
                T -> id
                """;

        Grammar grammar = parser.parse(input);
        String normalized = grammar.getNormalizedRepresentation();

        assertTrue(normalized.contains("E -> E + T | T"));
        assertTrue(normalized.contains("T -> id"));
    }
}
