package com.grammarlab.dto;

import com.grammarlab.model.GrammarSymbol;
import com.grammarlab.model.Production;

import java.util.List;

public class ProductionDto {

    private int id;
    private String lhs;
    private List<String> rhs;
    private String formattedRule;

    public ProductionDto() {}

    public ProductionDto(int id, String lhs, List<String> rhs, String formattedRule) {
        this.id = id;
        this.lhs = lhs;
        this.rhs = rhs;
        this.formattedRule = formattedRule;
    }

    public static ProductionDto from(Production p) {
        List<String> rhsNames = p.getRhs().stream().map(GrammarSymbol::getName).toList();
        return new ProductionDto(p.getId(), p.getLhs().getName(), rhsNames, p.getFormattedRule());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLhs() {
        return lhs;
    }

    public void setLhs(String lhs) {
        this.lhs = lhs;
    }

    public List<String> getRhs() {
        return rhs;
    }

    public void setRhs(List<String> rhs) {
        this.rhs = rhs;
    }

    public String getFormattedRule() {
        return formattedRule;
    }

    public void setFormattedRule(String formattedRule) {
        this.formattedRule = formattedRule;
    }
}
