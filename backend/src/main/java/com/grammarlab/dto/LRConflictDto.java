package com.grammarlab.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a shift/reduce or reduce/reduce conflict in an LR parsing table.
 * Matches the ConflictBanner component's expected structure.
 */
public class LRConflictDto {

    /** "Shift / Reduce Conflict" or "Reduce / Reduce Conflict" */
    private String type;

    /** State where the conflict occurs (e.g., "I2"). */
    private String state;

    /** The terminal lookahead symbol triggering the conflict. */
    private String terminal;

    /** Human-readable explanation. */
    private String message;

    /** The conflicting actions (e.g., "s2", "r3 (E -> T)"). */
    private List<String> rules = new ArrayList<>();

    public LRConflictDto() {}

    public LRConflictDto(String type, String state, String terminal, String message, List<String> rules) {
        this.type = type;
        this.state = state;
        this.terminal = terminal;
        this.message = message;
        this.rules = rules;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getRules() { return rules; }
    public void setRules(List<String> rules) { this.rules = rules; }
}
