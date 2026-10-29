package com.grammarlab.exception;

import com.grammarlab.dto.GrammarValidationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GrammarParseException.class)
    public ResponseEntity<GrammarValidationResponse> handleGrammarParseException(GrammarParseException ex) {
        GrammarValidationResponse response = GrammarValidationResponse.failure(ex.getErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GrammarValidationResponse> handleIllegalArgument(IllegalArgumentException ex) {
        GrammarValidationResponse response = GrammarValidationResponse.failure(List.of(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GrammarValidationResponse> handleGenericException(Exception ex) {
        GrammarValidationResponse response = GrammarValidationResponse.failure(
                List.of("An unexpected server error occurred: " + ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
