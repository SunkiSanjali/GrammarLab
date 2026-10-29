import React from 'react';
import ParsingTableView from './ParsingTableView';
import ConflictBanner from './ConflictBanner';

export default function LL1SectionView({ ll1Data }) {
  if (!ll1Data) {
    return (
      <div className="gl-tab-content">
        <div className="gl-section-header">
          <div>
            <h3 className="gl-section-title">LL(1) Predictive Parsing Table M[A, a]</h3>
            <p className="gl-section-desc">
              Constructed by placing rule A → α into table entries corresponding to FIRST(α) and FOLLOW(A).
            </p>
          </div>
        </div>
        <div className="gl-empty-state">
          No LL(1) table generated yet. Click <strong>"Run Compiler Analysis"</strong> to construct the predictive table and analyze conflicts.
        </div>
      </div>
    );
  }

  return (
    <div className="gl-tab-content">
      <div className="gl-section-header">
        <div>
          <h3 className="gl-section-title">LL(1) Predictive Parsing Table M[A, a]</h3>
          <p className="gl-section-desc">
            Constructed by placing rule A → α into table entries corresponding to FIRST(α) and FOLLOW(A).
          </p>
        </div>

        <div className="gl-status-pill">
          {ll1Data.isLL1 ? (
            <span className="gl-badge gl-badge-success">✓ Grammar is LL(1)</span>
          ) : (
            <span className="gl-badge gl-badge-danger">✕ Grammar is NOT LL(1)</span>
          )}
        </div>
      </div>

      {/* Conflict banner */}
      <ConflictBanner conflicts={ll1Data.conflicts} />

      {/* Table view */}
      <div style={{ marginTop: '1.5rem' }}>
        <h4 className="gl-sub-title">LL(1) Parsing Table</h4>
        <ParsingTableView
          type="LL1"
          headers={ll1Data.headers}
          rows={ll1Data.rows}
        />
      </div>

      {/* Educational note on LL(1) criteria */}
      <div className="gl-callout-box" style={{ marginTop: '1.5rem' }}>
        <h4 className="gl-callout-title">LL(1) Parsing Conditions</h4>
        <ul className="gl-callout-list">
          <li>For every non-terminal A with rules A → α | β:</li>
          <li>1. FIRST(α) ∩ FIRST(β) = ∅ (No First/First collisions).</li>
          <li>2. If α derives ε, then FIRST(β) ∩ FOLLOW(A) = ∅ (No First/Follow collisions).</li>
        </ul>
      </div>
    </div>
  );
}
