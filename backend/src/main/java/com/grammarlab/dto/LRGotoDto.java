package com.grammarlab.dto;

/**
 * Represents a GOTO transition from one LR item set to another on a given symbol.
 */
public class LRGotoDto {

    /** The grammar symbol (terminal or non-terminal) triggering the transition. */
    private String symbol;

    /** Target state name (e.g., "I3"). */
    private String target;

    public LRGotoDto() {}

    public LRGotoDto(String symbol, String target) {
        this.symbol = symbol;
        this.target = target;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
}
