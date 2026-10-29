package com.grammarlab.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of LL(1) string parsing simulation.
 */
public class LL1ParseResponse {

    private boolean accepted;
    private boolean isLL1;
    private String message;
    private List<LL1ParseStep> steps = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public LL1ParseResponse() {}

    public static LL1ParseResponse failure(String message, List<LL1ParseStep> steps) {
        LL1ParseResponse res = new LL1ParseResponse();
        res.setAccepted(false);
        res.setMessage(message);
        res.setSteps(steps != null ? steps : new ArrayList<>());
        res.setErrors(List.of(message));
        return res;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isLL1() {
        return isLL1;
    }

    public void setLL1(boolean LL1) {
        isLL1 = LL1;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<LL1ParseStep> getSteps() {
        return steps;
    }

    public void setSteps(List<LL1ParseStep> steps) {
        this.steps = steps;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
