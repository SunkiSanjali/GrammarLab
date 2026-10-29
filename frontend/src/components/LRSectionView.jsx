import React, { useState } from 'react';
import ParsingTableView from './ParsingTableView';
import ItemSetCardsView from './ItemSetCardsView';
import AutomatonGraphView from './AutomatonGraphView';
import ConflictBanner from './ConflictBanner';

export default function LRSectionView({
  title,
  subtitle,
  algorithmType, // 'LR0' | 'SLR1' | 'CLR1' | 'LALR1'
  data,
}) {
  const [activeSubTab, setActiveSubTab] = useState('table'); // 'table' | 'items' | 'graph'

  if (!data) return null;

  return (
    <div className="gl-tab-content">
      <div className="gl-section-header">
        <div>
          <div className="gl-section-title-row">
            <span className="gl-badge gl-badge-cyan">{algorithmType}</span>
            <h3 className="gl-section-title">{title}</h3>
          </div>
          <p className="gl-section-desc">{subtitle}</p>
        </div>

        <div className="gl-header-right-group">
          {data.deterministic ? (
            <span className="gl-badge gl-badge-success">✓ Deterministic Table</span>
          ) : (
            <span className="gl-badge gl-badge-warning">
              ⚠️ {data.conflicts ? data.conflicts.length : 0} Conflict(s)
            </span>
          )}

          <div className="gl-subtab-switch">
            <button
              type="button"
              className={`gl-subtab-btn ${activeSubTab === 'table' ? 'active' : ''}`}
              onClick={() => setActiveSubTab('table')}
            >
              <span className="gl-subtab-icon">⊞</span> Parsing Table
            </button>
            {data.itemSets && (
              <button
                type="button"
                className={`gl-subtab-btn ${activeSubTab === 'items' ? 'active' : ''}`}
                onClick={() => setActiveSubTab('items')}
              >
                <span className="gl-subtab-icon">☰</span> Item Sets ({data.itemSets.length})
              </button>
            )}
            {data.itemSets && (
              <button
                type="button"
                className={`gl-subtab-btn ${activeSubTab === 'graph' ? 'active' : ''}`}
                onClick={() => setActiveSubTab('graph')}
              >
                <span className="gl-subtab-icon">☍</span> Automaton Graph
              </button>
            )}
          </div>
        </div>
      </div>

      {/* LALR Merged States Info Box */}
      {algorithmType === 'LALR1' && data.mergedPairs && (
        <div className="gl-merged-card">
          <div className="gl-merged-header">
            <span className="gl-badge gl-badge-purple">LALR(1) Core Merging</span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              {data.mergedPairs.length} state clusters collapsed
            </span>
          </div>
          <div className="gl-merged-list">
            {data.mergedPairs.map((pair, idx) => (
              <div key={idx} className="gl-merged-item">
                <span className="gl-merged-tag">{pair.mergedName}</span>
                <span className="gl-merged-arrow">from</span>
                <span className="gl-merged-sources">{pair.from.join(' + ')}</span>
                <code className="gl-merged-core">Core: {pair.core}</code>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Conflicts Banner */}
      <ConflictBanner conflicts={data.conflicts} />

      {/* Subtab Content */}
      <div style={{ marginTop: '1.5rem' }}>
        {activeSubTab === 'table' && (
          <div>
            <h4 className="gl-sub-title">ACTION &amp; GOTO Parsing Table</h4>
            <ParsingTableView
              type="LR"
              actionHeaders={data.actionHeaders}
              gotoHeaders={data.gotoHeaders}
              tableRows={data.tableRows}
            />
          </div>
        )}

        {activeSubTab === 'items' && data.itemSets && (
          <ItemSetCardsView
            itemSets={data.itemSets}
            title={`${algorithmType} Item Sets (Canonical Collection)`}
          />
        )}

        {activeSubTab === 'graph' && data.itemSets && (
          <AutomatonGraphView
            itemSets={data.itemSets}
            title={`${algorithmType} State-Transition Diagram`}
          />
        )}
      </div>
    </div>
  );
}
