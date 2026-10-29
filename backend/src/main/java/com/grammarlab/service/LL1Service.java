package com.grammarlab.service;

import com.grammarlab.dto.*;
import com.grammarlab.model.Grammar;
import com.grammarlab.model.GrammarSymbol;
import com.grammarlab.model.Production;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service constructing the LL(1) predictive parsing table,
 * detecting First-First and First-Follow conflicts,
 * and performing stack-based pushdown automaton parse simulations.
 */
@Service
public class LL1Service {

    private final GrammarService grammarService;
    private final FirstFollowService firstFollowService;

    public LL1Service(GrammarService grammarService, FirstFollowService firstFollowService) {
        this.grammarService = grammarService;
        this.firstFollowService = firstFollowService;
    }

    /**
     * Builds the LL(1) parsing table and analyzes conflicts.
     */
    public LL1TableResponse buildTable(String rawGrammar, String customStartSymbol) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, customStartSymbol);
        return buildTable(grammar);
    }

    /**
     * Builds the LL(1) parsing table directly from a Grammar instance.
     */
    public LL1TableResponse buildTable(Grammar grammar) {
        Map<GrammarSymbol, Set<GrammarSymbol>> firstSets = firstFollowService.computeFirstSets(grammar);
        Map<GrammarSymbol, Set<GrammarSymbol>> followSets = firstFollowService.computeFollowSets(grammar, firstSets);

        // Columns: terminals sorted deterministically + '$' as the end-marker column
        List<String> headers = grammar.getTerminals().stream()
                .map(GrammarSymbol::getName)
                .sorted(FirstFollowService.DETERMINISTIC_SYMBOL_COMPARATOR)
                .collect(Collectors.toCollection(ArrayList::new));
        headers.add(GrammarSymbol.EOF_STR);

        // Table map: Non-Terminal -> Terminal Name -> List of matching productions
        Map<String, Map<String, List<Production>>> rawTable = new LinkedHashMap<>();
        for (GrammarSymbol nt : grammar.getNonTerminals()) {
            Map<String, List<Production>> row = new LinkedHashMap<>();
            for (String col : headers) {
                row.put(col, new ArrayList<>());
            }
            rawTable.put(nt.getName(), row);
        }

        // Fill table entries
        for (Production production : grammar.getProductions()) {
            GrammarSymbol lhs = production.getLhs();
            List<GrammarSymbol> rhs = production.getRhs();

            Set<GrammarSymbol> firstRhs = firstFollowService.computeFirstOfSequence(rhs, firstSets);

            // Rule 1: For each terminal a in FIRST(α), add A -> α to M[A, a]
            for (GrammarSymbol sym : firstRhs) {
                if (!sym.isEpsilon()) {
                    rawTable.get(lhs.getName()).get(sym.getName()).add(production);
                }
            }

            // Rule 2: If ε in FIRST(α), for each terminal/EOF b in FOLLOW(A), add A -> α to M[A, b]
            if (firstRhs.contains(GrammarSymbol.EPSILON)) {
                Set<GrammarSymbol> lhsFollow = followSets.getOrDefault(lhs, Collections.emptySet());
                for (GrammarSymbol b : lhsFollow) {
                    List<Production> cell = rawTable.get(lhs.getName()).get(b.getName());
                    if (!cell.contains(production)) {
                        cell.add(production);
                    }
                }
            }
        }

        // Conflict Detection
        List<LL1Conflict> conflicts = new ArrayList<>();
        boolean isLL1 = true;

        List<LL1TableRowDto> rows = new ArrayList<>();
        Map<String, Map<String, List<String>>> serializableRawTable = new LinkedHashMap<>();

        for (GrammarSymbol nt : grammar.getNonTerminals()) {
            String ntName = nt.getName();
            Map<String, String> formattedCells = new LinkedHashMap<>();
            Map<String, List<String>> rowRaw = new LinkedHashMap<>();

            for (String col : headers) {
                List<Production> cellProds = rawTable.get(ntName).get(col);
                List<String> prodRules = cellProds.stream().map(Production::getFormattedRule).toList();
                rowRaw.put(col, prodRules);

                if (cellProds.isEmpty()) {
                    formattedCells.put(col, "");
                } else if (cellProds.size() == 1) {
                    formattedCells.put(col, cellProds.getFirst().getFormattedRule());
                } else {
                    // Conflict detected!
                    isLL1 = false;
                    String conflictString = cellProds.stream()
                            .map(Production::getFormattedRule)
                            .collect(Collectors.joining(" / ")) + " (Conflict)";
                    formattedCells.put(col, conflictString);

                    boolean hasEps = cellProds.stream().anyMatch(Production::isEpsilon);
                    String conflictType = hasEps ? "First-Follow Conflict" : "First-First Conflict";
                    String explanation = String.format(
                            "%s in table entry M[%s, %s]: Ambiguity between %s",
                            conflictType, ntName, col, prodRules
                    );

                    conflicts.add(new LL1Conflict(conflictType, ntName, col, prodRules, explanation));
                }
            }

            rows.add(new LL1TableRowDto(ntName, formattedCells));
            serializableRawTable.put(ntName, rowRaw);
        }

        LL1TableResponse response = new LL1TableResponse();
        response.setValid(true);
        response.setLL1(isLL1);
        response.setStartSymbol(grammar.getStartSymbol().getName());
        response.setNormalizedGrammar(grammar.getNormalizedRepresentation());
        response.setHeaders(headers);
        response.setRows(rows);
        response.setRawTable(serializableRawTable);
        response.setConflicts(conflicts);

        return response;
    }

    /**
     * Simulates predictive pushdown automaton parsing of an input string
     * using the LL(1) parsing table.
     */
    public LL1ParseResponse simulateParse(String rawGrammar, String customStartSymbol, String inputString) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, customStartSymbol);
        LL1TableResponse tableResponse = buildTable(grammar);

        // Tokenize input string
        List<String> inputTokens = new ArrayList<>();
        if (inputString != null && !inputString.trim().isEmpty()) {
            String[] tokens = inputString.trim().split("\\s+");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    inputTokens.add(token);
                }
            }
        }

        // Validate tokens against grammar terminals
        Set<String> validTerminals = grammar.getTerminals().stream()
                .map(GrammarSymbol::getName)
                .collect(Collectors.toSet());

        for (String t : inputTokens) {
            if (!t.equals(GrammarSymbol.EOF_STR) && !validTerminals.contains(t)) {
                return LL1ParseResponse.failure(
                        "Input contains unrecognized terminal symbol: '" + t + "'. Allowed terminals: " + validTerminals,
                        Collections.emptyList()
                );
            }
        }

        // Ensure end marker '$'
        if (inputTokens.isEmpty() || !inputTokens.getLast().equals(GrammarSymbol.EOF_STR)) {
            inputTokens.add(GrammarSymbol.EOF_STR);
        }

        // Parsing stack initialized with: bottom = $, top = StartSymbol
        Deque<String> stack = new ArrayDeque<>();
        stack.push(GrammarSymbol.EOF_STR);
        stack.push(grammar.getStartSymbol().getName());

        List<LL1ParseStep> steps = new ArrayList<>();
        int inputPointer = 0;
        int stepCount = 1;
        int maxSteps = 1000;

        while (stepCount <= maxSteps) {
            if (stack.isEmpty()) {
                if (inputPointer >= inputTokens.size() - 1 && inputTokens.get(inputPointer).equals(GrammarSymbol.EOF_STR)) {
                    steps.add(new LL1ParseStep(stepCount, "$", "$", "Accept", "Input successfully parsed"));
                    LL1ParseResponse res = new LL1ParseResponse();
                    res.setAccepted(true);
                    res.setLL1(tableResponse.isLL1());
                    res.setMessage("String successfully accepted by LL(1) parsing.");
                    res.setSteps(steps);
                    return res;
                } else {
                    String remaining = String.join(" ", inputTokens.subList(inputPointer, inputTokens.size()));
                    steps.add(new LL1ParseStep(stepCount, "empty", remaining, "Error: Stack empty with input remaining", ""));
                    return LL1ParseResponse.failure("Syntax Error: Stack became empty before input was fully consumed.", steps);
                }
            }

            String top = stack.peek();
            String currentLookahead = inputTokens.get(inputPointer);

            String stackView = formatStack(stack);
            String remainingInput = String.join(" ", inputTokens.subList(inputPointer, inputTokens.size()));

            // 1. Success condition: both top of stack and lookahead are '$'
            if (top.equals(GrammarSymbol.EOF_STR) && currentLookahead.equals(GrammarSymbol.EOF_STR)) {
                steps.add(new LL1ParseStep(stepCount, stackView, remainingInput, "Accept", "Input successfully parsed"));
                LL1ParseResponse res = new LL1ParseResponse();
                res.setAccepted(true);
                res.setLL1(tableResponse.isLL1());
                res.setMessage("String successfully accepted by LL(1) parsing.");
                res.setSteps(steps);
                return res;
            }

            // 2. Terminal on stack
            if (top.equals(GrammarSymbol.EOF_STR) || validTerminals.contains(top)) {
                if (top.equals(currentLookahead)) {
                    steps.add(new LL1ParseStep(stepCount++, stackView, remainingInput, "Match '" + top + "'", ""));
                    stack.pop();
                    inputPointer++;
                } else {
                    String errMsg = "Syntax Error: Mismatch on stack. Expected '" + top + "', but found '" + currentLookahead + "'.";
                    steps.add(new LL1ParseStep(stepCount, stackView, remainingInput, "Error: " + errMsg, ""));
                    return LL1ParseResponse.failure(errMsg, steps);
                }
            }
            // 3. Non-Terminal on stack
            else {
                Map<String, List<String>> row = tableResponse.getRawTable().get(top);
                List<String> cellRules = (row != null) ? row.get(currentLookahead) : null;

                if (cellRules == null || cellRules.isEmpty()) {
                    String errMsg = "Syntax Error: No table entry in M[" + top + ", " + currentLookahead + "].";
                    steps.add(new LL1ParseStep(stepCount, stackView, remainingInput, "Error: " + errMsg, ""));
                    return LL1ParseResponse.failure(errMsg, steps);
                }

                // If cell has conflict, select first rule and note conflict
                String rule = cellRules.getFirst();
                String actionNote = (cellRules.size() > 1) ? "Output " + rule + " (Conflict in M[" + top + ", " + currentLookahead + "])" : "Output " + rule;

                steps.add(new LL1ParseStep(stepCount++, stackView, remainingInput, actionNote, rule));
                stack.pop();

                // Find corresponding production to push RHS
                String[] parts = rule.split("->");
                String rhsPart = parts[1].trim();

                if (!rhsPart.equals(GrammarSymbol.EPSILON_STR)) {
                    String[] rhsTokens = rhsPart.split("\\s+");
                    // Push onto stack in reverse order
                    for (int i = rhsTokens.length - 1; i >= 0; i--) {
                        stack.push(rhsTokens[i]);
                    }
                }
            }
        }

        return LL1ParseResponse.failure("Parsing terminated: Exceeded maximum step limit (" + maxSteps + "). Possible recursion loop.", steps);
    }

    private String formatStack(Deque<String> stack) {
        List<String> list = new ArrayList<>(stack);
        Collections.reverse(list);
        return String.join(" ", list);
    }
}
