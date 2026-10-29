import React, { useState } from 'react';

export default function ItemSetCardsView({ itemSets, title = 'Canonical Collection of Item Sets' }) {
  const [filterText, setFilterText] = useState('');

  if (!itemSets || itemSets.length === 0) {
    return <div className="gl-empty-state">No item sets generated.</div>;
  }

  const filteredSets = itemSets.filter(
    (s) =>
      s.name.toLowerCase().includes(filterText.toLowerCase()) ||
      s.items.some((item) => item.rule.toLowerCase().includes(filterText.toLowerCase()))
  );

  return (
    <div className="gl-item-sets-container">
      <div className="gl-sub-header">
        <div className="gl-sub-header-left">
          <h4 className="gl-sub-title">{title}</h4>
          <span className="gl-count-tag">{itemSets.length} States</span>
        </div>

        <div className="gl-search-bar">
          <input
            type="text"
            className="gl-text-input gl-search-input"
            placeholder="Search state (e.g. I0) or rule..."
            value={filterText}
            onChange={(e) => setFilterText(e.target.value)}
          />
        </div>
      </div>

      <div className="gl-item-sets-grid">
        {filteredSets.map((set) => (
          <div key={set.id} className="gl-item-set-card">
            <div className="gl-item-card-header">
              <div className="gl-state-title">
                <span className="gl-state-badge">{set.name}</span>
                <span className="gl-item-count">{set.items.length} items</span>
              </div>
            </div>

            <div className="gl-items-body">
              {set.items.map((item, idx) => {
                // Highlight the dot in the rule
                const parts = item.rule.split('•');
                return (
                  <div key={idx} className="gl-item-rule-row">
                    <code className="gl-rule-code">
                      {parts.length === 2 ? (
                        <>
                          <span>{parts[0]}</span>
                          <span className="gl-dot-highlight">•</span>
                          <span>{parts[1]}</span>
                        </>
                      ) : (
                        item.rule
                      )}
                    </code>

                    {item.lookahead && (
                      <span className="gl-lookahead-badge" title="Lookahead symbols">
                        [ {item.lookahead} ]
                      </span>
                    )}
                  </div>
                );
              })}
            </div>

            {set.gotos && set.gotos.length > 0 && (
              <div className="gl-item-card-footer">
                <span className="gl-footer-label">GOTO Transitions:</span>
                <div className="gl-goto-badges">
                  {set.gotos.map((g, gIdx) => (
                    <span key={gIdx} className="gl-goto-pill">
                      <span className="gl-goto-sym">{g.symbol}</span>
                      <span className="gl-goto-arrow">→</span>
                      <span className="gl-goto-target">{g.target}</span>
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
