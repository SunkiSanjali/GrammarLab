package com.grammarlab.service;

import com.grammarlab.dto.*;
import com.grammarlab.model.Grammar;
import com.grammarlab.model.GrammarSymbol;
import com.grammarlab.model.Production;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements LR(0) and SLR(1) parser construction.
 *
 * <p>Algorithm outline (both parsers share the same item sets and transitions):
 * <ol>
 *   <li>Augment the grammar: add S' -> S with production id 0.</li>
 *   <li>Represent each LR(0) item as (production, dotPosition).</li>
 *   <li>CLOSURE: given a kernel set, repeatedly add items for non-terminals
 *       immediately after a dot.</li>
 *   <li>GOTO(I, X): advance the dot over X in every item of I, then close.</li>
 *   <li>Canonical collection: start from CLOSURE({S' -> •S}) and apply GOTO
 *       for every grammar symbol until no new states appear.</li>
 *   <li>LR(0) ACTION table: shift into every successor state on terminals;
 *       reduce by production p on ALL terminals/$ when the dot is at the end;
 *       accept when the dot is at the end of S' -> S.</li>
 *   <li>SLR(1) ACTION table: same shifts; reduce by p only on terminals in
 *       FOLLOW(A) where A is the LHS of p.</li>
 *   <li>Both: GOTO table entries for non-terminals.</li>
 *   <li>Conflict detection: any cell that gets two different actions is a
 *       shift/reduce or reduce/reduce conflict.</li>
 *   <li>Table-driven parsing: standard LR driver loop with a state stack and
 *       a symbol stack; produces a step-by-step trace.</li>
 * </ol>
 */
@Service
public class LRService {

    // ───────────────────────────────────────────────────────── inner types ──

    /**
     * An LR(0) item: a production with a dot inserted at position {@code dot}.
     * dot == 0 means the dot is before the first symbol; dot == rhs.size() means
     * the dot is after the last symbol (a "complete" item).
     *
     * <p>For epsilon productions (A -> ε) the rhs list is [ε] but logically
     * the only useful dot position is 0 (complete immediately).
     */
    private record LR0Item(Production production, int dot) {

        /** True when the dot has passed all RHS symbols (complete item). */
        boolean isComplete() {
            List<GrammarSymbol> rhs = production.getRhs();
            if (production.isEpsilon()) return true;   // A -> ε is always complete
            return dot >= rhs.size();
        }

        /**
         * The symbol immediately after the dot, or null when the item is complete.
         */
        GrammarSymbol symbolAfterDot() {
            if (isComplete()) return null;
            return production.getRhs().get(dot);
        }

        /** Returns a new item with the dot advanced one position. */
        LR0Item advance() {
            return new LR0Item(production, dot + 1);
        }

        /** Human-readable display with bullet (•) for the dot. */
        String format() {
            List<GrammarSymbol> rhs = production.getRhs();
            StringBuilder sb = new StringBuilder();
            sb.append(production.getLhs().getName()).append(" -> ");
            if (production.isEpsilon()) {
                sb.append("• ε");
            } else {
                for (int i = 0; i < rhs.size(); i++) {
                    if (i == dot) sb.append("• ");
                    sb.append(rhs.get(i).getName()).append(" ");
                }
                if (dot == rhs.size()) sb.append("•");
            }
            return sb.toString().trim();
        }
    }

    /**
     * An immutable, ordered set of LR(0) items representing one DFA state.
     * Equality is set-based so duplicate states are detected correctly.
     */
    private static final class ItemSet {
        final int id;
        final LinkedHashSet<LR0Item> items;

        ItemSet(int id, LinkedHashSet<LR0Item> items) {
            this.id = id;
            this.items = items;
        }

        String name() { return "I" + id; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ItemSet other)) return false;
            return items.equals(other.items);
        }

        @Override
        public int hashCode() { return items.hashCode(); }
    }

    // ─────────────────────────────────────────────── injected dependencies ──

    private final GrammarService grammarService;
    private final FirstFollowService firstFollowService;

    public LRService(GrammarService grammarService, FirstFollowService firstFollowService) {
        this.grammarService = grammarService;
        this.firstFollowService = firstFollowService;
    }

    // ═══════════════════════════════════════════════ public API ═════════════

    /** Build the LR(0) table response (table + item sets + conflicts). */
    public LRTableResponse buildLR0Table(String rawGrammar, String startSymbol) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, startSymbol);
        Grammar augmented = grammar.getAugmentedGrammar();

        List<ItemSet> states = buildCanonicalCollection(augmented);
        Map<Integer, Map<String, List<String>>> rawAction = new LinkedHashMap<>();
        Map<Integer, Map<String, String>> rawGoto = new LinkedHashMap<>();
        List<LRConflictDto> conflicts = buildLR0Tables(augmented, states, rawAction, rawGoto);

        return buildResponse("LR(0)", grammar, augmented, states, rawAction, rawGoto, conflicts);
    }

    /** Build the SLR(1) table response (reuses LR(0) item sets). */
    public LRTableResponse buildSLR1Table(String rawGrammar, String startSymbol) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, startSymbol);
        Grammar augmented = grammar.getAugmentedGrammar();

        // Compute FOLLOW sets on the *original* grammar (not augmented)
        Map<GrammarSymbol, Set<GrammarSymbol>> firstSets = firstFollowService.computeFirstSets(grammar);
        Map<GrammarSymbol, Set<GrammarSymbol>> followSets = firstFollowService.computeFollowSets(grammar, firstSets);

        List<ItemSet> states = buildCanonicalCollection(augmented);
        Map<Integer, Map<String, List<String>>> rawAction = new LinkedHashMap<>();
        Map<Integer, Map<String, String>> rawGoto = new LinkedHashMap<>();
        List<LRConflictDto> conflicts = buildSLR1Tables(augmented, states, followSets, rawAction, rawGoto);

        return buildResponse("SLR(1)", grammar, augmented, states, rawAction, rawGoto, conflicts);
    }

    /** Simulate LR(0) table-driven parsing of {@code inputString}. */
    public LRParseResponse parseLR0(String rawGrammar, String startSymbol, String inputString) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, startSymbol);
        Grammar augmented = grammar.getAugmentedGrammar();
        List<ItemSet> states = buildCanonicalCollection(augmented);
        Map<Integer, Map<String, List<String>>> rawAction = new LinkedHashMap<>();
        Map<Integer, Map<String, String>> rawGoto = new LinkedHashMap<>();
        buildLR0Tables(augmented, states, rawAction, rawGoto);
        return simulateParse(grammar, augmented, states, rawAction, rawGoto, inputString);
    }

    /** Simulate SLR(1) table-driven parsing of {@code inputString}. */
    public LRParseResponse parseSLR1(String rawGrammar, String startSymbol, String inputString) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, startSymbol);
        Grammar augmented = grammar.getAugmentedGrammar();
        Map<GrammarSymbol, Set<GrammarSymbol>> firstSets = firstFollowService.computeFirstSets(grammar);
        Map<GrammarSymbol, Set<GrammarSymbol>> followSets = firstFollowService.computeFollowSets(grammar, firstSets);
        List<ItemSet> states = buildCanonicalCollection(augmented);
        Map<Integer, Map<String, List<String>>> rawAction = new LinkedHashMap<>();
        Map<Integer, Map<String, String>> rawGoto = new LinkedHashMap<>();
        buildSLR1Tables(augmented, states, followSets, rawAction, rawGoto);
        return simulateParse(grammar, augmented, states, rawAction, rawGoto, inputString);
    }

    // ═══════════════════════════════════════════ LR(0) item-set algorithms ═

    /**
     * CLOSURE(I): keep adding items [B -> •γ] for every [A -> α•Bβ] in the
     * current set, until no new items are added.
     */
    LinkedHashSet<LR0Item> closure(LinkedHashSet<LR0Item> items, Grammar augmented) {
        LinkedHashSet<LR0Item> result = new LinkedHashSet<>(items);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (LR0Item item : new ArrayList<>(result)) {
                GrammarSymbol sym = item.symbolAfterDot();
                if (sym != null && sym.isNonTerminal()) {
                    for (Production p : augmented.getProductionsFor(sym)) {
                        LR0Item newItem = new LR0Item(p, 0);
                        if (result.add(newItem)) changed = true;
                    }
                }
            }
        }
        return result;
    }

    /**
     * GOTO(I, X): advance the dot over X in every item where X is after the dot,
     * then compute the closure of the resulting kernel.
     */
    LinkedHashSet<LR0Item> gotoSet(LinkedHashSet<LR0Item> items, GrammarSymbol symbol,
                                    Grammar augmented) {
        LinkedHashSet<LR0Item> kernel = new LinkedHashSet<>();
        for (LR0Item item : items) {
            if (!item.isComplete() && symbol.equals(item.symbolAfterDot())) {
                kernel.add(item.advance());
            }
        }
        if (kernel.isEmpty()) return kernel;
        return closure(kernel, augmented);
    }

    /**
     * Builds the canonical collection of LR(0) item sets (states).
     * State 0 is the closure of { S' -> •S }.
     */
    List<ItemSet> buildCanonicalCollection(Grammar augmented) {
        Production augProd = augmented.getProductions().get(0); // S' -> S, id=0
        LinkedHashSet<LR0Item> initial = new LinkedHashSet<>();
        initial.add(new LR0Item(augProd, 0));
        LinkedHashSet<LR0Item> i0Items = closure(initial, augmented);

        List<ItemSet> states = new ArrayList<>();
        Map<LinkedHashSet<LR0Item>, Integer> seen = new LinkedHashMap<>();

        states.add(new ItemSet(0, i0Items));
        seen.put(i0Items, 0);

        // Collect every grammar symbol that can appear after a dot
        Set<GrammarSymbol> allSymbols = new LinkedHashSet<>();
        for (Production p : augmented.getProductions()) {
            if (!p.isEpsilon()) allSymbols.addAll(p.getRhs());
        }

        Queue<ItemSet> worklist = new ArrayDeque<>(states);
        while (!worklist.isEmpty()) {
            ItemSet current = worklist.poll();
            for (GrammarSymbol sym : allSymbols) {
                LinkedHashSet<LR0Item> next = gotoSet(current.items, sym, augmented);
                if (!next.isEmpty() && !seen.containsKey(next)) {
                    int id = states.size();
                    ItemSet newState = new ItemSet(id, next);
                    states.add(newState);
                    seen.put(next, id);
                    worklist.add(newState);
                }
            }
        }
        return states;
    }

    // ══════════════════════════════════════ table-construction helpers ══════

    /**
     * Builds the LR(0) ACTION and GOTO tables.
     * <ul>
     *   <li>Shift sN on terminal a when GOTO(state, a) = N.</li>
     *   <li>Reduce by production p on ALL terminals (including $) when an item is
     *       complete — except the augmented start item which becomes "acc".</li>
     *   <li>GOTO[state, A] = N for every non-terminal A with a transition.</li>
     * </ul>
     * Returns any detected conflicts.
     */
    private List<LRConflictDto> buildLR0Tables(
            Grammar augmented,
            List<ItemSet> states,
            Map<Integer, Map<String, List<String>>> rawAction,
            Map<Integer, Map<String, String>> rawGoto) {

        // Collect all terminal symbols (used as ACTION columns)
        List<String> terminals = collectTerminals(augmented);
        Production augStart = augmented.getProductions().get(0);

        for (ItemSet state : states) {
            Map<String, List<String>> actionRow = new LinkedHashMap<>();
            Map<String, String> gotoRow = new LinkedHashMap<>();
            rawAction.put(state.id, actionRow);
            rawGoto.put(state.id, gotoRow);

            // ── Shifts and GOTO ──
            for (GrammarSymbol sym : collectGotoSymbols(state, augmented)) {
                LinkedHashSet<LR0Item> target = gotoSet(state.items, sym, augmented);
                int targetId = findStateId(states, target);
                if (targetId < 0) continue;

                if (sym.isTerminal()) {
                    addAction(actionRow, sym.getName(), "s" + targetId);
                } else if (sym.isNonTerminal()) {
                    gotoRow.put(sym.getName(), String.valueOf(targetId));
                }
            }

            // ── Reductions and Accept ──
            for (LR0Item item : state.items) {
                if (!item.isComplete()) continue;

                if (item.production.equals(augStart) && item.production.isAugmented()) {
                    // S' -> S •  =>  accept on $
                    addAction(actionRow, GrammarSymbol.EOF_STR, "acc");
                } else {
                    // Reduce by this production on ALL terminals + $  (LR(0) — no lookahead)
                    String reduceEntry = reduceLabel(item.production);
                    for (String t : terminals) {
                        addAction(actionRow, t, reduceEntry);
                    }
                    addAction(actionRow, GrammarSymbol.EOF_STR, reduceEntry);
                }
            }
        }

        return extractConflicts(states, rawAction, "LR(0)");
    }

    /**
     * Builds the SLR(1) ACTION and GOTO tables.
     * Reductions are placed only on terminals in FOLLOW(LHS) instead of all terminals.
     */
    private List<LRConflictDto> buildSLR1Tables(
            Grammar augmented,
            List<ItemSet> states,
            Map<GrammarSymbol, Set<GrammarSymbol>> followSets,
            Map<Integer, Map<String, List<String>>> rawAction,
            Map<Integer, Map<String, String>> rawGoto) {

        Production augStart = augmented.getProductions().get(0);

        for (ItemSet state : states) {
            Map<String, List<String>> actionRow = new LinkedHashMap<>();
            Map<String, String> gotoRow = new LinkedHashMap<>();
            rawAction.put(state.id, actionRow);
            rawGoto.put(state.id, gotoRow);

            // ── Shifts and GOTO ──
            for (GrammarSymbol sym : collectGotoSymbols(state, augmented)) {
                LinkedHashSet<LR0Item> target = gotoSet(state.items, sym, augmented);
                int targetId = findStateId(states, target);
                if (targetId < 0) continue;

                if (sym.isTerminal()) {
                    addAction(actionRow, sym.getName(), "s" + targetId);
                } else if (sym.isNonTerminal()) {
                    gotoRow.put(sym.getName(), String.valueOf(targetId));
                }
            }

            // ── Reductions and Accept ──
            for (LR0Item item : state.items) {
                if (!item.isComplete()) continue;

                if (item.production.equals(augStart) && item.production.isAugmented()) {
                    addAction(actionRow, GrammarSymbol.EOF_STR, "acc");
                } else {
                    // Reduce only on FOLLOW(LHS)
                    GrammarSymbol lhs = item.production.getLhs();
                    Set<GrammarSymbol> follow = followSets.getOrDefault(lhs, Collections.emptySet());
                    String reduceEntry = reduceLabel(item.production);
                    for (GrammarSymbol f : follow) {
                        addAction(actionRow, f.getName(), reduceEntry);
                    }
                }
            }
        }

        return extractConflicts(states, rawAction, "SLR(1)");
    }

    // ════════════════════════════════════════════════ table-driven parsing ═

    /**
     * Standard LR driver: state stack + symbol stack.
     * Records a {@link LRParseStep} for every action taken.
     */
    private LRParseResponse simulateParse(
            Grammar original,
            Grammar augmented,
            List<ItemSet> states,
            Map<Integer, Map<String, List<String>>> rawAction,
            Map<Integer, Map<String, String>> rawGoto,
            String inputString) {

        // Tokenize
        List<String> tokens = tokenize(inputString);
        Set<String> validTerminals = original.getTerminals().stream()
                .map(GrammarSymbol::getName).collect(Collectors.toSet());

        for (String tok : tokens) {
            if (!tok.equals(GrammarSymbol.EOF_STR) && !validTerminals.contains(tok)) {
                return LRParseResponse.failure(
                        "Unrecognized terminal: '" + tok + "'. Valid terminals: " + validTerminals,
                        Collections.emptyList());
            }
        }
        if (tokens.isEmpty() || !tokens.getLast().equals(GrammarSymbol.EOF_STR)) {
            tokens.add(GrammarSymbol.EOF_STR);
        }

        // Stacks
        Deque<Integer> stateStack = new ArrayDeque<>();
        Deque<String> symStack = new ArrayDeque<>();
        stateStack.push(0);
        symStack.push(GrammarSymbol.EOF_STR);

        List<LRParseStep> steps = new ArrayList<>();
        int ptr = 0;
        int stepNum = 1;
        final int MAX_STEPS = 2000;

        while (stepNum <= MAX_STEPS) {
            int currentState = stateStack.peek();
            String lookahead = tokens.get(ptr);

            String stateStackStr  = formatStateStack(stateStack);
            String symStackStr    = formatSymStack(symStack);
            String inputRemaining = String.join(" ", tokens.subList(ptr, tokens.size()));

            Map<String, List<String>> actionRow = rawAction.getOrDefault(currentState, Collections.emptyMap());
            List<String> cellActions = actionRow.getOrDefault(lookahead, Collections.emptyList());

            if (cellActions.isEmpty()) {
                String err = "Syntax Error: No action in state " + currentState
                        + " for input '" + lookahead + "'.";
                steps.add(new LRParseStep(stepNum, stateStackStr, symStackStr, inputRemaining,
                        "Error: " + err, ""));
                return LRParseResponse.failure(err, steps);
            }

            // Prefer "acc" > shift > reduce when there is a conflict (use first entry)
            String action = pickAction(cellActions);

            if (action.equals("acc")) {
                steps.add(new LRParseStep(stepNum, stateStackStr, symStackStr, inputRemaining,
                        "Accept", ""));
                LRParseResponse res = new LRParseResponse();
                res.setAccepted(true);
                res.setMessage("String accepted.");
                res.setSteps(steps);
                return res;

            } else if (action.startsWith("s")) {
                int nextState = Integer.parseInt(action.substring(1));
                steps.add(new LRParseStep(stepNum++, stateStackStr, symStackStr, inputRemaining,
                        "Shift " + action, ""));
                stateStack.push(nextState);
                symStack.push(lookahead);
                ptr++;

            } else if (action.startsWith("r")) {
                int prodId = Integer.parseInt(action.substring(1));
                Production prod = augmented.getProductionById(prodId)
                        .orElseThrow(() -> new IllegalStateException("Unknown production id: " + prodId));

                String ruleStr = prod.getFormattedRule();
                steps.add(new LRParseStep(stepNum++, stateStackStr, symStackStr, inputRemaining,
                        "Reduce " + action + " (" + ruleStr + ")", ruleStr));

                // Pop |β| symbols (epsilon production pops 0)
                int popCount = prod.isEpsilon() ? 0 : prod.getRhs().size();
                for (int i = 0; i < popCount; i++) {
                    stateStack.pop();
                    symStack.pop();
                }

                // GOTO[exposed state, LHS]
                int exposedState = stateStack.peek();
                String lhsName = prod.getLhs().getName();
                Map<String, String> gotoRow = rawGoto.getOrDefault(exposedState, Collections.emptyMap());
                String gotoTarget = gotoRow.get(lhsName);

                if (gotoTarget == null) {
                    String err = "Syntax Error: No GOTO entry in state " + exposedState
                            + " for non-terminal '" + lhsName + "'.";
                    steps.add(new LRParseStep(stepNum, stateStackStr, symStackStr, inputRemaining,
                            "Error: " + err, ""));
                    return LRParseResponse.failure(err, steps);
                }
                stateStack.push(Integer.parseInt(gotoTarget));
                symStack.push(lhsName);

            } else {
                return LRParseResponse.failure("Unknown action: " + action, steps);
            }
        }

        return LRParseResponse.failure(
                "Parse exceeded " + MAX_STEPS + " steps — possible grammar cycle.", steps);
    }

    // ══════════════════════════════════════════════════ response building ═══

    private LRTableResponse buildResponse(
            String parserType,
            Grammar original,
            Grammar augmented,
            List<ItemSet> states,
            Map<Integer, Map<String, List<String>>> rawAction,
            Map<Integer, Map<String, String>> rawGoto,
            List<LRConflictDto> conflicts) {

        LRTableResponse resp = new LRTableResponse();
        resp.setParserType(parserType);
        resp.setStartSymbol(original.getStartSymbol().getName());
        resp.setAugmentedStartSymbol(augmented.getStartSymbol().getName());
        resp.setNormalizedGrammar(original.getNormalizedRepresentation());
        resp.setDeterministic(conflicts.isEmpty());
        resp.setConflicts(conflicts);

        resp.setAugmentedProductions(
                augmented.getProductions().stream().map(ProductionDto::from).toList());

        // ── Item sets ──
        List<LRItemSetDto> itemSetDtos = new ArrayList<>();
        for (ItemSet state : states) {
            LRItemSetDto dto = new LRItemSetDto(state.id, state.name());
            for (LR0Item item : state.items) {
                boolean isCore = item.production.isAugmented() || item.dot > 0
                        || (item.production.isEpsilon());
                dto.getItems().add(new LRItemDto(item.format(), isCore));
            }
            // GOTO transitions for the automaton graph
            for (GrammarSymbol sym : collectGotoSymbols(state, augmented)) {
                LinkedHashSet<LR0Item> tgt = gotoSet(state.items, sym, augmented);
                int tgtId = findStateId(states, tgt);
                if (tgtId >= 0) {
                    dto.getGotos().add(new LRGotoDto(sym.getName(), "I" + tgtId));
                }
            }
            itemSetDtos.add(dto);
        }
        resp.setItemSets(itemSetDtos);

        // ── Headers ──
        List<String> actionHeaders = collectTerminals(augmented);
        actionHeaders.add(GrammarSymbol.EOF_STR);
        resp.setActionHeaders(actionHeaders);

        List<String> gotoHeaders = augmented.getNonTerminals().stream()
                .filter(nt -> !nt.equals(augmented.getStartSymbol()))
                .map(GrammarSymbol::getName)
                .toList();
        resp.setGotoHeaders(gotoHeaders);

        // ── Table rows ──
        List<Map<String, Object>> tableRows = new ArrayList<>();
        for (ItemSet state : states) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("state", String.valueOf(state.id));

            Map<String, String> actions = new LinkedHashMap<>();
            Map<String, List<String>> actionRow = rawAction.getOrDefault(state.id, Collections.emptyMap());
            for (String col : actionHeaders) {
                List<String> cellList = actionRow.getOrDefault(col, Collections.emptyList());
                if (!cellList.isEmpty()) {
                    actions.put(col, String.join(" / ", cellList));
                }
            }
            row.put("actions", actions);

            Map<String, String> gotos = new LinkedHashMap<>();
            Map<String, String> gotoRow = rawGoto.getOrDefault(state.id, Collections.emptyMap());
            for (String nt : gotoHeaders) {
                String target = gotoRow.get(nt);
                if (target != null) gotos.put(nt, target);
            }
            row.put("gotos", gotos);

            tableRows.add(row);
        }
        resp.setTableRows(tableRows);

        return resp;
    }

    // ═══════════════════════════════════════════════════════ utility helpers ═

    /** All terminal names in the augmented grammar, deterministically ordered. */
    private List<String> collectTerminals(Grammar augmented) {
        return augmented.getTerminals().stream()
                .map(GrammarSymbol::getName)
                .sorted(FirstFollowService.DETERMINISTIC_SYMBOL_COMPARATOR)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Symbols that appear immediately after a dot in any item of the state. */
    private Set<GrammarSymbol> collectGotoSymbols(ItemSet state, Grammar augmented) {
        Set<GrammarSymbol> syms = new LinkedHashSet<>();
        for (LR0Item item : state.items) {
            GrammarSymbol sym = item.symbolAfterDot();
            if (sym != null && !sym.isEpsilon()) syms.add(sym);
        }
        return syms;
    }

    private int findStateId(List<ItemSet> states, LinkedHashSet<LR0Item> items) {
        for (ItemSet s : states) {
            if (s.items.equals(items)) return s.id;
        }
        return -1;
    }

    /** Adds an entry to the action cell list; duplicates are not repeated. */
    private void addAction(Map<String, List<String>> actionRow, String terminal, String entry) {
        actionRow.computeIfAbsent(terminal, k -> new ArrayList<>());
        if (!actionRow.get(terminal).contains(entry)) {
            actionRow.get(terminal).add(entry);
        }
    }

    /** Reduce label: "rN" where N is the production id. */
    private String reduceLabel(Production p) {
        return "r" + p.getId();
    }

    /**
     * Extract conflicts from the raw action map.
     * A conflict exists whenever a cell has more than one distinct action.
     */
    private List<LRConflictDto> extractConflicts(
            List<ItemSet> states,
            Map<Integer, Map<String, List<String>>> rawAction,
            String parserLabel) {

        List<LRConflictDto> conflicts = new ArrayList<>();
        for (ItemSet state : states) {
            Map<String, List<String>> row = rawAction.getOrDefault(state.id, Collections.emptyMap());
            for (Map.Entry<String, List<String>> entry : row.entrySet()) {
                List<String> acts = entry.getValue();
                if (acts.size() <= 1) continue;

                String terminal = entry.getKey();
                boolean hasShift  = acts.stream().anyMatch(a -> a.startsWith("s"));
                boolean hasReduce = acts.stream().anyMatch(a -> a.startsWith("r"));
                boolean hasAcc    = acts.stream().anyMatch(a -> a.equals("acc"));

                String type;
                if (hasShift && hasReduce) {
                    type = "Shift / Reduce Conflict";
                } else if (hasAcc && hasReduce) {
                    type = "Shift / Reduce Conflict"; // acc vs reduce
                } else {
                    type = "Reduce / Reduce Conflict";
                }

                String message = parserLabel + " conflict in state " + state.name()
                        + " on '" + terminal + "': " + String.join(", ", acts);

                conflicts.add(new LRConflictDto(type, state.name(), terminal, message, acts));
            }
        }
        return conflicts;
    }

    /**
     * When a cell has multiple actions (conflict), pick the "best" one for parsing:
     * prefer acc, then shift, then first reduce.
     */
    private String pickAction(List<String> actions) {
        for (String a : actions) if (a.equals("acc")) return a;
        for (String a : actions) if (a.startsWith("s")) return a;
        return actions.get(0);
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        if (input == null || input.isBlank()) return tokens;
        for (String t : input.trim().split("\\s+")) {
            if (!t.isEmpty()) tokens.add(t);
        }
        return tokens;
    }

    private String formatStateStack(Deque<Integer> stack) {
        List<Integer> list = new ArrayList<>(stack);
        Collections.reverse(list);
        return list.stream().map(String::valueOf).collect(Collectors.joining(" "));
    }

    private String formatSymStack(Deque<String> stack) {
        List<String> list = new ArrayList<>(stack);
        Collections.reverse(list);
        return String.join(" ", list);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CLR(1) — Canonical LR(1) parser
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * An LR(1) item: production + dot position + a single lookahead terminal.
     * Equality and hashing include the lookahead so that items with different
     * lookaheads are kept separate (unlike LR(0) items).
     */
    record LR1Item(Production production, int dot, GrammarSymbol lookahead) {

        boolean isComplete() {
            if (production.isEpsilon()) return true;
            return dot >= production.getRhs().size();
        }

        GrammarSymbol symbolAfterDot() {
            if (isComplete()) return null;
            return production.getRhs().get(dot);
        }

        LR1Item advance() {
            return new LR1Item(production, dot + 1, lookahead);
        }

        /** LR(0) core identity — ignores lookahead. */
        String coreKey() {
            return production.getId() + "@" + dot;
        }

        String format() {
            List<GrammarSymbol> rhs = production.getRhs();
            StringBuilder sb = new StringBuilder();
            sb.append(production.getLhs().getName()).append(" -> ");
            if (production.isEpsilon()) {
                sb.append("• ε");
            } else {
                for (int i = 0; i < rhs.size(); i++) {
                    if (i == dot) sb.append("• ");
                    sb.append(rhs.get(i).getName()).append(" ");
                }
                if (dot == rhs.size()) sb.append("•");
            }
            return sb.toString().trim();
        }
    }

    /** One state in the canonical LR(1) collection. */
    private static final class LR1ItemSet {
        final int id;
        final LinkedHashSet<LR1Item> items;

        LR1ItemSet(int id, LinkedHashSet<LR1Item> items) {
            this.id = id;
            this.items = items;
        }

        String name() { return "I" + id; }

        @Override public boolean equals(Object o) {
            if (!(o instanceof LR1ItemSet other)) return false;
            return items.equals(other.items);
        }
        @Override public int hashCode() { return items.hashCode(); }
    }

    // ──────────────────────────────────────────── CLR(1) public entry points

    /** Build the CLR(1) table response. */
    public LRTableResponse buildCLR1Table(String rawGrammar, String startSymbol) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, startSymbol);
        Grammar augmented = grammar.getAugmentedGrammar();

        List<LR1ItemSet> states = buildCanonicalLR1Collection(augmented);
        Map<Integer, Map<String, List<String>>> rawAction = new LinkedHashMap<>();
        Map<Integer, Map<String, String>> rawGoto = new LinkedHashMap<>();
        List<LRConflictDto> conflicts = buildCLR1Tables(augmented, states, rawAction, rawGoto);

        return buildLR1Response("CLR(1)", grammar, augmented, states, rawAction, rawGoto, conflicts, null);
    }

    /** Simulate CLR(1) table-driven parsing. */
    public LRParseResponse parseCLR1(String rawGrammar, String startSymbol, String inputString) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, startSymbol);
        Grammar augmented = grammar.getAugmentedGrammar();
        List<LR1ItemSet> states = buildCanonicalLR1Collection(augmented);
        Map<Integer, Map<String, List<String>>> rawAction = new LinkedHashMap<>();
        Map<Integer, Map<String, String>> rawGoto = new LinkedHashMap<>();
        buildCLR1Tables(augmented, states, rawAction, rawGoto);
        return simulateLRParse(grammar, augmented, rawAction, rawGoto, inputString);
    }

    // ──────────────────────────────────────── LR(1) CLOSURE and GOTO

    /**
     * LR(1) CLOSURE.
     *
     * <p>For every item [A -> α•Bβ, a] in the set, and every production B -> γ,
     * add [B -> •γ, b] for each b in FIRST(βa).
     */
    LinkedHashSet<LR1Item> lr1Closure(LinkedHashSet<LR1Item> items, Grammar augmented,
                                       Map<GrammarSymbol, Set<GrammarSymbol>> firstSets) {
        LinkedHashSet<LR1Item> result = new LinkedHashSet<>(items);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (LR1Item item : new ArrayList<>(result)) {
                GrammarSymbol B = item.symbolAfterDot();
                if (B == null || !B.isNonTerminal()) continue;

                // Compute FIRST(β a) where β is the suffix after B and a is item.lookahead
                List<GrammarSymbol> betaPlusA = new ArrayList<>();
                List<GrammarSymbol> rhs = item.production.getRhs();
                if (!item.production.isEpsilon()) {
                    for (int i = item.dot + 1; i < rhs.size(); i++) betaPlusA.add(rhs.get(i));
                }
                betaPlusA.add(item.lookahead);

                Set<GrammarSymbol> lookaheads = firstFollowService.computeFirstOfSequence(betaPlusA, firstSets);

                for (Production p : augmented.getProductionsFor(B)) {
                    for (GrammarSymbol la : lookaheads) {
                        if (la.isEpsilon()) continue;
                        LR1Item newItem = new LR1Item(p, 0, la);
                        if (result.add(newItem)) changed = true;
                    }
                }
            }
        }
        return result;
    }

    /**
     * LR(1) GOTO(I, X): advance the dot over X, then close.
     */
    LinkedHashSet<LR1Item> lr1Goto(LinkedHashSet<LR1Item> items, GrammarSymbol symbol,
                                    Grammar augmented,
                                    Map<GrammarSymbol, Set<GrammarSymbol>> firstSets) {
        LinkedHashSet<LR1Item> kernel = new LinkedHashSet<>();
        for (LR1Item item : items) {
            if (!item.isComplete() && symbol.equals(item.symbolAfterDot())) {
                kernel.add(item.advance());
            }
        }
        if (kernel.isEmpty()) return kernel;
        return lr1Closure(kernel, augmented, firstSets);
    }

    /**
     * Builds the canonical collection of LR(1) item sets.
     * Initial state: CLOSURE({[S' -> •S, $]}).
     */
    List<LR1ItemSet> buildCanonicalLR1Collection(Grammar augmented) {
        Map<GrammarSymbol, Set<GrammarSymbol>> firstSets = firstFollowService.computeFirstSets(augmented);

        Production augProd = augmented.getProductions().get(0); // S' -> S, id=0
        LinkedHashSet<LR1Item> initial = new LinkedHashSet<>();
        initial.add(new LR1Item(augProd, 0, GrammarSymbol.EOF));
        LinkedHashSet<LR1Item> i0 = lr1Closure(initial, augmented, firstSets);

        List<LR1ItemSet> states = new ArrayList<>();
        Map<LinkedHashSet<LR1Item>, Integer> seen = new LinkedHashMap<>();
        states.add(new LR1ItemSet(0, i0));
        seen.put(i0, 0);

        Set<GrammarSymbol> allSymbols = new LinkedHashSet<>();
        for (Production p : augmented.getProductions()) {
            if (!p.isEpsilon()) allSymbols.addAll(p.getRhs());
        }

        Queue<LR1ItemSet> worklist = new ArrayDeque<>(states);
        while (!worklist.isEmpty()) {
            LR1ItemSet current = worklist.poll();
            for (GrammarSymbol sym : allSymbols) {
                LinkedHashSet<LR1Item> next = lr1Goto(current.items, sym, augmented, firstSets);
                if (!next.isEmpty() && !seen.containsKey(next)) {
                    int id = states.size();
                    LR1ItemSet ns = new LR1ItemSet(id, next);
                    states.add(ns);
                    seen.put(next, id);
                    worklist.add(ns);
                }
            }
        }
        return states;
    }

    // ──────────────────────────────────────────── CLR(1) table construction

    private List<LRConflictDto> buildCLR1Tables(
            Grammar augmented,
            List<LR1ItemSet> states,
            Map<Integer, Map<String, List<String>>> rawAction,
            Map<Integer, Map<String, String>> rawGoto) {

        Map<GrammarSymbol, Set<GrammarSymbol>> firstSets = firstFollowService.computeFirstSets(augmented);
        Production augStart = augmented.getProductions().get(0);

        for (LR1ItemSet state : states) {
            Map<String, List<String>> actionRow = new LinkedHashMap<>();
            Map<String, String> gotoRow = new LinkedHashMap<>();
            rawAction.put(state.id, actionRow);
            rawGoto.put(state.id, gotoRow);

            // ── Shifts and GOTO ──
            Set<GrammarSymbol> gotoSyms = collectLR1GotoSymbols(state);
            for (GrammarSymbol sym : gotoSyms) {
                LinkedHashSet<LR1Item> target = lr1Goto(state.items, sym, augmented, firstSets);
                int targetId = findLR1StateId(states, target);
                if (targetId < 0) continue;
                if (sym.isTerminal()) {
                    addAction(actionRow, sym.getName(), "s" + targetId);
                } else if (sym.isNonTerminal()) {
                    gotoRow.put(sym.getName(), String.valueOf(targetId));
                }
            }

            // ── Reductions and Accept ──
            for (LR1Item item : state.items) {
                if (!item.isComplete()) continue;
                if (item.production.equals(augStart) && item.production.isAugmented()) {
                    // Accept only on $
                    if (item.lookahead.equals(GrammarSymbol.EOF)) {
                        addAction(actionRow, GrammarSymbol.EOF_STR, "acc");
                    }
                } else {
                    // Reduce on the item's specific lookahead
                    addAction(actionRow, item.lookahead.getName(), reduceLabel(item.production));
                }
            }
        }

        return extractLR1Conflicts(states, rawAction, "CLR(1)");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LALR(1) — Lookahead LR parser
    // ═══════════════════════════════════════════════════════════════════════

    /** Build the LALR(1) table response. */
    public LRTableResponse buildLALR1Table(String rawGrammar, String startSymbol) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, startSymbol);
        Grammar augmented = grammar.getAugmentedGrammar();

        List<LR1ItemSet> clrStates = buildCanonicalLR1Collection(augmented);
        return buildLALR1Response(grammar, augmented, clrStates);
    }

    /** Simulate LALR(1) table-driven parsing. */
    public LRParseResponse parseLALR1(String rawGrammar, String startSymbol, String inputString) {
        Grammar grammar = grammarService.getParsedGrammar(rawGrammar, startSymbol);
        Grammar augmented = grammar.getAugmentedGrammar();
        List<LR1ItemSet> clrStates = buildCanonicalLR1Collection(augmented);

        // Merge CLR(1) states by core, build tables, then simulate
        MergedLALR merged = mergeByCore(augmented, clrStates);
        return simulateLRParse(grammar, augmented, merged.rawAction, merged.rawGoto, inputString);
    }

    // ──────────────────────────────────────────── LALR merging

    /** Carries the merged ACTION/GOTO tables and conflict list. */
    private record MergedLALR(
            List<MergedState> mergedStates,
            Map<Integer, Map<String, List<String>>> rawAction,
            Map<Integer, Map<String, String>> rawGoto,
            List<LRConflictDto> conflicts) {}

    /** One merged LALR(1) state: has a stable id (lowest CLR id in the group). */
    private record MergedState(
            int id,
            List<Integer> clrIds,     // originating CLR(1) state ids
            LinkedHashSet<LR1Item> items) {
        String name() { return "I" + id; }
    }

    /**
     * Groups CLR(1) states with identical LR(0) cores into merged LALR(1) states.
     *
     * <p>Two states share a core when they contain the same set of (production, dot)
     * pairs — ignoring lookaheads.  For each group, the merged state carries the
     * union of all lookaheads for every core item.
     *
     * <p>The merged state id is the id of the first (lowest-id) CLR(1) state in each group.
     */
    private MergedLALR mergeByCore(Grammar augmented, List<LR1ItemSet> clrStates) {
        Map<GrammarSymbol, Set<GrammarSymbol>> firstSets = firstFollowService.computeFirstSets(augmented);

        // ── 1. Group CLR states by their core signature ──
        // Core signature = sorted list of "prodId@dot" strings
        Map<String, List<LR1ItemSet>> coreGroups = new LinkedHashMap<>();
        for (LR1ItemSet s : clrStates) {
            String sig = s.items.stream()
                    .map(LR1Item::coreKey)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining("|"));
            coreGroups.computeIfAbsent(sig, k -> new ArrayList<>()).add(s);
        }

        // ── 2. Build merged states; id = min CLR id in the group ──
        // Also build: clrIdToMergedId mapping
        Map<Integer, Integer> clrIdToMergedId = new LinkedHashMap<>();
        List<MergedState> mergedStates = new ArrayList<>();

        for (List<LR1ItemSet> group : coreGroups.values()) {
            int mergedId = group.stream().mapToInt(s -> s.id).min().orElseThrow();

            // Merge lookaheads: for each (prod, dot) core, union all lookaheads from the group
            // Key: coreKey  ->  Map<coreKey, (LR1Item representative + Set<lookahead>)>
            Map<String, LR1Item> coreToRep = new LinkedHashMap<>();
            Map<String, Set<GrammarSymbol>> coreToLookaheads = new LinkedHashMap<>();

            for (LR1ItemSet s : group) {
                for (LR1Item item : s.items) {
                    String ck = item.coreKey();
                    coreToRep.putIfAbsent(ck, item);
                    coreToLookaheads.computeIfAbsent(ck, k -> new LinkedHashSet<>())
                            .add(item.lookahead);
                }
            }

            // Create one LR1Item per core, with a representative lookahead (we create
            // one item per (core, lookahead) pair so the set is fully expanded)
            LinkedHashSet<LR1Item> mergedItems = new LinkedHashSet<>();
            for (Map.Entry<String, LR1Item> e : coreToRep.entrySet()) {
                LR1Item rep = e.getValue();
                for (GrammarSymbol la : coreToLookaheads.get(e.getKey())) {
                    mergedItems.add(new LR1Item(rep.production(), rep.dot(), la));
                }
            }

            MergedState ms = new MergedState(mergedId, group.stream().map(s -> s.id).toList(), mergedItems);
            mergedStates.add(ms);

            for (LR1ItemSet s : group) {
                clrIdToMergedId.put(s.id, mergedId);
            }
        }

        // Sort by id for stable output
        mergedStates.sort(Comparator.comparingInt(s -> s.id));

        // ── 3. Build LALR tables using the merged states ──
        Map<Integer, Map<String, List<String>>> rawAction = new LinkedHashMap<>();
        Map<Integer, Map<String, String>> rawGoto = new LinkedHashMap<>();
        Production augStart = augmented.getProductions().get(0);

        for (MergedState ms : mergedStates) {
            Map<String, List<String>> actionRow = new LinkedHashMap<>();
            Map<String, String> gotoRow = new LinkedHashMap<>();
            rawAction.put(ms.id, actionRow);
            rawGoto.put(ms.id, gotoRow);

            // Shifts / GOTO — compute transitions from any one of the original CLR states
            // (transitions from states with the same core go to states with the same core)
            // We use lr1Goto on the merged items directly, then map target to merged id.
            Set<GrammarSymbol> gotoSyms = collectLR1GotoSymbolsFromItems(ms.items);
            for (GrammarSymbol sym : gotoSyms) {
                // Find any CLR state with same core to use its transition
                // (since GOTO is core-deterministic, we can compute GOTO on merged items)
                LinkedHashSet<LR1Item> target = lr1Goto(ms.items, sym, augmented, firstSets);
                if (target.isEmpty()) continue;

                // Map target to its merged id
                String targetSig = target.stream()
                        .map(LR1Item::coreKey)
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining("|"));
                // Find merged state with this core signature
                Integer targetMergedId = findMergedIdByCore(mergedStates, targetSig);
                if (targetMergedId == null) continue;

                if (sym.isTerminal()) {
                    addAction(actionRow, sym.getName(), "s" + targetMergedId);
                } else if (sym.isNonTerminal()) {
                    gotoRow.put(sym.getName(), String.valueOf(targetMergedId));
                }
            }

            // Reductions / Accept — use merged lookaheads
            for (LR1Item item : ms.items) {
                if (!item.isComplete()) continue;
                if (item.production.equals(augStart) && item.production.isAugmented()) {
                    if (item.lookahead.equals(GrammarSymbol.EOF)) {
                        addAction(actionRow, GrammarSymbol.EOF_STR, "acc");
                    }
                } else {
                    addAction(actionRow, item.lookahead.getName(), reduceLabel(item.production));
                }
            }
        }

        List<LRConflictDto> conflicts = extractLALR1Conflicts(mergedStates, rawAction);
        return new MergedLALR(mergedStates, rawAction, rawGoto, conflicts);
    }

    /** Finds the merged id for a given core signature string. */
    private Integer findMergedIdByCore(List<MergedState> mergedStates, String coreSig) {
        for (MergedState ms : mergedStates) {
            String sig = ms.items.stream()
                    .map(LR1Item::coreKey)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining("|"));
            if (sig.equals(coreSig)) return ms.id;
        }
        return null;
    }

    // ──────────────────────────────────────────── LALR response building

    private LRTableResponse buildLALR1Response(
            Grammar original,
            Grammar augmented,
            List<LR1ItemSet> clrStates) {

        MergedLALR merged = mergeByCore(augmented, clrStates);

        LRTableResponse resp = new LRTableResponse();
        resp.setParserType("LALR(1)");
        resp.setStartSymbol(original.getStartSymbol().getName());
        resp.setAugmentedStartSymbol(augmented.getStartSymbol().getName());
        resp.setNormalizedGrammar(original.getNormalizedRepresentation());
        resp.setDeterministic(merged.conflicts().isEmpty());
        resp.setConflicts(merged.conflicts());
        resp.setAugmentedProductions(
                augmented.getProductions().stream().map(ProductionDto::from).toList());

        // ── Item sets (merged states) ──
        List<LRItemSetDto> itemSetDtos = new ArrayList<>();
        for (MergedState ms : merged.mergedStates()) {
            LRItemSetDto dto = new LRItemSetDto(ms.id, ms.name());

            // Deduplicate items by (prod, dot); collect all lookaheads per core item
            Map<String, Set<GrammarSymbol>> coreToLookaheads = new LinkedHashMap<>();
            Map<String, LR1Item> coreToRep = new LinkedHashMap<>();
            for (LR1Item item : ms.items) {
                String ck = item.coreKey();
                coreToRep.putIfAbsent(ck, item);
                coreToLookaheads.computeIfAbsent(ck, k -> new LinkedHashSet<>()).add(item.lookahead);
            }
            for (Map.Entry<String, LR1Item> e : coreToRep.entrySet()) {
                LR1Item rep = e.getValue();
                boolean isCore = rep.production().isAugmented() || rep.dot() > 0 || rep.production().isEpsilon();
                Set<GrammarSymbol> las = coreToLookaheads.get(e.getKey());
                String laStr = las.stream()
                        .map(GrammarSymbol::getName)
                        .sorted(FirstFollowService.DETERMINISTIC_SYMBOL_COMPARATOR)
                        .collect(Collectors.joining(", "));
                dto.getItems().add(new LRItemDto(rep.format(), isCore, laStr));
            }

            // Transitions (from merged rawGoto + action shifts)
            Map<String, String> gotoRow = merged.rawGoto().getOrDefault(ms.id, Collections.emptyMap());
            Map<String, List<String>> actionRow = merged.rawAction().getOrDefault(ms.id, Collections.emptyMap());
            Set<String> addedTransitions = new LinkedHashSet<>();
            for (Map.Entry<String, String> e : gotoRow.entrySet()) {
                String key = e.getKey() + "->" + e.getValue();
                if (addedTransitions.add(key)) {
                    dto.getGotos().add(new LRGotoDto(e.getKey(), "I" + e.getValue()));
                }
            }
            for (Map.Entry<String, List<String>> e : actionRow.entrySet()) {
                for (String act : e.getValue()) {
                    if (act.startsWith("s")) {
                        String key = e.getKey() + "->" + act.substring(1);
                        if (addedTransitions.add(key)) {
                            dto.getGotos().add(new LRGotoDto(e.getKey(), "I" + act.substring(1)));
                        }
                    }
                }
            }
            itemSetDtos.add(dto);
        }
        resp.setItemSets(itemSetDtos);

        // ── mergedPairs for the frontend LALR merged-states info box ──
        List<Map<String, Object>> mergedPairs = new ArrayList<>();
        for (MergedState ms : merged.mergedStates()) {
            if (ms.clrIds().size() <= 1) continue;  // only show actually-merged states
            Map<String, Object> pair = new LinkedHashMap<>();
            pair.put("mergedName", ms.name());
            pair.put("from", ms.clrIds().stream().map(id -> "I" + id).toList());
            // Core: first item's format (representative)
            String core = ms.items.isEmpty() ? "" : ms.items.iterator().next().format();
            pair.put("core", core);
            mergedPairs.add(pair);
        }
        resp.setMergedPairs(mergedPairs);

        // ── Headers ──
        List<String> actionHeaders = collectTerminals(augmented);
        actionHeaders.add(GrammarSymbol.EOF_STR);
        resp.setActionHeaders(actionHeaders);
        List<String> gotoHeaders = augmented.getNonTerminals().stream()
                .filter(nt -> !nt.equals(augmented.getStartSymbol()))
                .map(GrammarSymbol::getName).toList();
        resp.setGotoHeaders(gotoHeaders);

        // ── Table rows ──
        List<Map<String, Object>> tableRows = new ArrayList<>();
        for (MergedState ms : merged.mergedStates()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("state", String.valueOf(ms.id));
            Map<String, String> actions = new LinkedHashMap<>();
            Map<String, List<String>> actionRow = merged.rawAction().getOrDefault(ms.id, Collections.emptyMap());
            for (String col : actionHeaders) {
                List<String> cell = actionRow.getOrDefault(col, Collections.emptyList());
                if (!cell.isEmpty()) actions.put(col, String.join(" / ", cell));
            }
            row.put("actions", actions);
            Map<String, String> gotos = new LinkedHashMap<>();
            Map<String, String> gotoRow = merged.rawGoto().getOrDefault(ms.id, Collections.emptyMap());
            for (String nt : gotoHeaders) {
                String t = gotoRow.get(nt);
                if (t != null) gotos.put(nt, t);
            }
            row.put("gotos", gotos);
            tableRows.add(row);
        }
        resp.setTableRows(tableRows);

        return resp;
    }

    // ──────────────────────────────────────────── CLR(1) response building

    private LRTableResponse buildLR1Response(
            String parserType,
            Grammar original,
            Grammar augmented,
            List<LR1ItemSet> states,
            Map<Integer, Map<String, List<String>>> rawAction,
            Map<Integer, Map<String, String>> rawGoto,
            List<LRConflictDto> conflicts,
            Object unused) {

        LRTableResponse resp = new LRTableResponse();
        resp.setParserType(parserType);
        resp.setStartSymbol(original.getStartSymbol().getName());
        resp.setAugmentedStartSymbol(augmented.getStartSymbol().getName());
        resp.setNormalizedGrammar(original.getNormalizedRepresentation());
        resp.setDeterministic(conflicts.isEmpty());
        resp.setConflicts(conflicts);
        resp.setAugmentedProductions(
                augmented.getProductions().stream().map(ProductionDto::from).toList());

        Map<GrammarSymbol, Set<GrammarSymbol>> firstSets = firstFollowService.computeFirstSets(augmented);

        // ── Item sets: deduplicate by (prod, dot), collect all lookaheads ──
        List<LRItemSetDto> itemSetDtos = new ArrayList<>();
        for (LR1ItemSet state : states) {
            LRItemSetDto dto = new LRItemSetDto(state.id, state.name());

            // Deduplicate: group by (prod, dot) and combine lookaheads
            Map<String, Set<GrammarSymbol>> coreToLAs = new LinkedHashMap<>();
            Map<String, LR1Item> coreToRep = new LinkedHashMap<>();
            for (LR1Item item : state.items) {
                String ck = item.coreKey();
                coreToRep.putIfAbsent(ck, item);
                coreToLAs.computeIfAbsent(ck, k -> new LinkedHashSet<>()).add(item.lookahead);
            }
            for (Map.Entry<String, LR1Item> e : coreToRep.entrySet()) {
                LR1Item rep = e.getValue();
                boolean isCore = rep.production().isAugmented() || rep.dot() > 0 || rep.production().isEpsilon();
                Set<GrammarSymbol> las = coreToLAs.get(e.getKey());
                String laStr = las.stream()
                        .map(GrammarSymbol::getName)
                        .sorted(FirstFollowService.DETERMINISTIC_SYMBOL_COMPARATOR)
                        .collect(Collectors.joining(", "));
                dto.getItems().add(new LRItemDto(rep.format(), isCore, laStr));
            }

            // Transitions
            for (GrammarSymbol sym : collectLR1GotoSymbols(state)) {
                LinkedHashSet<LR1Item> tgt = lr1Goto(state.items, sym, augmented, firstSets);
                int tgtId = findLR1StateId(states, tgt);
                if (tgtId >= 0) dto.getGotos().add(new LRGotoDto(sym.getName(), "I" + tgtId));
            }
            itemSetDtos.add(dto);
        }
        resp.setItemSets(itemSetDtos);

        // ── Headers ──
        List<String> actionHeaders = collectTerminals(augmented);
        actionHeaders.add(GrammarSymbol.EOF_STR);
        resp.setActionHeaders(actionHeaders);
        List<String> gotoHeaders = augmented.getNonTerminals().stream()
                .filter(nt -> !nt.equals(augmented.getStartSymbol()))
                .map(GrammarSymbol::getName).toList();
        resp.setGotoHeaders(gotoHeaders);

        // ── Table rows ──
        List<Map<String, Object>> tableRows = new ArrayList<>();
        for (LR1ItemSet state : states) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("state", String.valueOf(state.id));
            Map<String, String> actions = new LinkedHashMap<>();
            Map<String, List<String>> actionRow = rawAction.getOrDefault(state.id, Collections.emptyMap());
            for (String col : actionHeaders) {
                List<String> cell = actionRow.getOrDefault(col, Collections.emptyList());
                if (!cell.isEmpty()) actions.put(col, String.join(" / ", cell));
            }
            row.put("actions", actions);
            Map<String, String> gotos = new LinkedHashMap<>();
            Map<String, String> gotoRow = rawGoto.getOrDefault(state.id, Collections.emptyMap());
            for (String nt : gotoHeaders) {
                String t = gotoRow.get(nt);
                if (t != null) gotos.put(nt, t);
            }
            row.put("gotos", gotos);
            tableRows.add(row);
        }
        resp.setTableRows(tableRows);

        return resp;
    }

    // ──────────────────────────────────────────── shared LR parse driver

    /**
     * General LR parse driver usable by CLR(1) and LALR(1).
     * Identical logic to the LR(0)/SLR(1) driver; factored out to avoid duplication.
     */
    private LRParseResponse simulateLRParse(
            Grammar original,
            Grammar augmented,
            Map<Integer, Map<String, List<String>>> rawAction,
            Map<Integer, Map<String, String>> rawGoto,
            String inputString) {

        List<String> tokens = tokenize(inputString);
        Set<String> validTerminals = original.getTerminals().stream()
                .map(GrammarSymbol::getName).collect(Collectors.toSet());
        for (String tok : tokens) {
            if (!tok.equals(GrammarSymbol.EOF_STR) && !validTerminals.contains(tok)) {
                return LRParseResponse.failure(
                        "Unrecognized terminal: '" + tok + "'. Valid terminals: " + validTerminals,
                        Collections.emptyList());
            }
        }
        if (tokens.isEmpty() || !tokens.getLast().equals(GrammarSymbol.EOF_STR)) {
            tokens.add(GrammarSymbol.EOF_STR);
        }

        Deque<Integer> stateStack = new ArrayDeque<>();
        Deque<String> symStack = new ArrayDeque<>();
        stateStack.push(0);
        symStack.push(GrammarSymbol.EOF_STR);

        List<LRParseStep> steps = new ArrayList<>();
        int ptr = 0, stepNum = 1;
        final int MAX_STEPS = 2000;

        while (stepNum <= MAX_STEPS) {
            int currentState = stateStack.peek();
            String lookahead = tokens.get(ptr);
            String stateStackStr  = formatStateStack(stateStack);
            String symStackStr    = formatSymStack(symStack);
            String inputRemaining = String.join(" ", tokens.subList(ptr, tokens.size()));

            Map<String, List<String>> actionRow = rawAction.getOrDefault(currentState, Collections.emptyMap());
            List<String> cellActions = actionRow.getOrDefault(lookahead, Collections.emptyList());

            if (cellActions.isEmpty()) {
                String err = "Syntax Error: No action in state " + currentState + " for '" + lookahead + "'.";
                steps.add(new LRParseStep(stepNum, stateStackStr, symStackStr, inputRemaining, "Error: " + err, ""));
                return LRParseResponse.failure(err, steps);
            }

            String action = pickAction(cellActions);

            if (action.equals("acc")) {
                steps.add(new LRParseStep(stepNum, stateStackStr, symStackStr, inputRemaining, "Accept", ""));
                LRParseResponse res = new LRParseResponse();
                res.setAccepted(true);
                res.setMessage("String accepted.");
                res.setSteps(steps);
                return res;

            } else if (action.startsWith("s")) {
                int nextState = Integer.parseInt(action.substring(1));
                steps.add(new LRParseStep(stepNum++, stateStackStr, symStackStr, inputRemaining, "Shift " + action, ""));
                stateStack.push(nextState);
                symStack.push(lookahead);
                ptr++;

            } else if (action.startsWith("r")) {
                int prodId = Integer.parseInt(action.substring(1));
                Production prod = augmented.getProductionById(prodId)
                        .orElseThrow(() -> new IllegalStateException("Unknown production id: " + prodId));
                String ruleStr = prod.getFormattedRule();
                steps.add(new LRParseStep(stepNum++, stateStackStr, symStackStr, inputRemaining,
                        "Reduce " + action + " (" + ruleStr + ")", ruleStr));
                int popCount = prod.isEpsilon() ? 0 : prod.getRhs().size();
                for (int i = 0; i < popCount; i++) { stateStack.pop(); symStack.pop(); }
                int exposedState = stateStack.peek();
                String lhsName = prod.getLhs().getName();
                String gotoTarget = rawGoto.getOrDefault(exposedState, Collections.emptyMap()).get(lhsName);
                if (gotoTarget == null) {
                    String err = "Syntax Error: No GOTO in state " + exposedState + " for '" + lhsName + "'.";
                    steps.add(new LRParseStep(stepNum, stateStackStr, symStackStr, inputRemaining, "Error: " + err, ""));
                    return LRParseResponse.failure(err, steps);
                }
                stateStack.push(Integer.parseInt(gotoTarget));
                symStack.push(lhsName);

            } else {
                return LRParseResponse.failure("Unknown action: " + action, steps);
            }
        }
        return LRParseResponse.failure("Parse exceeded " + MAX_STEPS + " steps.", steps);
    }

    // ──────────────────────────────────────────── LR(1) utility helpers

    private Set<GrammarSymbol> collectLR1GotoSymbols(LR1ItemSet state) {
        return collectLR1GotoSymbolsFromItems(state.items);
    }

    private Set<GrammarSymbol> collectLR1GotoSymbolsFromItems(LinkedHashSet<LR1Item> items) {
        Set<GrammarSymbol> syms = new LinkedHashSet<>();
        for (LR1Item item : items) {
            GrammarSymbol sym = item.symbolAfterDot();
            if (sym != null && !sym.isEpsilon()) syms.add(sym);
        }
        return syms;
    }

    private int findLR1StateId(List<LR1ItemSet> states, LinkedHashSet<LR1Item> items) {
        for (LR1ItemSet s : states) {
            if (s.items.equals(items)) return s.id;
        }
        return -1;
    }

    private List<LRConflictDto> extractLR1Conflicts(
            List<LR1ItemSet> states,
            Map<Integer, Map<String, List<String>>> rawAction,
            String label) {

        List<LRConflictDto> conflicts = new ArrayList<>();
        for (LR1ItemSet state : states) {
            extractConflictsFromRow(state.name(), state.id, rawAction.getOrDefault(state.id, Collections.emptyMap()), label, conflicts);
        }
        return conflicts;
    }

    private List<LRConflictDto> extractLALR1Conflicts(
            List<MergedState> states,
            Map<Integer, Map<String, List<String>>> rawAction) {

        List<LRConflictDto> conflicts = new ArrayList<>();
        for (MergedState state : states) {
            extractConflictsFromRow(state.name(), state.id, rawAction.getOrDefault(state.id, Collections.emptyMap()), "LALR(1)", conflicts);
        }
        return conflicts;
    }

    private void extractConflictsFromRow(
            String stateName, int stateId,
            Map<String, List<String>> row,
            String label,
            List<LRConflictDto> conflicts) {

        for (Map.Entry<String, List<String>> entry : row.entrySet()) {
            List<String> acts = entry.getValue();
            if (acts.size() <= 1) continue;
            String terminal = entry.getKey();
            boolean hasShift  = acts.stream().anyMatch(a -> a.startsWith("s"));
            boolean hasReduce = acts.stream().anyMatch(a -> a.startsWith("r"));
            boolean hasAcc    = acts.stream().anyMatch(a -> a.equals("acc"));
            String type = (hasShift && hasReduce) || (hasAcc && hasReduce)
                    ? "Shift / Reduce Conflict" : "Reduce / Reduce Conflict";
            String message = label + " conflict in state " + stateName
                    + " on '" + terminal + "': " + String.join(", ", acts);
            conflicts.add(new LRConflictDto(type, stateName, terminal, message, acts));
        }
    }
}
