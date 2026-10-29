package com.grammarlab.dto;

import java.util.List;

/**
 * Representation of FIRST and FOLLOW sets for a single non-terminal.
 */
public class FirstFollowItemDto {

    private String symbol;
    private boolean isNullable;
    private List<String> first;
    private List<String> follow;

    public FirstFollowItemDto() {}

    public FirstFollowItemDto(String symbol, boolean isNullable, List<String> first, List<String> follow) {
        this.symbol = symbol;
        this.isNullable = isNullable;
        this.first = first;
        this.follow = follow;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public List<String> getFirst() {
        return first;
    }

    public void setFirst(List<String> first) {
        this.first = first;
    }

    public List<String> getFollow() {
        return follow;
    }

    public void setFollow(List<String> follow) {
        this.follow = follow;
    }
}
