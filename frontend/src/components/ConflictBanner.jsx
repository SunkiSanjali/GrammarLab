import React from 'react';

export default function ConflictBanner({ conflicts }) {
  if (!conflicts || conflicts.length === 0) {
    return (
      <div className="gl-alert gl-alert-success">
        <span className="gl-alert-icon">✓</span>
        <div>
          <strong>No Conflicts Detected:</strong> The parsing table is deterministic and unambiguous for this parser class.
        </div>
      </div>
    );
  }

  return (
    <div className="gl-conflicts-container">
      <div className="gl-alert gl-alert-warning" style={{ marginBottom: '1rem' }}>
        <span className="gl-alert-icon">⚠️</span>
        <div>
          <strong>Grammar Conflict Warning:</strong> Found {conflicts.length} ambiguity collision(s). This grammar cannot be deterministically parsed under the chosen parser class without resolution.
        </div>
      </div>

      <div className="gl-conflict-cards">
        {conflicts.map((c, idx) => (
          <div key={idx} className="gl-conflict-card">
            <div className="gl-conflict-header">
              <span className={`gl-badge ${c.type.includes('Reduce / Reduce') ? 'gl-badge-danger' : 'gl-badge-warning'}`}>
                {c.type}
              </span>
              {c.state && <span className="gl-badge gl-badge-neutral">State: {c.state}</span>}
              {c.terminal && <span className="gl-badge gl-badge-cyan">Lookahead: {c.terminal}</span>}
            </div>
            <p className="gl-conflict-msg">{c.message}</p>
            {c.rules && c.rules.length > 0 && (
              <div className="gl-conflict-rules">
                <span className="gl-rules-label">Conflicting Rules:</span>
                <ul>
                  {c.rules.map((rule, rIdx) => (
                    <li key={rIdx} className="gl-conflict-rule-item">
                      <code>{rule}</code>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
