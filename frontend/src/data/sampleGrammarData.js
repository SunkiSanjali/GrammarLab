/**
 * Clearly identified sample data for GrammarLab layout development and offline testing.
 * All algorithm logic belongs in the Spring Boot backend.
 */

export const GRAMMAR_PRESETS = [
  {
    id: 'expr',
    name: 'Arithmetic Expressions (Classic CFG)',
    grammar: `E -> E + T | T\nT -> T * F | F\nF -> ( E ) | id`,
    sampleInput: 'id + id * id',
  },
  {
    id: 'll1_factored',
    name: 'LL(1) Predictive Expression Grammar',
    grammar: `E -> T E'\nE' -> + T E' | ε\nT -> F T'\nT' -> * F T' | ε\nF -> ( E ) | id`,
    sampleInput: 'id + id * id',
  },
  {
    id: 'slr_conflict',
    name: 'LR(0) vs SLR(1) Conflict Grammar',
    grammar: `S -> L = R | R\nL -> * R | id\nR -> L`,
    sampleInput: '* id = id',
  },
  {
    id: 'dangling_else',
    name: 'Dangling Else (Ambiguous)',
    grammar: `S -> if C then S else S | if C then S | other\nC -> cond`,
    sampleInput: 'if cond then if cond then other else other',
  },
  {
    id: 'clr_lalr',
    name: 'CLR(1) & LALR(1) Lookahead Grammar',
    grammar: `S -> C C\nC -> c C | d`,
    sampleInput: 'c d c d',
  },
];

