package com.grammarlab.service;

import com.grammarlab.dto.GrammarValidationResponse;
import com.grammarlab.dto.ProductionDto;
import com.grammarlab.exception.GrammarParseException;
import com.grammarlab.model.Grammar;
import com.grammarlab.model.GrammarSymbol;
import com.grammarlab.parser.GrammarParser;
import com.grammarlab.validator.GrammarValidator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * High-level service that handles grammar parsing, validation,
 * and presentation mapping.
 */
@Service
public class GrammarService {

    private final GrammarParser grammarParser;
    private final GrammarValidator grammarValidator;

    public GrammarService(GrammarParser grammarParser, GrammarValidator grammarValidator) {
        this.grammarParser = grammarParser;
        this.grammarValidator = grammarValidator;
    }

    /**
     * Parses, validates, and transforms a raw grammar string.
     */
    public GrammarValidationResponse processGrammar(String rawGrammar, String customStartSymbol) {
        Grammar grammar = grammarParser.parse(rawGrammar, customStartSymbol);

        GrammarValidator.ValidationResult validationResult = grammarValidator.validate(grammar);
        if (!validationResult.isValid()) {
            throw new GrammarParseException(validationResult.errors());
        }

        Grammar augmentedGrammar = grammar.getAugmentedGrammar();

        GrammarValidationResponse response = new GrammarValidationResponse();
        response.setValid(true);
        response.setStartSymbol(grammar.getStartSymbol().getName());

        response.setNonTerminals(
                grammar.getNonTerminals().stream().map(GrammarSymbol::getName).toList()
        );
        response.setTerminals(
                grammar.getTerminals().stream().map(GrammarSymbol::getName).toList()
        );

        response.setProductions(
                grammar.getProductions().stream().map(ProductionDto::from).toList()
        );

        response.setAugmentedProductions(
                augmentedGrammar.getProductions().stream().map(ProductionDto::from).toList()
        );

        response.setNormalizedGrammar(grammar.getNormalizedRepresentation());
        response.setWarnings(validationResult.warnings());

        return response;
    }

    /**
     * Helper to obtain the domain Grammar object directly for downstream compiler engines.
     */
    public Grammar getParsedGrammar(String rawGrammar, String customStartSymbol) {
        Grammar grammar = grammarParser.parse(rawGrammar, customStartSymbol);
        GrammarValidator.ValidationResult result = grammarValidator.validate(grammar);
        if (!result.isValid()) {
            throw new GrammarParseException(result.errors());
        }
        return grammar;
    }
}
