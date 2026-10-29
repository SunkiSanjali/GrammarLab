import React from 'react';

export default function FirstFollowView({ firstFollowData }) {
  if (!firstFollowData || !firstFollowData.items || firstFollowData.items.length === 0) {
    return (
      <div className="gl-tab-content">
        <div className="gl-section-header">
          <div>
            <h3 className="gl-section-title">FIRST and FOLLOW Sets</h3>
            <p className="gl-section-desc">
              Essential lookahead sets for predictive LL(1) and shift-reduce LR tables.
            </p>
          </div>
        </div>
        <div className="gl-empty-state">
          No FIRST &amp; FOLLOW sets computed yet. Click <strong>"Run Compiler Analysis"</strong> to execute the backend algorithm.
        </div>
      </div>
    );
  }

  return (
    <div className="gl-tab-content">
      <div className="gl-section-header">
        <div>
          <h3 className="gl-section-title">FIRST and FOLLOW Sets</h3>
          <p className="gl-section-desc">
            Essential lookahead sets for predictive LL(1) and shift-reduce LR tables.
          </p>
        </div>
      </div>

      <div className="gl-table-container">
        <table className="gl-data-table gl-matrix-table">
          <thead>
            <tr>
              <th style={{ width: '140px' }}>Non-Terminal</th>
              <th style={{ width: '130px' }}>Nullable (X ⇒* ε)</th>
              <th>FIRST(X) Set Notation</th>
              <th>FOLLOW(X) Set Notation</th>
            </tr>
          </thead>
          <tbody>
            {firstFollowData.items.map((row) => (
              <tr key={row.symbol}>
                <td>
                  <span className="gl-nt-badge">{row.symbol}</span>
                </td>
                <td>
                  {row.isNullable ? (
                    <span className="gl-badge gl-badge-warning">ε-Nullable</span>
                  ) : (
                    <span className="gl-badge gl-badge-neutral">Non-Nullable</span>
                  )}
                </td>
                <td>
                  <div className="gl-math-set-row">
                    <span className="gl-math-func">FIRST({row.symbol})</span>
                    <span className="gl-math-eq">=</span>
                    <span className="gl-set-display">
                      <span className="gl-set-bracket">&#123;</span>
                      {row.first.length === 0 ? (
                        <span className="gl-set-empty">∅</span>
                      ) : (
                        row.first.map((sym, idx) => (
                          <span
                            key={idx}
                            className={`gl-sym-tag ${sym === 'ε' ? 'gl-sym-epsilon' : 'gl-sym-terminal'}`}
                          >
                            {sym}
                          </span>
                        ))
                      )}
                      <span className="gl-set-bracket">&#125;</span>
                    </span>
                  </div>
                </td>
                <td>
                  <div className="gl-math-set-row">
                    <span className="gl-math-func">FOLLOW({row.symbol})</span>
                    <span className="gl-math-eq">=</span>
                    <span className="gl-set-display">
                      <span className="gl-set-bracket">&#123;</span>
                      {row.follow.length === 0 ? (
                        <span className="gl-set-empty">∅</span>
                      ) : (
                        row.follow.map((sym, idx) => (
                          <span
                            key={idx}
                            className={`gl-sym-tag ${sym === '$' ? 'gl-sym-eof' : 'gl-sym-terminal'}`}
                          >
                            {sym}
                          </span>
                        ))
                      )}
                      <span className="gl-set-bracket">&#125;</span>
                    </span>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mathematical Notation Card Summary */}
      <div className="gl-math-cards-grid" style={{ marginTop: '1.5rem' }}>
        {firstFollowData.items.map((row) => (
          <div key={row.symbol} className="gl-math-card">
            <div className="gl-math-card-header">
              <span className="gl-math-var">{row.symbol}</span>
              {row.isNullable && <span className="gl-badge gl-badge-warning-sm">Nullable</span>}
            </div>
            <div className="gl-math-card-body">
              <div className="gl-math-line">
                <span className="gl-math-label">FIRST:</span>
                <code>&#123; {row.first.join(', ') || '∅'} &#125;</code>
              </div>
              <div className="gl-math-line">
                <span className="gl-math-label">FOLLOW:</span>
                <code>&#123; {row.follow.join(', ') || '∅'} &#125;</code>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Educational Callout */}
      <div className="gl-callout-box" style={{ marginTop: '1.5rem' }}>
        <h4 className="gl-callout-title">Compiler Theory Notes</h4>
        <ul className="gl-callout-list">
          <li>
            <strong>FIRST(α):</strong> The set of terminals that can appear as the first symbol of a string derived from α. If α derives ε, then ε ∈ FIRST(α).
          </li>
          <li>
            <strong>FOLLOW(A):</strong> The set of terminals that can appear immediately to the right of non-terminal A in some sentential form. Always includes $ for the start symbol.
          </li>
        </ul>
      </div>
    </div>
  );
}
