package com.grammarlab.dto;

import java.util.List;

/**
 * Details of an LL(1) parsing table conflict.
 */
public class LL1Conflict {

    private String type; // e.g. "First-First Conflict" or "First-Follow Conflict"
    private String nonTerminal;
    private String terminal;
    private List<String> rules;
    private String message;

    public LL1Conflict() {}

    public LL1Conflict(String type, String nonTerminal, String terminal, List<String> rules, String message) {
        this.type = type;
        this.nonTerminal = nonTerminal;
        this.terminal = terminal;
        this.rules = rules;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNonTerminal() {
        return nonTerminal;
    }

    public void setNonTerminal(String nonTerminal) {
        this.nonTerminal = nonTerminal;
    }

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
