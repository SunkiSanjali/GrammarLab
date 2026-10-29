package com.grammarlab.dto;

import java.util.ArrayList;
import java.util.List;

public class GrammarValidationResponse {

    private boolean valid;
    private String startSymbol;
    private List<String> nonTerminals = new ArrayList<>();
    private List<String> terminals = new ArrayList<>();
    private List<ProductionDto> productions = new ArrayList<>();
    private List<ProductionDto> augmentedProductions = new ArrayList<>();
    private String normalizedGrammar;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public GrammarValidationResponse() {}

    public static GrammarValidationResponse failure(List<String> errors) {
        GrammarValidationResponse res = new GrammarValidationResponse();
        res.setValid(false);
        res.setErrors(errors != null ? errors : List.of("Unknown validation error"));
        return res;
    }

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

    public List<String> getNonTerminals() {
        return nonTerminals;
    }

    public void setNonTerminals(List<String> nonTerminals) {
        this.nonTerminals = nonTerminals;
    }

    public List<String> getTerminals() {
        return terminals;
    }

    public void setTerminals(List<String> terminals) {
        this.terminals = terminals;
    }

    public List<ProductionDto> getProductions() {
        return productions;
    }

    public void setProductions(List<ProductionDto> productions) {
        this.productions = productions;
    }

    public List<ProductionDto> getAugmentedProductions() {
        return augmentedProductions;
    }

    public void setAugmentedProductions(List<ProductionDto> augmentedProductions) {
        this.augmentedProductions = augmentedProductions;
    }

    public String getNormalizedGrammar() {
        return normalizedGrammar;
    }

    public void setNormalizedGrammar(String normalizedGrammar) {
        this.normalizedGrammar = normalizedGrammar;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
