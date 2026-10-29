import React from 'react';

export default function GrammarSummaryView({ grammarSummary }) {
  if (!grammarSummary) return null;

  return (
    <div className="gl-tab-content">
      <div className="gl-section-header">
        <div>
          <h3 className="gl-section-title">Context-Free Grammar Formal Definition</h3>
          <p className="gl-section-desc">
            Tuple G = (V<sub>N</sub>, V<sub>T</sub>, P, S) representing non-terminals, terminals, production rules, and start symbol.
          </p>
        </div>
      </div>

      <div className="gl-summary-grid">
        <div className="gl-summary-card">
          <span className="gl-summary-label">Start Symbol (S)</span>
          <div className="gl-symbol-badge-large">{grammarSummary.startSymbol}</div>
        </div>

        <div className="gl-summary-card">
          <span className="gl-summary-label">Non-Terminals (V<sub>N</sub>) ({grammarSummary.nonTerminals.length})</span>
          <div className="gl-badges-wrap">
            {grammarSummary.nonTerminals.map((nt) => (
              <span key={nt} className="gl-badge gl-badge-purple">
                {nt}
              </span>
            ))}
          </div>
        </div>

        <div className="gl-summary-card">
          <span className="gl-summary-label">Terminals (V<sub>T</sub>) ({grammarSummary.terminals.length})</span>
          <div className="gl-badges-wrap">
            {grammarSummary.terminals.map((t) => (
              <span key={t} className="gl-badge gl-badge-cyan">
                {t}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div className="gl-two-col-grid" style={{ marginTop: '1.5rem' }}>
        {/* Original Productions */}
        <div className="gl-card gl-panel-inner">
          <div className="gl-inner-header">
            <h4 className="gl-inner-title">Numbered Productions P</h4>
            <span className="gl-count-tag">{grammarSummary.productions.length} rules</span>
          </div>
          <div className="gl-productions-list">
            {grammarSummary.productions.map((p) => (
              <div key={p.id} className="gl-production-row">
                <span className="gl-prod-index">({p.id})</span>
                <code className="gl-prod-text">{p.text}</code>
              </div>
            ))}
          </div>
        </div>

        {/* Augmented Grammar */}
        <div className="gl-card gl-panel-inner">
          <div className="gl-inner-header">
            <h4 className="gl-inner-title">Augmented Grammar G' (For LR Parsers)</h4>
            <span className="gl-count-tag">{grammarSummary.augmentedProductions.length} rules</span>
          </div>
          <div className="gl-productions-list">
            {grammarSummary.augmentedProductions.map((p) => (
              <div
                key={p.id}
                className={`gl-production-row ${p.id === 0 ? 'gl-augmented-root' : ''}`}
              >
                <span className="gl-prod-index">({p.id})</span>
                <code className="gl-prod-text">{p.text}</code>
                {p.id === 0 && <span className="gl-badge gl-badge-accent-sm">Root Augmentation</span>}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
