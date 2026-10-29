package com.grammarlab.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response from an LR(0) or SLR(1) string parsing simulation.
 */
public class LRParseResponse {

    private boolean accepted;
    private String message;
    private List<LRParseStep> steps = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public LRParseResponse() {}

    public static LRParseResponse failure(String message, List<LRParseStep> steps) {
        LRParseResponse res = new LRParseResponse();
        res.setAccepted(false);
        res.setMessage(message);
        res.setSteps(steps != null ? steps : new ArrayList<>());
        res.setErrors(List.of(message));
        return res;
    }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<LRParseStep> getSteps() { return steps; }
    public void setSteps(List<LRParseStep> steps) { this.steps = steps; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
