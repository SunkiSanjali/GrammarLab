package com.grammarlab.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a single Context-Free Grammar production rule: A -> α
 * Preserves explicit production order and ID.
 */
public class Production {

    private final int id;
    private final GrammarSymbol lhs;
    private final List<GrammarSymbol> rhs;

    public Production(int id, GrammarSymbol lhs, List<GrammarSymbol> rhs) {
        this.id = id;
        this.lhs = Objects.requireNonNull(lhs, "LHS cannot be null.");
        if (!lhs.isNonTerminal()) {
            throw new IllegalArgumentException("LHS must be a non-terminal symbol, got: " + lhs);
        }
        if (rhs == null || rhs.isEmpty()) {
            this.rhs = List.of(GrammarSymbol.EPSILON);
        } else {
            this.rhs = Collections.unmodifiableList(rhs);
        }
    }

    public int getId() {
        return id;
    }

    public GrammarSymbol getLhs() {
        return lhs;
    }

    public List<GrammarSymbol> getRhs() {
        return rhs;
    }

    /**
     * Checks if this is an epsilon production (A -> ε).
     */
    public boolean isEpsilon() {
        return rhs.size() == 1 && rhs.getFirst().isEpsilon();
    }

    /**
     * Checks if this is the augmented production S' -> S (ID 0).
     */
    public boolean isAugmented() {
        return id == 0;
    }

    public String getFormattedRule() {
        String rhsString = rhs.stream()
                .map(GrammarSymbol::getName)
                .collect(Collectors.joining(" "));
        return lhs.getName() + " -> " + rhsString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Production other)) return false;
        return id == other.id && Objects.equals(lhs, other.lhs) && Objects.equals(rhs, other.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lhs, rhs);
    }

    @Override
    public String toString() {
        return "(" + id + ") " + getFormattedRule();
    }
}
