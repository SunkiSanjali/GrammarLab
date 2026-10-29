package com.grammarlab.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single row in the LL(1) parsing table (corresponds to one non-terminal).
 */
public class LL1TableRowDto {

    private String nonTerminal;
    private Map<String, String> cells = new LinkedHashMap<>();

    public LL1TableRowDto() {}

    public LL1TableRowDto(String nonTerminal, Map<String, String> cells) {
        this.nonTerminal = nonTerminal;
        this.cells = cells;
    }

    public String getNonTerminal() {
        return nonTerminal;
    }

    public void setNonTerminal(String nonTerminal) {
        this.nonTerminal = nonTerminal;
    }

    public Map<String, String> getCells() {
        return cells;
    }

    public void setCells(Map<String, String> cells) {
        this.cells = cells;
    }
}
