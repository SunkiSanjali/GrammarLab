package com.grammarlab.dto;

/**
 * One execution step in an LR shift-reduce parse trace.
 * Matches the StringParserView trace table columns:
 * Step | State Stack | Symbol Stack | Input Buffer | Action | Production
 */
public class LRParseStep {

    private int step;
    private String stateStack;
    private String symbolStack;
    private String inputBuffer;
    private String action;
    private String rule;

    public LRParseStep() {}

    public LRParseStep(int step, String stateStack, String symbolStack,
                       String inputBuffer, String action, String rule) {
        this.step = step;
        this.stateStack = stateStack;
        this.symbolStack = symbolStack;
        this.inputBuffer = inputBuffer;
        this.action = action;
        this.rule = rule;
    }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public String getStateStack() { return stateStack; }
    public void setStateStack(String stateStack) { this.stateStack = stateStack; }

    public String getSymbolStack() { return symbolStack; }
    public void setSymbolStack(String symbolStack) { this.symbolStack = symbolStack; }

    public String getInputBuffer() { return inputBuffer; }
    public void setInputBuffer(String inputBuffer) { this.inputBuffer = inputBuffer; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }
}
