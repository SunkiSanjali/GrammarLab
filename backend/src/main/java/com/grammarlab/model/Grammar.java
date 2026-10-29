package com.grammarlab.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a normalized Context-Free Grammar G = (V_N, V_T, P, S).
 * Preserves exact production ordering and symbol classification.
 */
public class Grammar {

    private final GrammarSymbol startSymbol;
    private final Set<GrammarSymbol> nonTerminals;
    private final Set<GrammarSymbol> terminals;
    private final List<Production> productions;
    private final Map<GrammarSymbol, List<Production>> productionsByLhs;

    public Grammar(
            GrammarSymbol startSymbol,
            Set<GrammarSymbol> nonTerminals,
            Set<GrammarSymbol> terminals,
            List<Production> productions
    ) {
        this.startSymbol = Objects.requireNonNull(startSymbol, "Start symbol cannot be null.");
        this.nonTerminals = Collections.unmodifiableSet(new LinkedHashSet<>(nonTerminals));
        this.terminals = Collections.unmodifiableSet(new LinkedHashSet<>(terminals));
        this.productions = Collections.unmodifiableList(new ArrayList<>(productions));

        Map<GrammarSymbol, List<Production>> map = new LinkedHashMap<>();
        for (GrammarSymbol nt : this.nonTerminals) {
            map.put(nt, new ArrayList<>());
        }
        for (Production p : this.productions) {
            map.computeIfAbsent(p.getLhs(), k -> new ArrayList<>()).add(p);
        }
        this.productionsByLhs = Collections.unmodifiableMap(map);
    }

    public GrammarSymbol getStartSymbol() {
        return startSymbol;
    }

    public Set<GrammarSymbol> getNonTerminals() {
        return nonTerminals;
    }

    public Set<GrammarSymbol> getTerminals() {
        return terminals;
    }

    public List<Production> getProductions() {
        return productions;
    }

    public List<Production> getProductionsFor(GrammarSymbol lhs) {
        return productionsByLhs.getOrDefault(lhs, Collections.emptyList());
    }

    public Optional<Production> getProductionById(int id) {
        return productions.stream().filter(p -> p.getId() == id).findFirst();
    }

    /**
     * Constructs an augmented grammar G' = (V_N U {S'}, V_T, P U {S' -> S}, S')
     * used internally for LR parsing.
     * The original grammar is not mutated.
     */
    public Grammar getAugmentedGrammar() {
        String augmentedName = startSymbol.getName() + "'";
        // If startSymbol already had single quote, ensure uniqueness
        while (containsSymbolNamed(augmentedName)) {
            augmentedName += "'";
        }

        GrammarSymbol augmentedStart = GrammarSymbol.nonTerminal(augmentedName);

        Set<GrammarSymbol> newNonTerminals = new LinkedHashSet<>();
        newNonTerminals.add(augmentedStart);
        newNonTerminals.addAll(this.nonTerminals);

        List<Production> newProductions = new ArrayList<>();
        // Production 0: S' -> S
        newProductions.add(new Production(0, augmentedStart, List.of(this.startSymbol)));
        newProductions.addAll(this.productions);

        return new Grammar(augmentedStart, newNonTerminals, this.terminals, newProductions);
    }

    private boolean containsSymbolNamed(String name) {
        return nonTerminals.stream().anyMatch(s -> s.getName().equals(name)) ||
               terminals.stream().anyMatch(s -> s.getName().equals(name));
    }

    /**
     * Produces a clean, normalized multi-line string representation of the grammar.
     * Groups productions with identical LHS: A -> α1 | α2
     */
    public String getNormalizedRepresentation() {
        StringBuilder sb = new StringBuilder();
        for (GrammarSymbol nt : nonTerminals) {
            List<Production> prods = getProductionsFor(nt);
            if (prods.isEmpty()) continue;

            sb.append(nt.getName()).append(" -> ");
            String altString = prods.stream()
                    .map(p -> p.getRhs().stream()
                            .map(GrammarSymbol::getName)
                            .collect(Collectors.joining(" ")))
                    .collect(Collectors.joining(" | "));
            sb.append(altString).append("\n");
        }
        return sb.toString().trim();
    }
}
