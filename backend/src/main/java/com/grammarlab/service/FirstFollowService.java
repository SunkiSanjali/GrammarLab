package com.grammarlab.service;

import com.grammarlab.dto.FirstFollowItemDto;
import com.grammarlab.dto.FirstFollowResponse;
import com.grammarlab.model.Grammar;
import com.grammarlab.model.GrammarSymbol;
import com.grammarlab.model.Production;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Computes FIRST and FOLLOW sets for Context-Free Grammars
 * using fixed-point iteration until convergence.
 */
@Service
public class FirstFollowService {

    private final GrammarService grammarService;

    /**
     * Deterministic symbol comparator ensuring readable output:
     * Regular symbols sorted naturally, with 'ε' and '$' placed at the end.
     */
    public static final Comparator<String> DETERMINISTIC_SYMBOL_COMPARATOR = (s1, s2) -> {
        if (s1.equals(s2)) return 0;
        if (s1.equals(GrammarSymbol.EPSILON_STR)) return 1;
        if (s2.equals(GrammarSymbol.EPSILON_STR)) return -1;
        if (s1.equals(GrammarSymbol.EOF_STR)) return 1;
        if (s2.equals(GrammarSymbol.EOF_STR)) return -1;
        return s1.compareTo(s2);
    };

    public FirstFollowService(GrammarService grammarService) {
        this.grammarService = grammarService;
    }

