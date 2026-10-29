package com.grammarlab.dto;

public class LL1ParseRequest {

    private String rawGrammar;
    private String startSymbol;
    private String inputString;

    public LL1ParseRequest() {}

    public LL1ParseRequest(String rawGrammar, String inputString) {
        this.rawGrammar = rawGrammar;
        this.inputString = inputString;
    }

    public LL1ParseRequest(String rawGrammar, String startSymbol, String inputString) {
        this.rawGrammar = rawGrammar;
        this.startSymbol = startSymbol;
        this.inputString = inputString;
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

    public String getInputString() {
        return inputString;
    }

    public void setInputString(String inputString) {
        this.inputString = inputString;
    }
}
