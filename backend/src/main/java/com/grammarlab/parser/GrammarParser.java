package com.grammarlab.parser;

import com.grammarlab.exception.GrammarParseException;
import com.grammarlab.model.Grammar;
import com.grammarlab.model.GrammarSymbol;
import com.grammarlab.model.Production;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Parses user-provided context-free grammar specifications.
 * Supports:
 * - Line-by-line productions
 * - Alternatives separated by '|'
 * - Space-delimited RHS symbols (e.g. 'E + T', 'while ( cond )')
 * - Multi-character symbols ('id', 'num', 'Expr')
 * - Epsilon representations normalized to 'ε'
 * - Preserves exact production order for reduction numbering
 */
@Component
public class GrammarParser {

    private static final String ARROW_STANDARD = "->";
    private static final String ARROW_UNICODE = "→";
    private static final Set<String> EPSILON_VARIANTS = Set.of("ε", "epsilon", "EPSILON", "#");

    /**
     * Parses raw grammar text with the default start symbol (first LHS symbol).
     */
    public Grammar parse(String rawGrammar) {
        return parse(rawGrammar, null);
    }

    /**
     * Parses raw grammar text with an optional custom start symbol.
     */
    public Grammar parse(String rawGrammar, String customStartSymbol) {
        if (rawGrammar == null || rawGrammar.trim().isEmpty()) {
            throw new GrammarParseException("Grammar input cannot be empty.");
        }

        List<String> errors = new ArrayList<>();
        List<RawRule> rawRules = new ArrayList<>();
        LinkedHashSet<String> lhsNames = new LinkedHashSet<>();

        String[] lines = rawGrammar.split("\\r?\\n");
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;
            String trimmed = line.trim();

            // Skip empty lines and comment lines
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
                continue;
            }

            // Normalize arrow
            String workingLine = trimmed.replace(ARROW_UNICODE, ARROW_STANDARD);
            if (!workingLine.contains(ARROW_STANDARD)) {
                errors.add("Line " + lineNumber + ": Invalid production format. Missing '->' delimiter in: '" + trimmed + "'");
                continue;
            }

            String[] arrowParts = workingLine.split(ARROW_STANDARD, -1);
            if (arrowParts.length > 2) {
                errors.add("Line " + lineNumber + ": Multiple '->' delimiters encountered in: '" + trimmed + "'");
                continue;
            }

            String lhs = arrowParts[0].trim();
            String rhs = arrowParts[1].trim();

            // Validate LHS
            if (lhs.isEmpty()) {
                errors.add("Line " + lineNumber + ": Left-hand side non-terminal cannot be empty.");
                continue;
            }

            if (lhs.contains(" ") || lhs.contains("\t")) {
                errors.add("Line " + lineNumber + ": Left-hand side must be a single non-terminal symbol without spaces, found: '" + lhs + "'");
                continue;
            }

            if (lhs.equals("$")) {
                errors.add("Line " + lineNumber + ": '$' is reserved as the end-of-input marker and cannot be used as a left-hand side symbol.");
                continue;
            }

            if (isEpsilonToken(lhs)) {
                errors.add("Line " + lineNumber + ": Epsilon ('ε') represents the empty string and cannot be used on the left-hand side.");
                continue;
            }

            lhsNames.add(lhs);

            // Parse RHS alternatives separated by '|'
            String[] alternatives = rhs.split("\\|", -1);
            List<List<String>> parsedAlternatives = new ArrayList<>();

            for (String alt : alternatives) {
                String altTrimmed = alt.trim();
                if (altTrimmed.isEmpty()) {
                    errors.add("Line " + lineNumber + ": Empty alternative encountered in production for '" + lhs + "'. Use 'ε' explicitly for empty productions.");
                    continue;
                }

                String[] rawTokens = altTrimmed.split("\\s+");
                List<String> tokenList = new ArrayList<>();
                boolean containsEpsilon = false;

                for (String token : rawTokens) {
                    if (token.isEmpty()) continue;

                    if (token.equals("$")) {
                        errors.add("Line " + lineNumber + ": '$' is reserved as the end-of-input marker and cannot be used as a grammar symbol.");
                        continue;
                    }

                    if (isEpsilonToken(token)) {
                        containsEpsilon = true;
                        tokenList.add(GrammarSymbol.EPSILON_STR);
                    } else {
                        tokenList.add(token);
                    }
                }

                if (containsEpsilon && tokenList.size() > 1) {
                    errors.add("Line " + lineNumber + ": Invalid epsilon usage in '" + altTrimmed + "'. 'ε' represents an empty production and cannot be combined with other symbols.");
                    continue;
                }

                if (!tokenList.isEmpty()) {
                    parsedAlternatives.add(tokenList);
                }
            }

            if (!parsedAlternatives.isEmpty()) {
                rawRules.add(new RawRule(lineNumber, lhs, parsedAlternatives));
            }
        }

        if (!errors.isEmpty()) {
            throw new GrammarParseException(errors);
        }

        if (lhsNames.isEmpty()) {
            throw new GrammarParseException("No valid productions found in grammar input.");
        }

        // Determine Start Symbol
        String effectiveStartName;
        if (customStartSymbol != null && !customStartSymbol.trim().isEmpty()) {
            effectiveStartName = customStartSymbol.trim();
            if (!lhsNames.contains(effectiveStartName)) {
                throw new GrammarParseException("Specified start symbol '" + effectiveStartName + "' is not defined in any production left-hand side.");
            }
        } else {
            effectiveStartName = lhsNames.iterator().next();
        }

        // Build Non-Terminals
        Map<String, GrammarSymbol> nonTerminalMap = new LinkedHashMap<>();
        for (String name : lhsNames) {
            nonTerminalMap.put(name, GrammarSymbol.nonTerminal(name));
        }

        GrammarSymbol startSymbol = nonTerminalMap.get(effectiveStartName);

        // Identify Terminals (any symbol on RHS not in nonTerminalMap and not epsilon)
        Map<String, GrammarSymbol> terminalMap = new LinkedHashMap<>();
        for (RawRule rule : rawRules) {
            for (List<String> alt : rule.alternatives) {
                for (String symName : alt) {
                    if (!symName.equals(GrammarSymbol.EPSILON_STR) && !nonTerminalMap.containsKey(symName)) {
                        terminalMap.putIfAbsent(symName, GrammarSymbol.terminal(symName));
                    }
                }
            }
        }

        // Construct 1-based Production list preserving user ordering
        List<Production> productions = new ArrayList<>();
        int productionId = 1;

        for (RawRule rule : rawRules) {
            GrammarSymbol ruleLhs = nonTerminalMap.get(rule.lhs);
            for (List<String> alt : rule.alternatives) {
                List<GrammarSymbol> rhsSymbols = new ArrayList<>();
                for (String symName : alt) {
                    if (symName.equals(GrammarSymbol.EPSILON_STR)) {
                        rhsSymbols.add(GrammarSymbol.EPSILON);
                    } else if (nonTerminalMap.containsKey(symName)) {
                        rhsSymbols.add(nonTerminalMap.get(symName));
                    } else {
                        rhsSymbols.add(terminalMap.get(symName));
                    }
                }
                productions.add(new Production(productionId++, ruleLhs, rhsSymbols));
            }
        }

        return new Grammar(
                startSymbol,
                new LinkedHashSet<>(nonTerminalMap.values()),
                new LinkedHashSet<>(terminalMap.values()),
                productions
        );
    }

    private static boolean isEpsilonToken(String token) {
        return EPSILON_VARIANTS.contains(token);
    }

    private record RawRule(int lineNumber, String lhs, List<List<String>> alternatives) {}
}