export const SAMPLE_ANALYSIS_DATA = {
  grammarSummary: {
    startSymbol: 'E',
    nonTerminals: ['E', 'T', 'F'],
    terminals: ['+', '*', '(', ')', 'id', '$'],
    productions: [
      { id: 1, text: 'E -> E + T' },
      { id: 2, text: 'E -> T' },
      { id: 3, text: 'T -> T * F' },
      { id: 4, text: 'T -> F' },
      { id: 5, text: 'F -> ( E )' },
      { id: 6, text: 'F -> id' },
    ],
    augmentedProductions: [
      { id: 0, text: "E' -> E" },
      { id: 1, text: 'E -> E + T' },
      { id: 2, text: 'E -> T' },
      { id: 3, text: 'T -> T * F' },
      { id: 4, text: 'T -> F' },
      { id: 5, text: 'F -> ( E )' },
      { id: 6, text: 'F -> id' },
    ],
  },

  // FIRST & FOLLOW is computed dynamically by the Spring Boot backend
  firstFollow: null,

  // LL(1) table and conflicts are computed dynamically by the Spring Boot backend
  ll1: null,

  lr0: {
    hasConflicts: true,
    conflicts: [
      {
        type: 'Shift / Reduce Conflict',
        state: 'I1',
        terminal: '+',
        message: 'State I1 has both Shift on "+" and Reduce on production E\' -> E.',
      },
      {
        type: 'Shift / Reduce Conflict',
        state: 'I2',
        terminal: '*',
        message: 'State I2 has both Shift on "*" and Reduce on production E -> T.',
      },
    ],
    itemSets: [
      {
        id: 0,
        name: 'I0',
        items: [
          { rule: "E' -> • E", core: true },
          { rule: 'E -> • E + T', core: false },
          { rule: 'E -> • T', core: false },
          { rule: 'T -> • T * F', core: false },
          { rule: 'T -> • F', core: false },
          { rule: 'F -> • ( E )', core: false },
          { rule: 'F -> • id', core: false },
        ],
        gotos: [
          { symbol: 'E', target: 'I1' },
          { symbol: 'T', target: 'I2' },
          { symbol: 'F', target: 'I3' },
          { symbol: '(', target: 'I4' },
          { symbol: 'id', target: 'I5' },
        ],
      },
      {
        id: 1,
        name: 'I1',
        items: [
          { rule: "E' -> E •", core: true },
          { rule: 'E -> E • + T', core: true },
        ],
        gotos: [{ symbol: '+', target: 'I6' }],
      },
      {
        id: 2,
        name: 'I2',
        items: [
          { rule: 'E -> T •', core: true },
          { rule: 'T -> T • * F', core: true },
        ],
        gotos: [{ symbol: '*', target: 'I7' }],
      },
      {
        id: 3,
        name: 'I3',
        items: [{ rule: 'T -> F •', core: true }],
        gotos: [],
      },
      {
        id: 4,
        name: 'I4',
        items: [
          { rule: 'F -> ( • E )', core: true },
          { rule: 'E -> • E + T', core: false },
          { rule: 'E -> • T', core: false },
          { rule: 'T -> • T * F', core: false },
          { rule: 'T -> • F', core: false },
          { rule: 'F -> • ( E )', core: false },
          { rule: 'F -> • id', core: false },
        ],
        gotos: [
          { symbol: 'E', target: 'I8' },
          { symbol: 'T', target: 'I2' },
          { symbol: 'F', target: 'I3' },
          { symbol: '(', target: 'I4' },
          { symbol: 'id', target: 'I5' },
        ],
      },
      {
        id: 5,
        name: 'I5',
        items: [{ rule: 'F -> id •', core: true }],
        gotos: [],
      },
    ],
    actionHeaders: ['id', '+', '*', '(', ')', '$'],
    gotoHeaders: ['E', 'T', 'F'],
    tableRows: [
      { state: '0', actions: { id: 's5', '(': 's4' }, gotos: { E: '1', T: '2', F: '3' } },
      { state: '1', actions: { '+': 's6 / r0 (S/R)', $: 'acc / r0 (S/R)' }, gotos: {} },
      { state: '2', actions: { '+': 'r2', '*': 's7 / r2 (S/R)', ')': 'r2', $: 'r2' }, gotos: {} },
      { state: '3', actions: { '+': 'r4', '*': 'r4', ')': 'r4', $: 'r4' }, gotos: {} },
      { state: '4', actions: { id: 's5', '(': 's4' }, gotos: { E: '8', T: '2', F: '3' } },
      { state: '5', actions: { '+': 'r6', '*': 'r6', ')': 'r6', $: 'r6' }, gotos: {} },
    ],
  },

  slr1: {
    hasConflicts: false,
    conflicts: [],
    actionHeaders: ['id', '+', '*', '(', ')', '$'],
    gotoHeaders: ['E', 'T', 'F'],
    tableRows: [
      { state: '0', actions: { id: 's5', '(': 's4' }, gotos: { E: '1', T: '2', F: '3' } },
      { state: '1', actions: { '+': 's6', $: 'acc' }, gotos: {} },
      { state: '2', actions: { '+': 'r2', '*': 's7', ')': 'r2', $: 'r2' }, gotos: {} },
      { state: '3', actions: { '+': 'r4', '*': 'r4', ')': 'r4', $: 'r4' }, gotos: {} },
      { state: '4', actions: { id: 's5', '(': 's4' }, gotos: { E: '8', T: '2', F: '3' } },
      { state: '5', actions: { '+': 'r6', '*': 'r6', ')': 'r6', $: 'r6' }, gotos: {} },
    ],
  },

  clr1: {
    hasConflicts: false,
    itemSetsCount: 14,
    itemSets: [
      {
        id: 0,
        name: 'I0',
        items: [
          { rule: "S' -> • S", lookahead: '$' },
          { rule: 'S -> • C C', lookahead: '$' },
          { rule: 'C -> • c C', lookahead: 'c, d' },
          { rule: 'C -> • d', lookahead: 'c, d' },
        ],
        gotos: [
          { symbol: 'S', target: 'I1' },
          { symbol: 'C', target: 'I2' },
          { symbol: 'c', target: 'I3' },
          { symbol: 'd', target: 'I4' },
        ],
      },
      {
        id: 1,
        name: 'I1',
        items: [{ rule: "S' -> S •", lookahead: '$' }],
        gotos: [],
      },
      {
        id: 2,
        name: 'I2',
        items: [
          { rule: 'S -> C • C', lookahead: '$' },
          { rule: 'C -> • c C', lookahead: '$' },
          { rule: 'C -> • d', lookahead: '$' },
        ],
        gotos: [
          { symbol: 'C', target: 'I5' },
          { symbol: 'c', target: 'I6' },
          { symbol: 'd', target: 'I7' },
        ],
      },
    ],
    actionHeaders: ['c', 'd', '$'],
    gotoHeaders: ['S', 'C'],
    tableRows: [
      { state: '0', actions: { c: 's3', d: 's4' }, gotos: { S: '1', C: '2' } },
      { state: '1', actions: { $: 'acc' }, gotos: {} },
      { state: '2', actions: { c: 's6', d: 's7' }, gotos: { C: '5' } },
      { state: '3', actions: { c: 's3', d: 's4' }, gotos: { C: '8' } },
      { state: '4', actions: { c: 'r3', d: 'r3' }, gotos: {} },
    ],
  },

  lalr1: {
    hasConflicts: false,
    mergedStatesCount: 10,
    mergedPairs: [
      { mergedName: 'I36', from: ['I3', 'I6'], core: 'C -> c • C' },
      { mergedName: 'I47', from: ['I4', 'I7'], core: 'C -> d •' },
      { mergedName: 'I89', from: ['I8', 'I9'], core: 'C -> c C •' },
    ],
    actionHeaders: ['c', 'd', '$'],
    gotoHeaders: ['S', 'C'],
    tableRows: [
      { state: '0', actions: { c: 's36', d: 's47' }, gotos: { S: '1', C: '2' } },
      { state: '1', actions: { $: 'acc' }, gotos: {} },
      { state: '2', actions: { c: 's36', d: 's47' }, gotos: { C: '5' } },
      { state: '36', actions: { c: 's36', d: 's47' }, gotos: { C: '89' } },
      { state: '47', actions: { c: 'r3', d: 'r3', $: 'r3' }, gotos: {} },
      { state: '5', actions: { $: 'r1' }, gotos: {} },
    ],
  },

  stringTrace: [
    { step: 1, stateStack: '0', symbolStack: '$', inputBuffer: 'id + id * id $', action: 'Shift s5', rule: '' },
    { step: 2, stateStack: '0 5', symbolStack: '$ id', inputBuffer: '+ id * id $', action: 'Reduce by F -> id', rule: 'F -> id' },
    { step: 3, stateStack: '0 3', symbolStack: '$ F', inputBuffer: '+ id * id $', action: 'Reduce by T -> F', rule: 'T -> F' },
    { step: 4, stateStack: '0 2', symbolStack: '$ T', inputBuffer: '+ id * id $', action: 'Reduce by E -> T', rule: 'E -> T' },
    { step: 5, stateStack: '0 1', symbolStack: '$ E', inputBuffer: '+ id * id $', action: 'Shift s6', rule: '' },
    { step: 6, stateStack: '0 1 6', symbolStack: '$ E +', inputBuffer: 'id * id $', action: 'Shift s5', rule: '' },
    { step: 7, stateStack: '0 1 6 5', symbolStack: '$ E + id', inputBuffer: '* id $', action: 'Reduce by F -> id', rule: 'F -> id' },
    { step: 8, stateStack: '0 1 6 3', symbolStack: '$ E + F', inputBuffer: '* id $', action: 'Reduce by T -> F', rule: 'T -> F' },
    { step: 9, stateStack: '0 1 6 9', symbolStack: '$ E + T', inputBuffer: '* id $', action: 'Shift s7', rule: '' },
    { step: 10, stateStack: '0 1 6 9 7', symbolStack: '$ E + T *', inputBuffer: 'id $', action: 'Shift s5', rule: '' },
    { step: 11, stateStack: '0 1 6 9 7 5', symbolStack: '$ E + T * id', inputBuffer: '$', action: 'Reduce by F -> id', rule: 'F -> id' },
    { step: 12, stateStack: '0 1 6 9 7 10', symbolStack: '$ E + T * F', inputBuffer: '$', action: 'Reduce by T -> T * F', rule: 'T -> T * F' },
    { step: 13, stateStack: '0 1 6 9', symbolStack: '$ E + T', inputBuffer: '$', action: 'Reduce by E -> E + T', rule: 'E -> E + T' },
    { step: 14, stateStack: '0 1', symbolStack: '$ E', inputBuffer: '$', action: 'Accept', rule: "Accept (E' -> E)" },
  ],
};
