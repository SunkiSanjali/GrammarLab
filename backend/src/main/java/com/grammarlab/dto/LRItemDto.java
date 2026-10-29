package com.grammarlab.dto;

/**
 * Represents a single LR(0) or SLR(1) item for frontend display.
 * Example: "E -> E • + T"
 */
public class LRItemDto {

    /** The formatted item string with bullet '•' marking the dot position. */
    private String rule;

    /** True if this is a kernel (core) item (dot is not at position 0), or the augmented start item. */
    private boolean core;

    /** Lookahead string (used for CLR/LALR; null/empty for LR(0)/SLR(1)). */
    private String lookahead;

    public LRItemDto() {}

    public LRItemDto(String rule, boolean core) {
        this.rule = rule;
        this.core = core;
    }

    public LRItemDto(String rule, boolean core, String lookahead) {
        this.rule = rule;
        this.core = core;
        this.lookahead = lookahead;
    }

    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }

    public boolean isCore() { return core; }
    public void setCore(boolean core) { this.core = core; }

    public String getLookahead() { return lookahead; }
    public void setLookahead(String lookahead) { this.lookahead = lookahead; }
}
