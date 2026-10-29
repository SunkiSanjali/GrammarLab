package com.grammarlab.model;

import java.util.Objects;

/**
 * Represents a symbol in a Context-Free Grammar.
 * Can be a Non-Terminal, Terminal, Epsilon (empty string), or End-Of-Input ($).
 */
public class GrammarSymbol implements Comparable<GrammarSymbol> {

    public static final String EPSILON_STR = "ε";
    public static final String EOF_STR = "$";

    public static final GrammarSymbol EPSILON = new GrammarSymbol(EPSILON_STR, SymbolType.EPSILON);
    public static final GrammarSymbol EOF = new GrammarSymbol(EOF_STR, SymbolType.EOF);

    private final String name;
    private final SymbolType type;

    public GrammarSymbol(String name, SymbolType type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol name cannot be null or empty.");
        }
        this.name = name.trim();
        this.type = Objects.requireNonNull(type, "Symbol type cannot be null.");
    }

    public static GrammarSymbol nonTerminal(String name) {
        return new GrammarSymbol(name, SymbolType.NON_TERMINAL);
    }

    public static GrammarSymbol terminal(String name) {
        return new GrammarSymbol(name, SymbolType.TERMINAL);
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public boolean isNonTerminal() {
        return type == SymbolType.NON_TERMINAL;
    }

    public boolean isTerminal() {
        return type == SymbolType.TERMINAL;
    }

    public boolean isEpsilon() {
        return type == SymbolType.EPSILON;
    }

    public boolean isEof() {
        return type == SymbolType.EOF;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GrammarSymbol other)) return false;
        return Objects.equals(name, other.name) && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(GrammarSymbol o) {
        if (this.type != o.type) {
            return this.type.compareTo(o.type);
        }
        return this.name.compareTo(o.name);
    }
}
