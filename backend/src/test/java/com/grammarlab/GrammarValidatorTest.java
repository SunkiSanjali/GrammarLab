package com.grammarlab;

import com.grammarlab.exception.GrammarParseException;
import com.grammarlab.model.Grammar;
import com.grammarlab.parser.GrammarParser;
import com.grammarlab.validator.GrammarValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrammarValidatorTest {

    private GrammarParser parser;
    private GrammarValidator validator;

    @BeforeEach
    void setUp() {
        parser = new GrammarParser();
        validator = new GrammarValidator();
    }

    @Test
    @DisplayName("Reject empty or blank grammar input")
    void testEmptyInput() {
        assertThrows(GrammarParseException.class, () -> parser.parse(""));
        assertThrows(GrammarParseException.class, () -> parser.parse("   \n\t  "));
        assertThrows(GrammarParseException.class, () -> parser.parse(null));
    }

    @Test
    @DisplayName("Reject malformed production without '->' arrow")
    void testMissingArrow() {
        GrammarParseException ex = assertThrows(GrammarParseException.class, () ->
                parser.parse("E E + T")
        );
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Missing '->'")));
    }

    @Test
    @DisplayName("Reject multiple '->' delimiters on the same line")
    void testMultipleArrows() {
        GrammarParseException ex = assertThrows(GrammarParseException.class, () ->
                parser.parse("E -> T -> F")
        );
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Multiple '->'")));
    }

    @Test
    @DisplayName("Reject empty left-hand side")
    void testEmptyLhs() {
        GrammarParseException ex = assertThrows(GrammarParseException.class, () ->
                parser.parse("-> T")
        );
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Left-hand side non-terminal cannot be empty")));
    }

    @Test
    @DisplayName("Reject left-hand side with spaces (must be single non-terminal)")
    void testMultiSymbolLhs() {
        GrammarParseException ex = assertThrows(GrammarParseException.class, () ->
                parser.parse("E T -> id")
        );
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("single non-terminal")));
    }

    @Test
    @DisplayName("Reject epsilon on left-hand side")
    void testEpsilonOnLhs() {
        GrammarParseException ex = assertThrows(GrammarParseException.class, () ->
                parser.parse("ε -> id")
        );
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("cannot be used on the left-hand side")));
    }

    @Test
    @DisplayName("Reject invalid epsilon combined with other RHS symbols (e.g. A -> a ε)")
    void testInvalidEpsilonUsage() {
        GrammarParseException ex = assertThrows(GrammarParseException.class, () ->
                parser.parse("A -> a ε\nA -> b")
        );
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Invalid epsilon usage")));
    }

    @Test
    @DisplayName("Reject '$' symbol in grammar productions")
    void testDollarSymbolDisallowed() {
        GrammarParseException ex = assertThrows(GrammarParseException.class, () ->
                parser.parse("A -> a $ b")
        );
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("'$' is reserved")));
    }

    @Test
    @DisplayName("Reject empty alternative (e.g. E -> | T)")
    void testEmptyAlternative() {
        GrammarParseException ex = assertThrows(GrammarParseException.class, () ->
                parser.parse("E -> | T\nT -> id")
        );
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Empty alternative encountered")));
    }

    @Test
    @DisplayName("Reject invalid custom start symbol not defined in grammar")
    void testInvalidStartSymbol() {
        GrammarParseException ex = assertThrows(GrammarParseException.class, () ->
                parser.parse("E -> T\nT -> id", "NonExistent")
        );
        assertTrue(ex.getMessage().contains("not defined in any production left-hand side"));
    }

    @Test
    @DisplayName("Accept valid left-recursive and mutually recursive grammars")
    void testRecursiveGrammars() {
        String leftRecursive = """
                E -> E + T | T
                T -> T * F | F
                F -> id
                """;

        Grammar g = parser.parse(leftRecursive);
        GrammarValidator.ValidationResult result = validator.validate(g);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("Warn on unreachable non-terminals without causing hard failure")
    void testUnreachableNonTerminalWarning() {
        String grammarWithUnreachable = """
                S -> a
                Unused -> b
                """;

        Grammar g = parser.parse(grammarWithUnreachable);
        GrammarValidator.ValidationResult result = validator.validate(g);

        assertTrue(result.isValid(), "Unreachable symbols should not invalidate the grammar");
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Unused") && w.contains("unreachable")));
    }
}
