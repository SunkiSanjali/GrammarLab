package com.grammarlab.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Response model for LR(0) and SLR(1) table construction.
 * Matches what LRSectionView / ParsingTableView / ItemSetCardsView / AutomatonGraphView expect.
 */
public class LRTableResponse {

    private boolean valid = true;

    /** "LR(0)" or "SLR(1)" */
    private String parserType;

    private String startSymbol;
    private String augmentedStartSymbol;
    private String normalizedGrammar;

    /** Augmented grammar productions for display (production 0 is S' -> S). */
    private List<ProductionDto> augmentedProductions = new ArrayList<>();

    /** True if the grammar is accepted by this parser class (no conflicts). */
    private boolean deterministic;

    /** Detected shift/reduce and reduce/reduce conflicts. */
    private List<LRConflictDto> conflicts = new ArrayList<>();

    /** Canonical collection of LR(0) item sets. */
    private List<LRItemSetDto> itemSets = new ArrayList<>();

    /** Terminal headers for the ACTION section of the table. */
    private List<String> actionHeaders = new ArrayList<>();

    /** Non-terminal headers for the GOTO section of the table. */
    private List<String> gotoHeaders = new ArrayList<>();

    /** Rows of the ACTION/GOTO table. Each row: { state, actions:{t->val}, gotos:{nt->state} } */
    private List<Map<String, Object>> tableRows = new ArrayList<>();

    /**
     * LALR(1) only: describes which CLR(1) states were merged.
     * Each entry: { mergedName, from: [list of original state names], core: representative item }
     */
    private List<Map<String, Object>> mergedPairs = new ArrayList<>();

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getParserType() { return parserType; }
    public void setParserType(String parserType) { this.parserType = parserType; }

    public String getStartSymbol() { return startSymbol; }
    public void setStartSymbol(String startSymbol) { this.startSymbol = startSymbol; }

    public String getAugmentedStartSymbol() { return augmentedStartSymbol; }
    public void setAugmentedStartSymbol(String augmentedStartSymbol) { this.augmentedStartSymbol = augmentedStartSymbol; }

    public String getNormalizedGrammar() { return normalizedGrammar; }
    public void setNormalizedGrammar(String normalizedGrammar) { this.normalizedGrammar = normalizedGrammar; }

    public List<ProductionDto> getAugmentedProductions() { return augmentedProductions; }
    public void setAugmentedProductions(List<ProductionDto> augmentedProductions) { this.augmentedProductions = augmentedProductions; }

    public boolean isDeterministic() { return deterministic; }
    public void setDeterministic(boolean deterministic) { this.deterministic = deterministic; }

    public List<LRConflictDto> getConflicts() { return conflicts; }
    public void setConflicts(List<LRConflictDto> conflicts) { this.conflicts = conflicts; }

    public List<LRItemSetDto> getItemSets() { return itemSets; }
    public void setItemSets(List<LRItemSetDto> itemSets) { this.itemSets = itemSets; }

    public List<String> getActionHeaders() { return actionHeaders; }
    public void setActionHeaders(List<String> actionHeaders) { this.actionHeaders = actionHeaders; }

    public List<String> getGotoHeaders() { return gotoHeaders; }
    public void setGotoHeaders(List<String> gotoHeaders) { this.gotoHeaders = gotoHeaders; }

    public List<Map<String, Object>> getTableRows() { return tableRows; }
    public void setTableRows(List<Map<String, Object>> tableRows) { this.tableRows = tableRows; }

    public List<Map<String, Object>> getMergedPairs() { return mergedPairs; }
    public void setMergedPairs(List<Map<String, Object>> mergedPairs) { this.mergedPairs = mergedPairs; }
}