    /**
     * Entry point for computing FIRST and FOLLOW sets from raw grammar text.
     */
    public FirstFollowResponse compute(String rawGrammar, String customStartSymbol) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, customStartSymbol);
        return computeForGrammar(grammar);
    }

    /**
     * Computes FIRST and FOLLOW sets directly for a parsed Grammar instance.
     */
    public FirstFollowResponse computeForGrammar(Grammar grammar) {
        Map<GrammarSymbol, Set<GrammarSymbol>> firstSets = computeFirstSets(grammar);
        Map<GrammarSymbol, Set<GrammarSymbol>> followSets = computeFollowSets(grammar, firstSets);

        FirstFollowResponse response = new FirstFollowResponse();
        response.setValid(true);
        response.setStartSymbol(grammar.getStartSymbol().getName());
        response.setNormalizedGrammar(grammar.getNormalizedRepresentation());

        List<FirstFollowItemDto> items = new ArrayList<>();
        Map<String, List<String>> firstSetsMap = new LinkedHashMap<>();
        Map<String, List<String>> followSetsMap = new LinkedHashMap<>();
        List<String> nullableList = new ArrayList<>();

        for (GrammarSymbol nt : grammar.getNonTerminals()) {
            Set<GrammarSymbol> first = firstSets.getOrDefault(nt, Collections.emptySet());
            Set<GrammarSymbol> follow = followSets.getOrDefault(nt, Collections.emptySet());

            boolean isNullable = first.contains(GrammarSymbol.EPSILON);
            if (isNullable) {
                nullableList.add(nt.getName());
            }

            List<String> sortedFirst = first.stream()
                    .map(GrammarSymbol::getName)
                    .sorted(DETERMINISTIC_SYMBOL_COMPARATOR)
                    .toList();

            List<String> sortedFollow = follow.stream()
                    .map(GrammarSymbol::getName)
                    .sorted(DETERMINISTIC_SYMBOL_COMPARATOR)
                    .toList();

            items.add(new FirstFollowItemDto(nt.getName(), isNullable, sortedFirst, sortedFollow));
            firstSetsMap.put(nt.getName(), sortedFirst);
            followSetsMap.put(nt.getName(), sortedFollow);
        }

        response.setItems(items);
        response.setFirstSets(firstSetsMap);
        response.setFollowSets(followSetsMap);
        response.setNullableNonTerminals(nullableList);

        return response;
    }

    /**
     * Computes FIRST sets for every non-terminal using fixed-point iteration.
     */
    public Map<GrammarSymbol, Set<GrammarSymbol>> computeFirstSets(Grammar grammar) {
        Map<GrammarSymbol, Set<GrammarSymbol>> first = new LinkedHashMap<>();

        // Initialize empty sets for all non-terminals
        for (GrammarSymbol nt : grammar.getNonTerminals()) {
            first.put(nt, new LinkedHashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;

            for (Production production : grammar.getProductions()) {
                GrammarSymbol lhs = production.getLhs();
                Set<GrammarSymbol> lhsFirst = first.get(lhs);
                int previousSize = lhsFirst.size();

                if (production.isEpsilon()) {
                    lhsFirst.add(GrammarSymbol.EPSILON);
                } else {
                    boolean allNullable = true;
                    for (GrammarSymbol symbol : production.getRhs()) {
                        if (symbol.isTerminal()) {
                            lhsFirst.add(symbol);
                            allNullable = false;
                            break;
                        } else if (symbol.isNonTerminal()) {
                            Set<GrammarSymbol> symFirst = first.get(symbol);
                            for (GrammarSymbol f : symFirst) {
                                if (!f.isEpsilon()) {
                                    lhsFirst.add(f);
                                }
                            }
                            if (!symFirst.contains(GrammarSymbol.EPSILON)) {
                                allNullable = false;
                                break;
                            }
                        }
                    }
                    if (allNullable) {
                        lhsFirst.add(GrammarSymbol.EPSILON);
                    }
                }

                if (lhsFirst.size() > previousSize) {
                    changed = true;
                }
            }
        }

        return first;
    }

    /**
     * Computes the FIRST set of an arbitrary sequence of symbols α = X_1 X_2 ... X_k.
     */
    public Set<GrammarSymbol> computeFirstOfSequence(
            List<GrammarSymbol> sequence,
            Map<GrammarSymbol, Set<GrammarSymbol>> firstSets
    ) {
        Set<GrammarSymbol> result = new LinkedHashSet<>();
        if (sequence == null || sequence.isEmpty()) {
            result.add(GrammarSymbol.EPSILON);
            return result;
        }

        boolean allNullable = true;
        for (GrammarSymbol symbol : sequence) {
            if (symbol.isEpsilon()) {
                continue;
            }
            if (symbol.isTerminal() || symbol.isEof()) {
                result.add(symbol);
                allNullable = false;
                break;
            }
            if (symbol.isNonTerminal()) {
                Set<GrammarSymbol> symFirst = firstSets.getOrDefault(symbol, Collections.emptySet());
                for (GrammarSymbol f : symFirst) {
                    if (!f.isEpsilon()) {
                        result.add(f);
                    }
                }
                if (!symFirst.contains(GrammarSymbol.EPSILON)) {
                    allNullable = false;
                    break;
                }
            }
        }

        if (allNullable) {
            result.add(GrammarSymbol.EPSILON);
        }

        return result;
    }

    /**
     * Computes FOLLOW sets for every non-terminal using fixed-point iteration.
     */
    public Map<GrammarSymbol, Set<GrammarSymbol>> computeFollowSets(
            Grammar grammar,
            Map<GrammarSymbol, Set<GrammarSymbol>> firstSets
    ) {
        Map<GrammarSymbol, Set<GrammarSymbol>> follow = new LinkedHashMap<>();

        // Initialize empty sets for all non-terminals
        for (GrammarSymbol nt : grammar.getNonTerminals()) {
            follow.put(nt, new LinkedHashSet<>());
        }

        // Rule 1: FOLLOW(StartSymbol) contains $
        follow.get(grammar.getStartSymbol()).add(GrammarSymbol.EOF);

        // Fixed-point iteration
        boolean changed = true;
        while (changed) {
            changed = false;

            for (Production production : grammar.getProductions()) {
                GrammarSymbol lhs = production.getLhs();
                Set<GrammarSymbol> lhsFollow = follow.get(lhs);
                List<GrammarSymbol> rhs = production.getRhs();

                if (production.isEpsilon()) {
                    continue;
                }

                for (int i = 0; i < rhs.size(); i++) {
                    GrammarSymbol current = rhs.get(i);

                    if (current.isNonTerminal()) {
                        Set<GrammarSymbol> currentFollow = follow.get(current);
                        int previousSize = currentFollow.size();

                        // Suffix β = X_{i+1} ... X_k
                        List<GrammarSymbol> beta = rhs.subList(i + 1, rhs.size());
                        Set<GrammarSymbol> firstBeta = computeFirstOfSequence(beta, firstSets);

                        // Rule 2: Add all non-epsilon symbols from FIRST(β) to FOLLOW(current)
                        for (GrammarSymbol f : firstBeta) {
                            if (!f.isEpsilon()) {
                                currentFollow.add(f);
                            }
                        }

                        // Rule 3: If β is nullable (or empty), add FOLLOW(lhs) to FOLLOW(current)
                        if (firstBeta.contains(GrammarSymbol.EPSILON)) {
                            currentFollow.addAll(lhsFollow);
                        }

                        if (currentFollow.size() > previousSize) {
                            changed = true;
                        }
                    }
                }
            }
        }

        return follow;
    }
}
