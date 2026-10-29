package com.grammarlab.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Response model containing LL(1) table, deterministic columns, and conflict diagnostics.
 */
public class LL1TableResponse {

    private boolean valid;
    private boolean isLL1;
    private String startSymbol;
    private String normalizedGrammar;
    private List<String> headers = new ArrayList<>();
    private List<LL1TableRowDto> rows = new ArrayList<>();
    private Map<String, Map<String, List<String>>> rawTable = new LinkedHashMap<>();
    private List<LL1Conflict> conflicts = new ArrayList<>();

    public LL1TableResponse() {}

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isLL1() {
        return isLL1;
    }

    public void setLL1(boolean LL1) {
        isLL1 = LL1;
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

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<LL1TableRowDto> getRows() {
        return rows;
    }

    public void setRows(List<LL1TableRowDto> rows) {
        this.rows = rows;
    }

    public Map<String, Map<String, List<String>>> getRawTable() {
        return rawTable;
    }

    public void setRawTable(Map<String, Map<String, List<String>>> rawTable) {
        this.rawTable = rawTable;
    }

    public List<LL1Conflict> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<LL1Conflict> conflicts) {
        this.conflicts = conflicts;
    }
}
