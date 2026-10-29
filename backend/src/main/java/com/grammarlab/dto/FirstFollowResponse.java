package com.grammarlab.dto;

import java.util.*;

/**
 * API response model for POST /api/grammar/first-follow.
 */
public class FirstFollowResponse {

    private boolean valid;
    private String startSymbol;
    private String normalizedGrammar;
    private List<FirstFollowItemDto> items = new ArrayList<>();
    private Map<String, List<String>> firstSets = new LinkedHashMap<>();
    private Map<String, List<String>> followSets = new LinkedHashMap<>();
    private List<String> nullableNonTerminals = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public FirstFollowResponse() {}

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public void setStartSymbol(String startSymbol) {
        this.startSymbol = startSymbol;
    }

    public String getNormalizedGrammar() {
        return normalizedGrammar;
    }

    public void setNormalizedGrammar(String normalizedGrammar) {
        this.normalizedGrammar = normalizedGrammar;
    }

    public List<FirstFollowItemDto> getItems() {
        return items;
    }

    public void setItems(List<FirstFollowItemDto> items) {
        this.items = items;
    }

    public Map<String, List<String>> getFirstSets() {
        return firstSets;
    }

    public void setFirstSets(Map<String, List<String>> firstSets) {
        this.firstSets = firstSets;
    }

    public Map<String, List<String>> getFollowSets() {
        return followSets;
    }

    public void setFollowSets(Map<String, List<String>> followSets) {
        this.followSets = followSets;
    }

    public List<String> getNullableNonTerminals() {
        return nullableNonTerminals;
    }

    public void setNullableNonTerminals(List<String> nullableNonTerminals) {
        this.nullableNonTerminals = nullableNonTerminals;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
