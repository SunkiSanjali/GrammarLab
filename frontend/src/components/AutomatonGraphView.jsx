import React, { useState } from 'react';

export default function AutomatonGraphView({ itemSets = [], title = 'LR State-Transition Automaton (DFA)' }) {
  const [zoom, setZoom] = useState(1);
  const [selectedState, setSelectedState] = useState(null);

  if (!itemSets || itemSets.length === 0) {
    return <div className="gl-empty-state">No automaton available.</div>;
  }

  // Pre-calculate node coordinates in a tidy grid or circle layout
  const cols = Math.min(4, Math.max(2, Math.ceil(Math.sqrt(itemSets.length))));
  const nodeWidth = 160;
  const nodeHeight = 80;
  const gapX = 100;
  const gapY = 80;
  const startX = 60;
  const startY = 60;

  const nodes = itemSets.map((s, idx) => {
    const col = idx % cols;
    const row = Math.floor(idx / cols);
    const x = startX + col * (nodeWidth + gapX);
    const y = startY + row * (nodeHeight + gapY);
    return {
      ...s,
      x,
      y,
      width: nodeWidth,
      height: nodeHeight,
    };
  });

  const nodeMap = new Map(nodes.map((n) => [n.name, n]));

  // Build edges
  const edges = [];
  nodes.forEach((source) => {
    if (source.gotos) {
      source.gotos.forEach((g) => {
        const target = nodeMap.get(g.target);
        if (target) {
          edges.push({
            from: source,
            to: target,
            symbol: g.symbol,
            id: `${source.name}-${g.symbol}-${target.name}`,
          });
        }
      });
    }
  });

  const svgWidth = Math.max(800, startX + cols * (nodeWidth + gapX) + 60);
  const svgHeight = Math.max(500, startY + (Math.floor(itemSets.length / cols) + 1) * (nodeHeight + gapY) + 60);

  return (
    <div className="gl-automaton-container">
      <div className="gl-automaton-toolbar">
        <div className="gl-toolbar-left">
          <h4 className="gl-sub-title">{title}</h4>
          <span className="gl-count-tag">{nodes.length} Nodes • {edges.length} Transitions</span>
        </div>

        <div className="gl-zoom-controls">
          <button
            type="button"
            className="gl-btn-icon"
            onClick={() => setZoom((z) => Math.max(0.5, z - 0.15))}
            title="Zoom Out"
          >
            -
          </button>
          <span className="gl-zoom-level">{Math.round(zoom * 100)}%</span>
          <button
            type="button"
            className="gl-btn-icon"
            onClick={() => setZoom((z) => Math.min(2, z + 0.15))}
            title="Zoom In"
          >
            +
          </button>
          <button
            type="button"
            className="gl-btn-icon"
            onClick={() => {
              setZoom(1);
              setSelectedState(null);
            }}
            title="Reset View"
          >
            ↺
          </button>
        </div>
      </div>

      <div className="gl-canvas-viewport">
        <svg
          className="gl-automaton-svg"
          width={svgWidth * zoom}
          height={svgHeight * zoom}
          viewBox={`0 0 ${svgWidth} ${svgHeight}`}
        >
          <defs>
            <marker
              id="arrowhead"
              markerWidth="8"
              markerHeight="6"
              refX="7"
              refY="3"
              orient="auto"
            >
              <polygon points="0 0, 8 3, 0 6" fill="#2563eb" />
            </marker>
          </defs>

          {/* Render Transitions (Edges) */}
          {edges.map((e) => {
            const isSelfLoop = e.from.name === e.to.name;
            const isHighlighted =
              selectedState && (selectedState.name === e.from.name || selectedState.name === e.to.name);

            if (isSelfLoop) {
              const x = e.from.x + nodeWidth / 2;
              const y = e.from.y;
              return (
                <g key={e.id} className="gl-edge-group">
                  <path
                    d={`M ${x - 20} ${y} C ${x - 40} ${y - 45}, ${x + 40} ${y - 45}, ${x + 20} ${y}`}
                    fill="none"
                    stroke={isHighlighted ? '#2563eb' : '#94a3b8'}
                    strokeWidth={isHighlighted ? 2 : 1.5}
                    markerEnd="url(#arrowhead)"
                  />
                  <text
                    x={x}
                    y={y - 35}
                    fill="#2563eb"
                    fontSize="11"
                    fontFamily="var(--cs-font-mono)"
                    fontWeight="600"
                    textAnchor="middle"
                  >
                    {e.symbol}
                  </text>
                </g>
              );
            }

            const x1 = e.from.x + nodeWidth / 2;
            const y1 = e.from.y + nodeHeight / 2;
            const x2 = e.to.x + nodeWidth / 2;
            const y2 = e.to.y + nodeHeight / 2;
            const midX = (x1 + x2) / 2;
            const midY = (y1 + y2) / 2;

            return (
              <g key={e.id} className="gl-edge-group">
                <line
                  x1={x1}
                  y1={y1}
                  x2={x2}
                  y2={y2}
                  stroke={isHighlighted ? '#2563eb' : '#cbd5e1'}
                  strokeWidth={isHighlighted ? 2 : 1.5}
                  markerEnd="url(#arrowhead)"
                />
                <circle cx={midX} cy={midY} r="10" fill="#ffffff" stroke="#94a3b8" />
                <text
                  x={midX}
                  y={midY + 3.5}
                  fill="#2563eb"
                  fontSize="11"
                  fontWeight="600"
                  fontFamily="var(--cs-font-mono)"
                  textAnchor="middle"
                >
                  {e.symbol}
                </text>
              </g>
            );
          })}

          {/* Render States (Nodes) */}
          {nodes.map((n) => {
            const isSelected = selectedState && selectedState.name === n.name;
            return (
              <g
                key={n.name}
                className="gl-node-group"
                transform={`translate(${n.x}, ${n.y})`}
                onClick={() => setSelectedState(n)}
                style={{ cursor: 'pointer' }}
              >
                <rect
                  width={n.width}
                  height={n.height}
                  rx="6"
                  className={`gl-node-rect ${isSelected ? 'gl-node-selected' : ''}`}
                />
                <text
                  x="14"
                  y="26"
                  className="gl-node-title"
                  fill="#1e293b"
                  fontSize="13"
                  fontWeight="700"
                >
                  State {n.name}
                </text>
                <text
                  x="14"
                  y="46"
                  fill="#64748b"
                  fontSize="11"
                  fontFamily="var(--cs-font-mono)"
                >
                  {n.items.length} items
                </text>
                <text
                  x="14"
                  y="64"
                  fill="#2563eb"
                  fontSize="10"
                  fontFamily="var(--cs-font-mono)"
                >
                  {n.items[0]?.rule.substring(0, 18)}...
                </text>
              </g>
            );
          })}
        </svg>

        {/* Selected State Inspector Drawer */}
        {selectedState && (
          <div className="gl-state-drawer">
            <div className="gl-drawer-header">
              <div className="gl-state-badge">{selectedState.name} Details</div>
              <button
                type="button"
                className="gl-btn-close"
                onClick={() => setSelectedState(null)}
              >
                ✕
              </button>
            </div>
            <div className="gl-drawer-body">
              <span className="gl-drawer-label">Items in closure:</span>
              <div className="gl-drawer-items">
                {selectedState.items.map((item, idx) => (
                  <div key={idx} className="gl-drawer-item-row">
                    <code>{item.rule}</code>
                    {item.lookahead && <span className="gl-lookahead-badge">[{item.lookahead}]</span>}
                  </div>
                ))}
              </div>

              {selectedState.gotos && selectedState.gotos.length > 0 && (
                <div style={{ marginTop: '1rem' }}>
                  <span className="gl-drawer-label">Transitions:</span>
                  <div className="gl-goto-badges" style={{ marginTop: '0.4rem' }}>
                    {selectedState.gotos.map((g, gIdx) => (
                      <span key={gIdx} className="gl-goto-pill">
                        {g.symbol} → {g.target}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
