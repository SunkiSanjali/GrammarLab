package com.grammarlab.validator;

import com.grammarlab.model.Grammar;
import com.grammarlab.model.GrammarSymbol;
import com.grammarlab.model.Production;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates semantic integrity of context-free grammars.
 * Detects:
 * - Missing or undefined start symbol
 * - Non-terminals without productions
 * - Conflicting terminal/non-terminal definitions
 * - Unreachable non-terminals (reported as warnings)
 * - Non-generating / non-terminating symbols (reported as warnings)
 * Allows valid recursive and left-recursive grammars.
 */
@Component
public class GrammarValidator {

    public ValidationResult validate(Grammar grammar) {
        if (grammar == null) {
            return new ValidationResult(false, List.of("Grammar cannot be null."), Collections.emptyList());
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Validate Start Symbol
        GrammarSymbol start = grammar.getStartSymbol();
        if (start == null) {
            errors.add("Start symbol is not defined.");
        } else if (!grammar.getNonTerminals().contains(start)) {
            errors.add("Start symbol '" + start.getName() + "' is not present in defined non-terminals.");
        }

        // 2. Validate Non-Terminals have productions
        for (GrammarSymbol nt : grammar.getNonTerminals()) {
            List<Production> prods = grammar.getProductionsFor(nt);
            if (prods.isEmpty()) {
                errors.add("Non-terminal '" + nt.getName() + "' is referenced but has no productions defined.");
            }
        }

        // 3. Validate Symbol Separation (no symbol is both NT and Terminal)
        Set<String> ntNames = new HashSet<>();
        for (GrammarSymbol nt : grammar.getNonTerminals()) {
            ntNames.add(nt.getName());
        }

        for (GrammarSymbol t : grammar.getTerminals()) {
            if (ntNames.contains(t.getName())) {
                errors.add("Symbol conflict: '" + t.getName() + "' is defined as both a non-terminal and a terminal.");
            }
            if (t.getName().equals("$")) {
                errors.add("'$' is reserved as the end-of-input marker and cannot be a terminal symbol.");
            }
            if (t.getName().equals(GrammarSymbol.EPSILON_STR)) {
                errors.add("'ε' cannot be declared as a regular terminal symbol.");
            }
        }

        // 4. Reachability Analysis (Warning)
        Set<GrammarSymbol> reachable = computeReachableSymbols(grammar);
        for (GrammarSymbol nt : grammar.getNonTerminals()) {
            if (!reachable.contains(nt) && !nt.equals(start)) {
                warnings.add("Warning: Non-terminal '" + nt.getName() + "' is unreachable from start symbol '" + start.getName() + "'.");
            }
        }

        // 5. Generating / Productivity Analysis (Warning)
        Set<GrammarSymbol> generating = computeGeneratingSymbols(grammar);
        for (GrammarSymbol nt : grammar.getNonTerminals()) {
            if (!generating.contains(nt)) {
                warnings.add("Warning: Non-terminal '" + nt.getName() + "' is non-terminating (cannot derive any terminal string).");
            }
        }

        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings);
    }

    private Set<GrammarSymbol> computeReachableSymbols(Grammar grammar) {
        Set<GrammarSymbol> reachable = new HashSet<>();
        if (grammar.getStartSymbol() == null) return reachable;

        Queue<GrammarSymbol> queue = new ArrayDeque<>();
        reachable.add(grammar.getStartSymbol());
        queue.add(grammar.getStartSymbol());

        while (!queue.isEmpty()) {
            GrammarSymbol current = queue.poll();
            for (Production p : grammar.getProductionsFor(current)) {
                for (GrammarSymbol rhsSym : p.getRhs()) {
                    if (rhsSym.isNonTerminal() && reachable.add(rhsSym)) {
                        queue.add(rhsSym);
                    }
                }
            }
        }
        return reachable;
    }

    private Set<GrammarSymbol> computeGeneratingSymbols(Grammar grammar) {
        Set<GrammarSymbol> generating = new HashSet<>();
        // All terminals and epsilon are generating
        generating.addAll(grammar.getTerminals());
        generating.add(GrammarSymbol.EPSILON);

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Production p : grammar.getProductions()) {
                GrammarSymbol lhs = p.getLhs();
                if (generating.contains(lhs)) continue;

                // Check if all RHS symbols are generating
                boolean allGenerating = true;
                for (GrammarSymbol s : p.getRhs()) {
                    if (!generating.contains(s)) {
                        allGenerating = false;
                        break;
                    }
                }

                if (allGenerating) {
                    generating.add(lhs);
                    changed = true;
                }
            }
        }
        return generating;
    }

    public record ValidationResult(boolean isValid, List<String> errors, List<String> warnings) {}
}
