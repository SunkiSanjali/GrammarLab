package com.grammarlab.dto;

/**
 * Request for LR(0) and SLR(1) string parsing simulations.
 */
public class LRParseRequest {

    private String rawGrammar;
    private String startSymbol;
    private String inputString;
    private String parserType;

    public LRParseRequest() {}

    public LRParseRequest(String rawGrammar, String inputString) {
        this.rawGrammar = rawGrammar;
        this.inputString = inputString;
    }

    public LRParseRequest(String rawGrammar, String startSymbol, String inputString) {
        this.rawGrammar = rawGrammar;
        this.startSymbol = startSymbol;
        this.inputString = inputString;
    }

    public LRParseRequest(String rawGrammar, String startSymbol, String inputString, String parserType) {
        this.rawGrammar = rawGrammar;
        this.startSymbol = startSymbol;
        this.inputString = inputString;
        this.parserType = parserType;
    }

    public String getRawGrammar() { return rawGrammar; }
    public void setRawGrammar(String rawGrammar) { this.rawGrammar = rawGrammar; }

    public String getStartSymbol() { return startSymbol; }
    public void setStartSymbol(String startSymbol) { this.startSymbol = startSymbol; }

    public String getInputString() { return inputString; }
    public void setInputString(String inputString) { this.inputString = inputString; }

    public String getParserType() { return parserType; }
    public void setParserType(String parserType) { this.parserType = parserType; }
}
