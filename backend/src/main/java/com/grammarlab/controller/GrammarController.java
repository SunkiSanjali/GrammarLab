package com.grammarlab.controller;

import com.grammarlab.dto.GrammarInputRequest;
import com.grammarlab.dto.GrammarValidationResponse;
import com.grammarlab.service.GrammarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing grammar parsing, normalization, and validation endpoints.
 */
@RestController
@RequestMapping("/api/grammar")
public class GrammarController {

    private final GrammarService grammarService;
    private final com.grammarlab.service.FirstFollowService firstFollowService;
    private final com.grammarlab.service.LL1Service ll1Service;
    private final com.grammarlab.service.LRService lrService;

    public GrammarController(
            GrammarService grammarService,
            com.grammarlab.service.FirstFollowService firstFollowService,
            com.grammarlab.service.LL1Service ll1Service,
            com.grammarlab.service.LRService lrService
    ) {
        this.grammarService = grammarService;
        this.firstFollowService = firstFollowService;
        this.ll1Service = ll1Service;
        this.lrService = lrService;
    }

    /**
     * Parses and validates a CFG input, returning its normalized representation,
     * terminals, non-terminals, start symbol, ordered productions, and augmented rules.
     */
    @PostMapping("/validate")
    public ResponseEntity<GrammarValidationResponse> validateGrammar(@RequestBody GrammarInputRequest request) {
        GrammarValidationResponse response = grammarService.processGrammar(
                request.getRawGrammar(),
                request.getStartSymbol()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Alias endpoint for grammar parsing and normalization.
     */
    @PostMapping("/parse")
    public ResponseEntity<GrammarValidationResponse> parseGrammar(@RequestBody GrammarInputRequest request) {
        return validateGrammar(request);
    }

    /**
     * Computes FIRST and FOLLOW sets for all non-terminals using fixed-point iteration.
     */
    @PostMapping("/first-follow")
    public ResponseEntity<com.grammarlab.dto.FirstFollowResponse> computeFirstFollow(@RequestBody GrammarInputRequest request) {
        com.grammarlab.dto.FirstFollowResponse response = firstFollowService.compute(
                request.getRawGrammar(),
                request.getStartSymbol()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Constructs the LL(1) parsing table and analyzes conflicts.
     */
    @PostMapping(value = {"/ll1/table", "/ll1"})
    public ResponseEntity<com.grammarlab.dto.LL1TableResponse> getLL1Table(@RequestBody GrammarInputRequest request) {
        com.grammarlab.dto.LL1TableResponse response = ll1Service.buildTable(
                request.getRawGrammar(),
                request.getStartSymbol()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Simulates LL(1) stack parsing trace for a given input string.
     */
    @PostMapping("/ll1/parse")
    public ResponseEntity<com.grammarlab.dto.LL1ParseResponse> parseLL1String(@RequestBody com.grammarlab.dto.LL1ParseRequest request) {
        com.grammarlab.dto.LL1ParseResponse response = ll1Service.simulateParse(
                request.getRawGrammar(),
                request.getStartSymbol(),
                request.getInputString()
        );
        return ResponseEntity.ok(response);
    }

    // ── LR(0) ─────────────────────────────────────────────────────────────

    /**
     * Constructs the LR(0) canonical item sets, DFA transitions,
     * and ACTION/GOTO parsing table with conflict detection.
     */
    @PostMapping("/lr0")
    public ResponseEntity<com.grammarlab.dto.LRTableResponse> getLR0Table(
            @RequestBody com.grammarlab.dto.GrammarInputRequest request) {
        com.grammarlab.dto.LRTableResponse response = lrService.buildLR0Table(
                request.getRawGrammar(), request.getStartSymbol());
        return ResponseEntity.ok(response);
    }

    /**
     * Simulates LR(0) table-driven parsing of an input string.
     */
    @PostMapping("/lr0/parse")
    public ResponseEntity<com.grammarlab.dto.LRParseResponse> parseLR0String(
            @RequestBody com.grammarlab.dto.LRParseRequest request) {
        com.grammarlab.dto.LRParseResponse response = lrService.parseLR0(
                request.getRawGrammar(), request.getStartSymbol(), request.getInputString());
        return ResponseEntity.ok(response);
    }

    // ── SLR(1) ────────────────────────────────────────────────────────────

    /**
     * Constructs the SLR(1) parsing table reusing LR(0) item sets
     * and restricting reductions to FOLLOW sets.
     */
    @PostMapping("/slr1")
    public ResponseEntity<com.grammarlab.dto.LRTableResponse> getSLR1Table(
            @RequestBody com.grammarlab.dto.GrammarInputRequest request) {
        com.grammarlab.dto.LRTableResponse response = lrService.buildSLR1Table(
                request.getRawGrammar(), request.getStartSymbol());
        return ResponseEntity.ok(response);
    }

    /**
     * Simulates SLR(1) table-driven parsing of an input string.
     */
    @PostMapping("/slr1/parse")
    public ResponseEntity<com.grammarlab.dto.LRParseResponse> parseSLR1String(
            @RequestBody com.grammarlab.dto.LRParseRequest request) {
        com.grammarlab.dto.LRParseResponse response = lrService.parseSLR1(
                request.getRawGrammar(), request.getStartSymbol(), request.getInputString());
        return ResponseEntity.ok(response);
    }

    // ── CLR(1) ────────────────────────────────────────────────────────────

    /**
     * Constructs the CLR(1) canonical LR(1) item sets, lookaheads,
     * and ACTION/GOTO parsing table with conflict detection.
     */
    @PostMapping("/clr1")
    public ResponseEntity<com.grammarlab.dto.LRTableResponse> getCLR1Table(
            @RequestBody com.grammarlab.dto.GrammarInputRequest request) {
        com.grammarlab.dto.LRTableResponse response = lrService.buildCLR1Table(
                request.getRawGrammar(), request.getStartSymbol());
        return ResponseEntity.ok(response);
    }

    /**
     * Simulates CLR(1) table-driven parsing of an input string.
     */
    @PostMapping("/clr1/parse")
    public ResponseEntity<com.grammarlab.dto.LRParseResponse> parseCLR1String(
            @RequestBody com.grammarlab.dto.LRParseRequest request) {
        com.grammarlab.dto.LRParseResponse response = lrService.parseCLR1(
                request.getRawGrammar(), request.getStartSymbol(), request.getInputString());
        return ResponseEntity.ok(response);
    }

    // ── LALR(1) ───────────────────────────────────────────────────────────

    /**
     * Constructs the LALR(1) parsing table by merging CLR(1) item sets
     * that share identical LR(0) cores.
     */
    @PostMapping("/lalr1")
    public ResponseEntity<com.grammarlab.dto.LRTableResponse> getLALR1Table(
            @RequestBody com.grammarlab.dto.GrammarInputRequest request) {
        com.grammarlab.dto.LRTableResponse response = lrService.buildLALR1Table(
                request.getRawGrammar(), request.getStartSymbol());
        return ResponseEntity.ok(response);
    }

    /**
     * Simulates LALR(1) table-driven parsing of an input string.
     */
    @PostMapping("/lalr1/parse")
    public ResponseEntity<com.grammarlab.dto.LRParseResponse> parseLALR1String(
            @RequestBody com.grammarlab.dto.LRParseRequest request) {
        com.grammarlab.dto.LRParseResponse response = lrService.parseLALR1(
                request.getRawGrammar(), request.getStartSymbol(), request.getInputString());
        return ResponseEntity.ok(response);
    }

    /**
     * Unified parse dispatcher for the StringParserView simulation panel.
     * Accepts parserType: "LL(1)", "LR(0)", "SLR(1)", "CLR(1)", "LALR(1)".
     */
    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@RequestBody com.grammarlab.dto.LRParseRequest request) {
        String type = request.getParserType();
        if ("LL(1)".equalsIgnoreCase(type) || "LL1".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(ll1Service.simulateParse(
                    request.getRawGrammar(), request.getStartSymbol(), request.getInputString()));
        } else if ("SLR(1)".equalsIgnoreCase(type) || "SLR1".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(lrService.parseSLR1(
                    request.getRawGrammar(), request.getStartSymbol(), request.getInputString()));
        } else if ("CLR(1)".equalsIgnoreCase(type) || "CLR1".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(lrService.parseCLR1(
                    request.getRawGrammar(), request.getStartSymbol(), request.getInputString()));
        } else if ("LALR(1)".equalsIgnoreCase(type) || "LALR1".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(lrService.parseLALR1(
                    request.getRawGrammar(), request.getStartSymbol(), request.getInputString()));
        } else {
            // Default to LR(0)
            return ResponseEntity.ok(lrService.parseLR0(
                    request.getRawGrammar(), request.getStartSymbol(), request.getInputString()));
        }
    }
}
