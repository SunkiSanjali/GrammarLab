package com.grammarlab;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GrammarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/grammar/validate succeeds for valid CFG")
    void testValidateValidGrammarEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "E -> E + T | T\\nT -> id"
                }
                """;

        mockMvc.perform(post("/api/grammar/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.startSymbol", is("E")))
                .andExpect(jsonPath("$.nonTerminals", hasItems("E", "T")))
                .andExpect(jsonPath("$.terminals", hasItems("+", "id")))
                .andExpect(jsonPath("$.productions", hasSize(3)))
                .andExpect(jsonPath("$.augmentedProductions", hasSize(4)))
                .andExpect(jsonPath("$.augmentedProductions[0].lhs", is("E'")));
    }

    @Test
    @DisplayName("POST /api/grammar/validate returns 400 with errors for invalid grammar")
    void testValidateInvalidGrammarEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "E -> $ + T"
                }
                """;

        mockMvc.perform(post("/api/grammar/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.errors", hasItem(containsString("'$' is reserved"))));
    }

    @Test
    @DisplayName("POST /api/grammar/validate supports custom start symbol")
    void testCustomStartSymbolEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "E -> T\\nT -> id",
                    "startSymbol": "T"
                }
                """;

        mockMvc.perform(post("/api/grammar/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.startSymbol", is("T")))
                .andExpect(jsonPath("$.augmentedProductions[0].lhs", is("T'")));
    }

    @Test
    @DisplayName("POST /api/grammar/first-follow returns computed FIRST and FOLLOW sets")
    void testFirstFollowEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "E -> E + T | T\\nT -> T * F | F\\nF -> ( E ) | id"
                }
                """;

        mockMvc.perform(post("/api/grammar/first-follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.firstSets.E", hasItems("(", "id")))
                .andExpect(jsonPath("$.followSets.E", hasItems("+", ")", "$")))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].symbol", is("E")))
                .andExpect(jsonPath("$.items[0].nullable", is(false)));
    }

    @Test
    @DisplayName("POST /api/grammar/ll1/table returns LL(1) parsing table and conflict analysis")
    void testLL1TableEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "E -> T E'\\nE' -> + T E' | ε\\nT -> F T'\\nT' -> * F T' | ε\\nF -> ( E ) | id"
                }
                """;

        mockMvc.perform(post("/api/grammar/ll1/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.ll1", is(true)))
                .andExpect(jsonPath("$.headers", hasItem("$")))
                .andExpect(jsonPath("$.conflicts", empty()));
    }

    @Test
    @DisplayName("POST /api/grammar/ll1/parse simulates step-by-step parsing trace")
    void testLL1ParseEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "E -> T E'\\nE' -> + T E' | ε\\nT -> F T'\\nT' -> * F T' | ε\\nF -> ( E ) | id",
                    "inputString": "id + id"
                }
                """;

        mockMvc.perform(post("/api/grammar/ll1/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(true)))
                .andExpect(jsonPath("$.steps", not(empty())))
                .andExpect(jsonPath("$.steps[-1].action", is("Accept")));
    }

    @Test
    @DisplayName("POST /api/grammar/lr0 returns canonical item sets, headers, and parsing table")
    void testLR0TableEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> a A\\nA -> a A | b"
                }
                """;

        mockMvc.perform(post("/api/grammar/lr0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.parserType", is("LR(0)")))
                .andExpect(jsonPath("$.deterministic", is(true)))
                .andExpect(jsonPath("$.itemSets", not(empty())))
                .andExpect(jsonPath("$.actionHeaders", hasItems("a", "b", "$")))
                .andExpect(jsonPath("$.gotoHeaders", hasItems("S", "A")))
                .andExpect(jsonPath("$.tableRows", not(empty())))
                .andExpect(jsonPath("$.conflicts", empty()));
    }

    @Test
    @DisplayName("POST /api/grammar/lr0 detects conflicts for ambiguous grammar")
    void testLR0TableWithConflictsEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> a A | a\\nA -> b"
                }
                """;

        mockMvc.perform(post("/api/grammar/lr0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.deterministic", is(false)))
                .andExpect(jsonPath("$.conflicts", not(empty())))
                .andExpect(jsonPath("$.conflicts[0].type", is("Shift / Reduce Conflict")));
    }

    @Test
    @DisplayName("POST /api/grammar/lr0/parse simulates LR(0) string parsing acceptance trace")
    void testLR0ParseEndpointAccept() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> a A\\nA -> a A | b",
                    "inputString": "a b"
                }
                """;

        mockMvc.perform(post("/api/grammar/lr0/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(true)))
                .andExpect(jsonPath("$.steps", not(empty())))
                .andExpect(jsonPath("$.steps[-1].action", is("Accept")));
    }

    @Test
    @DisplayName("POST /api/grammar/lr0/parse rejects invalid input string with syntax error")
    void testLR0ParseEndpointReject() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> a A\\nA -> a A | b",
                    "inputString": "b"
                }
                """;

        mockMvc.perform(post("/api/grammar/lr0/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(false)))
                .andExpect(jsonPath("$.message", containsString("Syntax Error")));
    }

    @Test
    @DisplayName("POST /api/grammar/simulate routes to LR(0) when parserType is LR(0)")
    void testSimulateDispatcherLR0() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> a A\\nA -> a A | b",
                    "parserType": "LR(0)",
                    "inputString": "a a b"
                }
                """;

        mockMvc.perform(post("/api/grammar/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(true)))
                .andExpect(jsonPath("$.steps", not(empty())))
                .andExpect(jsonPath("$.steps[-1].action", is("Accept")));
    }

    @Test
    @DisplayName("POST /api/grammar/slr1 returns SLR(1) parsing table and resolves conflicts where possible")
    void testSLR1TableEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> L = R | R\\nL -> * R | id\\nR -> L"
                }
                """;

        mockMvc.perform(post("/api/grammar/slr1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.parserType", is("SLR(1)")))
                .andExpect(jsonPath("$.actionHeaders", hasItems("=", "*", "id", "$")))
                .andExpect(jsonPath("$.gotoHeaders", hasItems("S", "L", "R")))
                .andExpect(jsonPath("$.tableRows", not(empty())));
    }

    @Test
    @DisplayName("POST /api/grammar/slr1/parse successfully parses valid input")
    void testSLR1ParseEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> L = R | R\\nL -> * R | id\\nR -> L",
                    "inputString": "* id = id"
                }
                """;

        mockMvc.perform(post("/api/grammar/slr1/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(true)))
                .andExpect(jsonPath("$.steps", not(empty())))
                .andExpect(jsonPath("$.steps[-1].action", is("Accept")));
    }

    @Test
    @DisplayName("POST /api/grammar/clr1 returns CLR(1) canonical collection with lookaheads")
    void testCLR1TableEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> C C\\nC -> c C | d"
                }
                """;

        mockMvc.perform(post("/api/grammar/clr1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.parserType", is("CLR(1)")))
                .andExpect(jsonPath("$.deterministic", is(true)))
                .andExpect(jsonPath("$.itemSets", not(empty())))
                .andExpect(jsonPath("$.conflicts", empty()));
    }

    @Test
    @DisplayName("POST /api/grammar/clr1/parse accepts valid CLR(1) input string")
    void testCLR1ParseEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> C C\\nC -> c C | d",
                    "inputString": "c d d"
                }
                """;

        mockMvc.perform(post("/api/grammar/clr1/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(true)))
                .andExpect(jsonPath("$.steps", not(empty())))
                .andExpect(jsonPath("$.steps[-1].action", is("Accept")));
    }

    @Test
    @DisplayName("POST /api/grammar/lalr1 merges compatible states and returns LALR(1) table")
    void testLALR1TableEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> C C\\nC -> c C | d"
                }
                """;

        mockMvc.perform(post("/api/grammar/lalr1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.parserType", is("LALR(1)")))
                .andExpect(jsonPath("$.itemSets", hasSize(7)))
                .andExpect(jsonPath("$.conflicts", empty()));
    }

    @Test
    @DisplayName("POST /api/grammar/lalr1 detects reduce-reduce conflict on LR(1) grammar")
    void testLALR1ConflictEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> a A c | b B c | a B d | b A d\\nA -> e\\nB -> e"
                }
                """;

        mockMvc.perform(post("/api/grammar/lalr1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.deterministic", is(false)))
                .andExpect(jsonPath("$.conflicts", not(empty())))
                .andExpect(jsonPath("$.conflicts[0].type", is("Reduce / Reduce Conflict")));
    }

    @Test
    @DisplayName("POST /api/grammar/lalr1/parse parses valid input trace")
    void testLALR1ParseEndpoint() throws Exception {
        String requestBody = """
                {
                    "rawGrammar": "S -> C C\\nC -> c C | d",
                    "inputString": "c c d d"
                }
                """;

        mockMvc.perform(post("/api/grammar/lalr1/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(true)))
                .andExpect(jsonPath("$.steps", not(empty())))
                .andExpect(jsonPath("$.steps[-1].action", is("Accept")));
    }

    @Test
    @DisplayName("POST /api/grammar/simulate routes to SLR(1), CLR(1), and LALR(1)")
    void testSimulateDispatcherOtherParsers() throws Exception {
        String requestBodySLR = """
                {
                    "rawGrammar": "S -> L = R | R\\nL -> * R | id\\nR -> L",
                    "parserType": "SLR(1)",
                    "inputString": "id = id"
                }
                """;
        mockMvc.perform(post("/api/grammar/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodySLR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(true)));

        String requestBodyCLR = """
                {
                    "rawGrammar": "S -> C C\\nC -> c C | d",
                    "parserType": "CLR(1)",
                    "inputString": "c d d"
                }
                """;
        mockMvc.perform(post("/api/grammar/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyCLR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(true)));

        String requestBodyLALR = """
                {
                    "rawGrammar": "S -> C C\\nC -> c C | d",
                    "parserType": "LALR(1)",
                    "inputString": "d d"
                }
                """;
        mockMvc.perform(post("/api/grammar/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyLALR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(true)));
    }
}
