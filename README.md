# GrammarLab — An Interactive Compiler Design Workbench

A full-stack, pedagogical workbench for grammar analysis, parsing table generation, conflict diagnosis, state automata visualization, and step-by-step pushdown automaton simulation across **LL(1)**, **LR(0)**, **SLR(1)**, **CLR(1)**, and **LALR(1)** parsing techniques.

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Problem Statement & Objectives](#2-problem-statement--objectives)
3. [Key Features](#3-key-features)
4. [Architecture & Technology Stack](#4-architecture--technology-stack)
5. [Prerequisites & Installation](#5-prerequisites--installation)
6. [Windows Commands (Run & Test)](#6-windows-commands-run--test)
7. [REST API Reference](#7-rest-api-reference)
8. [Textbook Example Grammars](#8-textbook-example-grammars)
9. [Comprehensive Test Suite](#9-comprehensive-test-suite)
10. [Known Limitations & Resource Safeguards](#10-known-limitations--resource-safeguards)
11. [Compiler Design Laboratory Report Outline](#11-compiler-design-laboratory-report-outline)
12. [Comprehensive Viva Examination Guide (30 Questions & Answers)](#12-comprehensive-viva-examination-guide-30-questions--answers)

---

## 1. Project Overview

**GrammarLab** is an interactive compiler design workbench created for computer science students and educators. Rather than relying on canned responses, pre-computed tables, or trivial hardcoded samples, GrammarLab calculates all compiler theory structures dynamically from **any arbitrary context-free grammar (CFG)** supplied by the user.

GrammarLab implements:
- Context-free grammar validation, symbol tokenization, and epsilon (`ε`) handling.
- Deterministic, fixed-point computation of **FIRST** and **FOLLOW** sets.
- **LL(1)** top-down predictive parsing table generation with FIRST/FIRST and FIRST/FOLLOW conflict diagnosis.
- **LR(0)** canonical collection construction ($CLOSURE$, $GOTO$), shift/reduce and reduce/reduce conflict detection.
- **SLR(1)** parsing with reductions strictly placed on $FOLLOW$ sets.
- **CLR(1)** (Canonical LR(1)) with full lookahead propagation $([A \to \alpha \cdot B \beta, a])$ and $FIRST(\beta a)$ lookahead inheritance.
- **LALR(1)** (Lookahead LR) with genuine core identification, lookahead unioning, merged state re-linking, and merge-induced conflict detection.
- Step-by-step pushdown automaton simulation with full stack traces, remaining input tape, and production reduction actions.

---

## 2. Problem Statement & Objectives

### Problem Statement
Compiler design textbooks (Aho, Lam, Sethi, Ullman — the "Dragon Book") teach parser construction using mathematical definitions that are difficult to visualize and trace by hand. Students frequently struggle with:
1. Determining lookahead inheritance in CLR(1) item sets.
2. Understanding why LALR(1) merges states with identical LR(0) cores and how that can produce reduce/reduce conflicts.
3. Differentiating why SLR(1) fails on grammars like $S \to L = R \mid R$ while CLR(1) succeeds.
4. Tracing shift/reduce stack actions on actual token sequences.

Existing web tools either support only one parser type, utilize pre-canned examples, or fail on multi-character symbols and epsilon productions.

### Objectives
1. Provide a **unified platform** for comparing LL(1), LR(0), SLR(1), CLR(1), and LALR(1) on the exact same grammar.
2. Support **general context-free grammars**, including multi-character tokens (`id`, `num`, `while`), epsilon productions (`ε`), left recursion, right recursion, and nullable chains.
3. Detect, isolate, and clearly explain all **parsing conflicts** (FIRST/FIRST, FIRST/FOLLOW, Shift/Reduce, Reduce/Reduce).
4. Provide interactive **state automata graphs** and **step-by-step stack animation**.
5. Back every algorithm with verified, automated test suites (100+ tests) with zero mock data.

---

## 3. Key Features

- **Dynamic Grammar Parser**: Supports multi-line input, multiple alternatives (`|`), multi-character non-terminals (`Expr`), terminals (`id`, `+`, `*`), and epsilon (`ε`).
- **Start Symbol Selection**: Defaults to the first LHS non-terminal, with validation of custom start symbols.
- **FIRST & FOLLOW Engine**: Fixed-point iterative algorithm correctly handling nullable chains and epsilon propagation; start symbol automatically includes `$`.
- **LL(1) Predictive Parser**: Complete table construction, conflict reporting, and predictive string tracing.
- **LR(0) Parser**: Canonical collection, ACTION (shift, reduce, accept, error), GOTO, state transitions.
- **SLR(1) Parser**: Restricts reduce actions to $FOLLOW(A)$, resolving many LR(0) shift/reduce conflicts.
- **CLR(1) Parser**: Exact $[A \to \alpha \cdot \beta, a]$ items with $FIRST(\beta a)$ lookahead computation during closure.
- **LALR(1) Parser**: State merging based on identical LR(0) cores, unioning lookahead sets, rebuilds transitions, and detects merge-induced reduce/reduce conflicts.
- **Automaton Visualization**: Interactive state transition diagram displaying all kernel/closure items and transition arcs.
- **String Simulator**: Interactive pushdown automaton execution featuring:
  - Play / Pause / Step Forward / Step Backward / Reset controls.
  - Stack inspection (state numbers and grammar symbols).
  - Input tape remaining.
  - Reduction rule name and formula.
  - Clear Accept or Syntax Error diagnosis.

---

## 4. Architecture & Technology Stack

```
┌────────────────────────────────────────────────────────┐
│             React 19 + Vite Frontend (UI)              │
│  - GrammarEditor: Input & validation feedback          │
│  - TabNav: 8 interactive workbench sections            │
│  - ParsingTableView: Matrix display of ACTION & GOTO   │
│  - ItemSetCardsView: Canonical collection display      │
│  - AutomatonGraphView: SVG state transition diagrams   │
│  - StringParserView: Step-by-step simulation engine    │
└───────────────────────────┬────────────────────────────┘
                            │ REST / JSON (HTTP)
                            ▼
┌────────────────────────────────────────────────────────┐
│           Java 21 + Spring Boot 3.3.4 (Engine)         │
│  - GrammarController: REST endpoints                   │
│  - GrammarParser & GrammarValidator: AST & tokens      │
│  - FirstFollowService: Fixed-point FIRST/FOLLOW engine │
│  - LL1Service: Predictive table & trace                │
│  - LRService: LR(0), SLR(1), CLR(1), LALR(1) algorithms│
└────────────────────────────────────────────────────────┘
```

### Technology Stack
- **Backend**:
  - Java 21 (LTS)
  - Spring Boot 3.3.4
  - Apache Maven 3.9+
  - JUnit 5 & MockMvc (103 automated tests)
- **Frontend**:
  - React 19
  - Vite 8
  - Vanilla CSS (custom design tokens, glassmorphism, responsive layout)
  - Native SVG for state automata

---

## 5. Prerequisites & Installation

### Prerequisites
- **Java Development Kit (JDK)**: Version 21 or higher installed and on `PATH`.
  - Verify with: `java -version`
- **Apache Maven**: Version 3.8+ on `PATH` (or use Maven wrapper if installed).
  - Verify with: `mvn -version`
- **Node.js**: Version 18+ or 20+ (LTS) and `npm`.
  - Verify with: `node -v` and `npm -v`

### Clone & Directory Structure
```
GrammarLab/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/grammarlab/
│       │   ├── config/CorsConfig.java
│       │   ├── controller/GrammarController.java
│       │   ├── dto/...
│       │   ├── model/...
│       │   └── service/
│       │       ├── GrammarParser.java
│       │       ├── GrammarValidator.java
│       │       ├── GrammarService.java
│       │       ├── FirstFollowService.java
│       │       ├── LL1Service.java
│       │       └── LRService.java
│       └── test/java/com/grammarlab/... (12 test suites, 103 tests)
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/grammarApi.js
│       ├── components/...
│       ├── App.jsx
│       └── index.css
└── README.md
```

---

## 6. Windows Commands (Run & Test)

Open two PowerShell or Command Prompt terminals in the `GrammarLab` directory:

### Terminal 1: Start Backend (Spring Boot)
```powershell
cd backend
mvn spring-boot:run
```
*The backend starts at `http://localhost:8080`.*
*Health check:* `http://localhost:8080/api/grammar/health`

### Terminal 2: Start Frontend (Vite Dev Server)
```powershell
cd frontend
npm install
npm run dev
```
*The frontend opens at `http://localhost:5173`.*

### Run All Backend Automated Tests
```powershell
cd backend
mvn test
```
*Output: Tests run: 103, Failures: 0, Errors: 0, Skipped: 0.*

### Build Production JAR
```powershell
cd backend
mvn package -DskipTests
```
*Output: `target/grammarlab-backend-1.0.0-SNAPSHOT.jar`.*

### Build Production Frontend Bundle
```powershell
cd frontend
npm run build
```
*Output: Production assets generated in `frontend/dist/` in ~1.1s.*

---

## 7. REST API Reference

All endpoints accept and return `application/json`. CORS is enabled for `http://localhost:5173`.

### 1. Grammar Validation
- **POST** `/api/grammar/validate`
  - **Body**: `{"rawGrammar": "E -> E + T | T\nT -> id", "startSymbol": "E"}`
  - **Response**: Parsed symbols, terminals, non-terminals, production list, augmented productions, validity boolean, error messages.

### 2. FIRST & FOLLOW
- **POST** `/api/grammar/first-follow`
  - **Body**: `{"rawGrammar": "E -> T E'\nE' -> + T E' | ε\nT -> id"}`
  - **Response**: `firstSets`, `followSets`, `items` (symbol, nullable flag, first list, follow list).

### 3. LL(1) Predictive Parsing
- **POST** `/api/grammar/ll1/table`
  - **Response**: `tableRows`, `headers`, `isLL1`, `conflicts` (FIRST/FIRST, FIRST/FOLLOW).
- **POST** `/api/grammar/ll1/parse`
  - **Body**: `{"rawGrammar": "...", "inputString": "id + id"}`
  - **Response**: `accepted`, `message`, `steps` (stepNum, stack, remainingInput, action).

### 4. LR(0) Parser
- **POST** `/api/grammar/lr0`
  - **Response**: Canonical item sets, `actionHeaders`, `gotoHeaders`, `tableRows`, `conflicts` (Shift/Reduce, Reduce/Reduce).
- **POST** `/api/grammar/lr0/parse`
  - **Response**: Step-by-step trace using the LR(0) parsing table.

### 5. SLR(1) Parser
- **POST** `/api/grammar/slr1`
  - **Response**: Item sets, FOLLOW-restricted ACTION/GOTO table, conflict diagnosis.
- **POST** `/api/grammar/slr1/parse`
  - **Response**: Simulation trace using the SLR(1) table.

### 6. CLR(1) Canonical Parser
- **POST** `/api/grammar/clr1`
  - **Response**: Canonical LR(1) item sets with lookaheads $[A \to \alpha \cdot \beta, a]$, ACTION/GOTO table, conflict list.
- **POST** `/api/grammar/clr1/parse`
  - **Response**: Simulation trace using the CLR(1) table.

### 7. LALR(1) Lookahead Parser
- **POST** `/api/grammar/lalr1`
  - **Response**: Merged states (collapsed identical LR(0) cores), `mergedPairs` metadata, LALR(1) ACTION/GOTO table, merge-induced conflicts.
- **POST** `/api/grammar/lalr1/parse`
  - **Response**: Simulation trace using the LALR(1) table.

### 8. Universal Simulation Dispatcher
- **POST** `/api/grammar/simulate`
  - **Body**: `{"rawGrammar": "...", "parserType": "CLR(1)", "inputString": "c d d"}`
  - **Response**: Dispatches to the selected parsing engine and returns unified stack trace and acceptance status.

---

## 8. Textbook Example Grammars

### Grammar 1: Classic Arithmetic Expressions (SLR(1), CLR(1), LALR(1))
```
E -> E + T | T
T -> T * F | F
F -> ( E ) | id
```
- **Classification**: Left-recursive (not LL(1)). Has LR(0) shift-reduce conflicts. Fully deterministic in SLR(1), CLR(1), and LALR(1).
- **Sample input**: `id + id * id` (Accepted).

### Grammar 2: LL(1) Transformed Expression Grammar
```
E -> T E'
E' -> + T E' | ε
T -> F T'
T' -> * F T' | ε
F -> ( E ) | id
```
- **Classification**: LL(1) deterministic (0 conflicts).
- **Sample input**: `( id + id ) * id` (Accepted).

### Grammar 3: LR(0) Conflict Resolved by SLR(1)
```
S -> a A | a
A -> b
```
- **Analysis**:
  - In LR(0), state after reading `a` has $S \to a \cdot A$, $S \to a \cdot$, and $A \to \cdot b$. Shift on `b` conflicts with reduce on `b`.
  - In SLR(1), $FOLLOW(S) = \{\$\}$. Reduction is placed only on `$`. Shift is on `b`. Conflict is resolved!

### Grammar 4: CLR(1) vs. SLR(1) Benchmark (Dragon Book Assignment Grammar)
```
S -> L = R | R
L -> * R | id
R -> L
```
- **Analysis**:
  - In SLR(1), state with $S \to L \cdot = R$ and $R \to L \cdot$ has a Shift/Reduce conflict on `=` because $= \in FOLLOW(R)$.
  - In CLR(1), lookahead for $R \to L \cdot$ is only `{$}`. Shift is on `=`. No conflict! CLR(1) succeeds where SLR(1) fails.
- **Sample input**: `* id = id` (Accepted).

### Grammar 5: LR(1) but NOT LALR(1) (Merge-Induced Reduce/Reduce Conflict)
```
S -> a A c | b B c | a B d | b A d
A -> e
B -> e
```
- **Analysis**:
  - In CLR(1), states containing $A \to e \cdot, c$ and $B \to e \cdot, d$ have disjoint lookaheads. 0 conflicts!
  - In LALR(1), states with the same core ($A \to e \cdot$, $B \to e \cdot$) are merged. Lookaheads union to $\{c, d\}$. In the merged state, on lookahead `c`, the parser cannot decide whether to reduce via $A \to e$ or $B \to e$.
  - Result: **Reduce / Reduce Conflict in LALR(1)**.

---

## 9. Comprehensive Test Suite

All 103 automated tests are verified passing:

| Test Class | Focus | Test Count | Status |
|---|---|---|---|
| `GrammarParserTest` | CFG tokenization, multi-char identifiers, epsilons | 6 | Passed |
| `GrammarValidatorTest` | Undefined non-terminals, cycle detection, format errors | 12 | Passed |
| `FirstFollowServiceTest` | FIRST, FOLLOW, epsilon propagation, fixed-point loops | 5 | Passed |
| `LL1ServiceTest` | LL(1) table, predictive stack trace, conflicts | 5 | Passed |
| `LRServiceTest` | LR(0) closure, goto, item sets, table, conflicts | 12 | Passed |
| `Prompt5VerificationTest` | Prompt 5 baseline verification | 16 | Passed |
| `Prompt6VerificationTest` | LR(0) comprehensive verification | 12 | Passed |
| `SLR1ServiceTest` | SLR(1) FOLLOW-constrained tables, conflict resolution | 5 | Passed |
| `CLR1ServiceTest` | CLR(1) lookaheads, canonical collection, table, trace | 6 | Passed |
| `LALR1ServiceTest` | LALR(1) state merging, lookahead unioning, RR conflict | 4 | Passed |
| `GrammarControllerTest` | MockMvc integration tests for all 8 REST endpoints | 19 | Passed |
| `GrammarLabApplicationTests`| Spring Boot context loader | 1 | Passed |
| **Total** | | **103** | **100% Passed** |

---

## 10. Known Limitations & Resource Safeguards

1. **Cycle Safeguards**: Grammars with unproductive cycles or infinite left-recursion loops are capped at a maximum of 500 parsing trace steps to prevent denial-of-service or infinite loops.
2. **Deterministic Fallback in Ambiguous Parsers**: If a user runs simulation on a grammar with unresolved conflicts, the simulator deterministically prioritizes `accept > shift > first reduce` to complete the trace while prominently alerting the user of the table conflicts.
3. **Symbol Separation**: Whitespace tokenization (`id + id`) is recommended for unambiguous multi-character token recognition. Single-character terminals can also be tokenized smoothly.
4. **Reserved Symbols**: `$` is reserved for end-of-input; `ε` (or empty alternative) is reserved for epsilon.

---

## 11. Compiler Design Laboratory Report Outline

### 1. Abstract
Comprehensive study and implementation of top-down and bottom-up parsing techniques in compiler design. Design of a full-stack interactive workbench supporting LL(1), LR(0), SLR(1), CLR(1), and LALR(1).

### 2. Introduction & Literature Survey
- Role of the syntax analyzer in the compiler pipeline.
- Trade-offs between top-down (LL) and bottom-up (LR) parsers.
- Hierarchy of grammar classes: $LL(1) \subset SLR(1) \subset LALR(1) \subset CLR(1) \subset LR(k)$.

### 3. Problem Definition & Objectives
- Challenges in understanding and debugging shift/reduce conflicts and lookahead sets.
- Development of a software workbench for automated computation and visualization.

### 4. Mathematical Formulations & Algorithms
- **FIRST & FOLLOW**: Fixed-point iterative definition.
- **LL(1)**: Selection condition: $\forall A \to \alpha \mid \beta$, $FIRST(\alpha) \cap FIRST(\beta) = \emptyset$ and if nullable, $FIRST(\alpha) \cap FOLLOW(A) = \emptyset$.
- **LR(0)**: $CLOSURE(I)$ and $GOTO(I, X)$ operations on items $[A \to \alpha \cdot \beta]$.
- **SLR(1)**: Placement of $reduce(A \to \alpha)$ in $ACTION[i, a]$ if and only if $a \in FOLLOW(A)$.
- **CLR(1)**: Items $[A \to \alpha \cdot \beta, a]$; lookahead inheritance from $FIRST(\beta a)$.
- **LALR(1)**: Equivalence relation on LR(1) states based on LR(0) kernel equality; unioning lookahead sets.

### 5. System Architecture & Design
- Layered MVC architecture (React Client, Spring Boot REST Engine).
- Object-oriented grammar model: `Grammar`, `Production`, `GrammarSymbol`, `LR0Item`, `LR1Item`.

### 6. Implementation Details
- Algorithm implementation highlights (`FirstFollowService`, `LL1Service`, `LRService`).
- State automaton graph generation and step-by-step trace engine.

### 7. Experimental Results & Test Cases
- Demonstration on LL(1), SLR(1), CLR(1), and LALR(1) benchmark grammars.
- Verification of merge-induced Reduce/Reduce conflict on $S \to a A c \mid b B c \mid a B d \mid b A d$.
- Test results summary (103/103 tests passed).

### 8. Conclusion & Future Enhancements
- Summary of workbench capabilities.
- Future work: Abstract Syntax Tree (AST) visualization, three-address code generation, syntax-directed translation (SDT) schemes.

### 9. References
- Aho, A. V., Lam, M. S., Sethi, R., & Ullman, J. D. (2006). *Compilers: Principles, Techniques, and Tools* (2nd ed.). Addison-Wesley.
- Cooper, K. D., & Torczon, L. (2011). *Engineering a Compiler* (2nd ed.). Morgan Kaufmann.
- Grune, D., & Jacobs, C. J. (2008). *Parsing Techniques: A Practical Guide*. Springer.

---

## 12. Comprehensive Viva Examination Guide (30 Questions & Answers)

### Q1. What is the fundamental difference between top-down and bottom-up parsing?
**Answer**: Top-down parsers (e.g., LL(1)) construct the parse tree starting from the root (start symbol) down to the leaves (input tokens) by performing leftmost derivations. Bottom-up parsers (e.g., LR) construct the tree starting from the leaves up to the root by identifying "handles" and applying reductions, corresponding to the reverse of a rightmost derivation.

### Q2. What do the letters in LL(1) and LR(1) stand for?
**Answer**:
- **LL(1)**: First **L** = Left-to-right scanning of input; Second **L** = Leftmost derivation; **(1)** = 1 lookahead token.
- **LR(1)**: **L** = Left-to-right scanning; **R** = Rightmost derivation in reverse; **(1)** = 1 lookahead token.

### Q3. How is the FIRST set of a grammar symbol defined?
**Answer**: For any grammar symbol $X$, $FIRST(X)$ is the set of terminals that begin strings derived from $X$. If $X \Rightarrow^* \varepsilon$, then $\varepsilon \in FIRST(X)$.

### Q4. How is the FOLLOW set of a non-terminal defined, and why does FOLLOW of the start symbol contain `$ `?
**Answer**: For a non-terminal $A$, $FOLLOW(A)$ is the set of terminals that can appear immediately to the right of $A$ in some sentential form ($S \Rightarrow^* \alpha A a \beta$). The start symbol $S$ can be followed by the end of the input string, represented by `$`.

### Q5. What are the two necessary and sufficient conditions for a grammar to be LL(1)?
**Answer**: For every non-terminal with multiple productions $A \to \alpha_1 \mid \alpha_2 \mid \dots \mid \alpha_n$:
1. $FIRST(\alpha_i) \cap FIRST(\alpha_j) = \emptyset$ for all $i \neq j$ (no FIRST/FIRST conflict).
2. If any $\alpha_i \Rightarrow^* \varepsilon$, then $FIRST(\alpha_j) \cap FOLLOW(A) = \emptyset$ for all $j \neq i$ (no FIRST/FOLLOW conflict).

### Q6. Can an LL(1) parser handle left-recursive grammars? Why or why not?
**Answer**: No. An immediate left-recursive production $A \to A \alpha \mid \beta$ causes $FIRST(A \alpha) \supseteq FIRST(\beta)$, leading to an immediate FIRST/FIRST conflict in the table cell $M[A, b]$ for any $b \in FIRST(\beta)$. Left recursion must be eliminated prior to LL(1) parsing.

### Q7. What is an LR(0) item?
**Answer**: An LR(0) item is a production with a dot $(\cdot)$ placed somewhere in its right-hand side, such as $[A \to \alpha \cdot \beta]$. The dot indicates how much of the production has already been seen (the prefix $\alpha$) and what we expect to see next (the suffix $\beta$).

### Q8. What does the CLOSURE operation compute in LR parser construction?
**Answer**: Given a set of items $I$, $CLOSURE(I)$ repeatedly adds $[B \to \cdot \gamma]$ for every item $[A \to \alpha \cdot B \beta]$ in $I$ and every production $B \to \gamma$, until no new items can be added. It represents all productions that may be recognized next given the current state.

### Q9. What does the GOTO operation compute?
**Answer**: $GOTO(I, X)$ computes the closure of all items obtained by advancing the dot over symbol $X$:
$$GOTO(I, X) = CLOSURE(\{[A \to \alpha X \cdot \beta] \mid [A \to \alpha \cdot X \beta] \in I\})$$
It represents the state transition from state $I$ upon observing symbol $X$.

### Q10. What are the two types of parsing conflicts in LR(0)?
**Answer**:
1. **Shift/Reduce conflict**: A state contains both a shift item $[A \to \alpha \cdot a \beta]$ and a reduce item $[B \to \gamma \cdot]$.
2. **Reduce/Reduce conflict**: A state contains two distinct completed reduce items $[A \to \alpha \cdot]$ and $[B \to \beta \cdot]$.

### Q11. How does SLR(1) improve upon LR(0)?
**Answer**: In LR(0), whenever an item $[A \to \alpha \cdot]$ is complete, a reduce action $r_k$ is placed across **all** terminal columns in the ACTION table. In SLR(1), the reduction $r_k$ is placed **only** under terminals $a \in FOLLOW(A)$. This resolves any shift/reduce conflict where the shift terminal is not in $FOLLOW(A)$.

### Q12. Give an example of a grammar that is NOT SLR(1) but is CLR(1).
**Answer**: The classic assignment grammar:
$$S \to L = R \mid R, \quad L \to * R \mid id, \quad R \to L$$
In state $I_2$ containing $\{S \to L \cdot = R, R \to L \cdot\}$, $= \in FOLLOW(R)$ because $S \Rightarrow L = R \Rightarrow R = R \Rightarrow \dots$ so $=$ appears after $L$, and $R \to L$ puts $FOLLOW(L) \subseteq FOLLOW(R)$. Thus SLR(1) places both a shift on `=` and a reduce on `=`, causing a shift/reduce conflict.

### Q13. How does CLR(1) resolve the conflict in $S \to L = R \mid R$?
**Answer**: CLR(1) items carry specific lookaheads: $[A \to \alpha \cdot \beta, a]$. For item $[R \to L \cdot, \$]$, the lookahead is strictly `{$}` because $R$ only appears at the end of the input in $S \to R$. Therefore, reduction occurs only on `$`, and shift occurs on `=`, completely resolving the conflict.

### Q14. What is a CLR(1) item and how is its lookahead determined during CLOSURE?
**Answer**: A CLR(1) item is a pair $[A \to \alpha \cdot B \beta, a]$ where $a$ is a lookahead terminal. When taking closure on non-terminal $B$, we add items $[B \to \cdot \gamma, b]$ where $b \in FIRST(\beta a)$. The lookahead is inherited from what can legally follow $B$ in this specific syntactic context.

### Q15. Why does CLR(1) produce so many states compared to LR(0)?
**Answer**: In CLR(1), two states that have identical productions and dot positions (the same LR(0) "core") are treated as completely different states if their lookahead sets differ. A grammar with $n$ LR(0) states can have hundreds or thousands of CLR(1) states.

### Q16. How does LALR(1) solve the state explosion problem of CLR(1)?
**Answer**: LALR(1) identifies all CLR(1) states that share identical LR(0) cores (ignoring lookaheads) and merges them into a single state. The lookahead sets for corresponding items are unioned. The number of states in LALR(1) is always identical to the number of states in LR(0).

### Q17. Can state merging in LALR(1) introduce Shift/Reduce conflicts? Why or why not?
**Answer**: **No**. State merging cannot introduce shift/reduce conflicts. The shift actions depend solely on the LR(0) core ($[A \to \alpha \cdot a \beta]$). Since states with the same core already had the same shifts, unioning lookaheads only modifies the terminal columns of reduce actions, which were already disjoint from shift targets.

### Q18. Can state merging in LALR(1) introduce Reduce/Reduce conflicts? Explain with an example.
**Answer**: **Yes**. When two states with the same core but different lookaheads are merged, their reduce lookaheads are unioned. If the merged lookaheads overlap for two different reduction rules, a reduce/reduce conflict occurs. Example:
$$S \to a A c \mid b B c \mid a B d \mid b A d, \quad A \to e, \quad B \to e$$
In CLR(1), state 1 has $[A \to e \cdot, c]$ and $[B \to e \cdot, d]$, while state 2 has $[A \to e \cdot, d]$ and $[B \to e \cdot, c]$. When merged in LALR(1), both $A \to e$ and $B \to e$ have lookahead $\{c, d\}$, creating a Reduce/Reduce conflict on both $c$ and $d$.

### Q19. What is an augmented grammar and why is it required in LR parsing?
**Answer**: An augmented grammar adds a fresh start symbol $S'$ and a production $S' \to S$. It ensures that the parser has a unique, unambiguous acceptance condition: the parser accepts when it is ready to reduce $S' \to S$, which occurs if and only if the entire input has been successfully reduced to the original start symbol $S$.

### Q20. What is a "handle" in bottom-up parsing?
**Answer**: A handle of a right-sentential form $\gamma = \alpha \beta w$ is a substring $\beta$ matching the right-hand side of a production $A \to \beta$, such that replacing $\beta$ with $A$ represents the reverse of the last step of the rightmost derivation leading to $\gamma$.

### Q21. What are the four actions performed by an LR parser?
**Answer**:
1. **Shift**: Push the next input terminal and the target state onto the stack; advance input pointer.
2. **Reduce**: Pop $2 \times |\beta|$ symbols (or $|\beta|$ states) corresponding to production $A \to \beta$; push non-terminal $A$ and the new state given by $GOTO[\text{top}, A]$.
3. **Accept**: Parsing succeeds; input is syntactically valid.
4. **Error**: No valid action entry exists; syntax error detected.

### Q22. How does an LR parser handle epsilon ($\varepsilon$) productions during reduction?
**Answer**: Since the right-hand side of an epsilon production $A \to \varepsilon$ has length 0 ($|\beta| = 0$), the parser pops **0** symbols from the stack. It then looks at the exposed top state and transitions to $GOTO[\text{exposed state}, A]$.

### Q23. What is the power relationship between the parsing classes?
**Answer**:
$$LL(1) \subset SLR(1) \subset LALR(1) \subset CLR(1) \subset LR(k)$$
Every LL(1) grammar is an SLR(1) grammar (if transformed to avoid left recursion). Every SLR(1) grammar is LALR(1). Every LALR(1) grammar is CLR(1).

### Q24. What happens if an ambiguous grammar is fed into an LR parser generator?
**Answer**: The parser generator will inevitably encounter conflicts (Shift/Reduce or Reduce/Reduce) in the parsing table because an ambiguous grammar has more than one parse tree for at least one string, meaning multiple valid actions exist for a single state and lookahead.

### Q25. Can any parsing method parse an ambiguous grammar deterministically without disambiguation rules?
**Answer**: No. By definition, deterministic pushdown automata can only recognize unambiguous context-free languages. Ambiguous grammars require precedence/associativity declarations (e.g., in Yacc/Bison) to resolve table conflicts deterministically.

### Q26. Why do practical parser generators (like Yacc/Bison) use LALR(1) rather than CLR(1)?
**Answer**: Because LALR(1) tables are orders of magnitude smaller (typically hundreds of states rather than tens of thousands of states for a language like C or Java), saving critical memory and cache footprint while parsing almost all real-world programming language constructs without conflicts.

### Q27. In your GrammarLab implementation, how do you verify state equality during LR(0) canonical collection construction?
**Answer**: Each state is represented by an `ItemSet` containing a `LinkedHashSet<LR0Item>`. Two states are equal if and only if their sets of items are identical (matching production ID and dot position). A hash map of `seen` item sets ensures every unique state is assigned a deterministic ID ($I_0, I_1, \dots$).

### Q28. In your GrammarLab implementation, how did you achieve genuine LALR(1) core merging?
**Answer**: We extract the LR(0) core signature (`prodId@dot`) from every CLR(1) state using a `.distinct().sorted()` stream. States sharing the exact same signature are grouped together. Lookaheads for identical core items are unioned into a single `LinkedHashSet<GrammarSymbol>`, GOTO targets are re-mapped to merged state IDs, and the table is scanned for any new reduce/reduce conflicts.

### Q29. How does GrammarLab prevent infinite parsing loops on cyclic grammars?
**Answer**: In `LRService.java`, the simulation loop enforces a strict `MAX_STEPS = 500` threshold. If the stack transitions exceed 500 steps without reducing to the start symbol or detecting an error, the parser terminates safely and reports: `"Parse exceeded 500 steps — possible grammar cycle."`

### Q30. Why is FOLLOW(S) not sufficient to prevent conflicts in $S \to L = R \mid R$, necessitating CLR(1)?
**Answer**: Because FOLLOW sets are global across the entire grammar. In $S \to L = R \mid R$, '=' is in $FOLLOW(R)$ globally, even though in the specific context where $R$ was derived at the top level ($S \to R$), '=' can never appear. CLR(1) solves this by maintaining context-sensitive lookaheads local to the derivation path rather than global approximations.

---

## Conclusion & Laboratory Submission

**GrammarLab** has been verified end-to-end:
- **Algorithms**: Tested across LL(1), LR(0), SLR(1), CLR(1), and LALR(1).
- **Backend Tests**: 103/103 tests passing.
- **Frontend Build**: Production build passing with 0 errors.
- **Documentation**: Complete lab outline and 30 viva questions.
