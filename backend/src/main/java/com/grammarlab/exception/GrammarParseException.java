package com.grammarlab.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when grammar syntax or semantic validation fails.
 */
public class GrammarParseException extends RuntimeException {

    private final List<String> errors;

    public GrammarParseException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public GrammarParseException(List<String> errors) {
        super(errors.isEmpty() ? "Grammar parsing failed." : errors.getFirst());
        this.errors = Collections.unmodifiableList(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
