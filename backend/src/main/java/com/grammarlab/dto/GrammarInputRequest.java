package com.grammarlab.dto;

public class GrammarInputRequest {

    private String rawGrammar;
    private String startSymbol;

    public GrammarInputRequest() {}

    public GrammarInputRequest(String rawGrammar) {
        this.rawGrammar = rawGrammar;
    }

    public GrammarInputRequest(String rawGrammar, String startSymbol) {
        this.rawGrammar = rawGrammar;
        this.startSymbol = startSymbol;
    }

    public String getRawGrammar() {
        return rawGrammar;
    }

    public void setRawGrammar(String rawGrammar) {
        this.rawGrammar = rawGrammar;
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public void setStartSymbol(String startSymbol) {
        this.startSymbol = startSymbol;
    }
}
