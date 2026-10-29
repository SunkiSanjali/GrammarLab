/**
 * GrammarLab REST API Client
 * Connects React frontend with the Spring Boot compiler analysis backend.
 */

const RAW_BASE = import.meta.env.VITE_API_BASE_URL || '';
const API_BASE = RAW_BASE ? `${RAW_BASE.replace(/\/+$/, '')}/api` : '/api';

export const grammarApi = {
  /**
   * Health check to ensure Spring Boot backend is reachable.
   */
  async checkHealth() {
    const res = await fetch(`${API_BASE}/health`);
    if (!res.ok) {
      throw new Error(`Backend responded with HTTP status ${res.status}`);
    }
    return await res.json();
  },

  /**
   * Validates grammar syntax and detects terminals/non-terminals.
   */
  async validateGrammar(rawGrammar) {
    const res = await fetch(`${API_BASE}/grammar/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to validate grammar');
    }
    return data;
  },

  /**
   * Computes FIRST and FOLLOW sets for the given CFG.
   */
  async computeFirstFollow(rawGrammar) {
    const res = await fetch(`${API_BASE}/grammar/first-follow`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to compute FIRST/FOLLOW sets');
    }
    return data;
  },

  /**
   * Computes LL(1) parsing table and checks for LL(1) conflicts.
   */
  async computeLL1(rawGrammar) {
    const res = await fetch(`${API_BASE}/grammar/ll1`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to generate LL(1) table');
    }
    return data;
  },

  /**
   * Computes LR(0) items, DFA transitions, and parsing table.
   */
  async computeLR0(rawGrammar) {
    const res = await fetch(`${API_BASE}/grammar/lr0`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to generate LR(0) table');
    }
    return data;
  },

  /**
   * Computes SLR(1) parsing table with FOLLOW-guided reductions.
   */
  async computeSLR1(rawGrammar) {
    const res = await fetch(`${API_BASE}/grammar/slr1`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to generate SLR(1) table');
    }
    return data;
  },

  /**
   * Computes CLR(1) / Canonical LR(1) items and parsing table.
   */
  async computeCLR1(rawGrammar) {
    const res = await fetch(`${API_BASE}/grammar/clr1`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to generate CLR(1) table');
    }
    return data;
  },

  /**
   * Computes LALR(1) merged items and parsing table.
   */
  async computeLALR1(rawGrammar) {
    const res = await fetch(`${API_BASE}/grammar/lalr1`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to generate LALR(1) table');
    }
    return data;
  },

  /**
   * Simulates LL(1) parsing stack trace on an input string.
   */
  async parseLL1(rawGrammar, startSymbol, inputString) {
    const res = await fetch(`${API_BASE}/grammar/ll1/parse`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar, startSymbol, inputString }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'LL(1) parsing simulation failed');
    }
    return data;
  },

  /**
   * Simulates LR(0) parsing stack trace on an input string.
   */
  async parseLR0(rawGrammar, startSymbol, inputString) {
    const res = await fetch(`${API_BASE}/grammar/lr0/parse`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar, startSymbol, inputString }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'LR(0) parsing simulation failed');
    }
    return data;
  },

  /**
   * Simulates SLR(1) parsing stack trace on an input string.
   */
  async parseSLR1(rawGrammar, startSymbol, inputString) {
    const res = await fetch(`${API_BASE}/grammar/slr1/parse`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar, startSymbol, inputString }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'SLR(1) parsing simulation failed');
    }
    return data;
  },

  /**
   * Simulates CLR(1) parsing stack trace on an input string.
   */
  async parseCLR1(rawGrammar, startSymbol, inputString) {
    const res = await fetch(`${API_BASE}/grammar/clr1/parse`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar, startSymbol, inputString }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'CLR(1) parsing simulation failed');
    }
    return data;
  },

  /**
   * Simulates LALR(1) parsing stack trace on an input string.
   */
  async parseLALR1(rawGrammar, startSymbol, inputString) {
    const res = await fetch(`${API_BASE}/grammar/lalr1/parse`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar, startSymbol, inputString }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'LALR(1) parsing simulation failed');
    }
    return data;
  },

  /**
   * Simulates parser step-by-step trace on an input string.
   */
  async simulateParse(rawGrammar, parserType, inputString) {
    if (parserType === 'LL(1)' || parserType === 'LL1') {
      return await this.parseLL1(rawGrammar, null, inputString);
    }
    if (parserType === 'LR(0)' || parserType === 'LR0') {
      return await this.parseLR0(rawGrammar, null, inputString);
    }
    if (parserType === 'SLR(1)' || parserType === 'SLR1') {
      return await this.parseSLR1(rawGrammar, null, inputString);
    }
    if (parserType === 'CLR(1)' || parserType === 'CLR1') {
      return await this.parseCLR1(rawGrammar, null, inputString);
    }
    if (parserType === 'LALR(1)' || parserType === 'LALR1') {
      return await this.parseLALR1(rawGrammar, null, inputString);
    }
    const res = await fetch(`${API_BASE}/grammar/simulate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rawGrammar, parserType, inputString }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Parsing simulation failed');
    }
    return data;
  }
};
