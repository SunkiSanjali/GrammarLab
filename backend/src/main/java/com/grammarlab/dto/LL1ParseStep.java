package com.grammarlab.dto;

/**
 * Single execution step in LL(1) predictive stack trace.
 */
public class LL1ParseStep {

    private int step;
    private String stack;
    private String symbolStack;
    private String inputBuffer;
    private String action;
    private String rule;

    public LL1ParseStep() {}

    public LL1ParseStep(int step, String stack, String inputBuffer, String action, String rule) {
        this.step = step;
        this.stack = stack;
        this.symbolStack = stack;
        this.inputBuffer = inputBuffer;
        this.action = action;
        this.rule = rule;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
        this.symbolStack = stack;
    }

    public String getSymbolStack() {
        return symbolStack;
    }

    public void setSymbolStack(String symbolStack) {
        this.symbolStack = symbolStack;
        this.stack = symbolStack;
    }

    public String getInputBuffer() {
        return inputBuffer;
    }

    public void setInputBuffer(String inputBuffer) {
        this.inputBuffer = inputBuffer;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }
}
