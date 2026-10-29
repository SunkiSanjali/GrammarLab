import React from 'react';

const SECTIONS = [
  { id: 'grammar', label: '1. CFG Formal Tuple', code: 'G', icon: '📐' },
  { id: 'first-follow', label: '2. FIRST & FOLLOW', code: 'F/F', icon: '🎯' },
  { id: 'll1', label: '3. LL(1) Predictive', code: 'LL1', icon: '⚡' },
  { id: 'lr0', label: '4. LR(0) Canonical', code: 'LR0', icon: '⚙️' },
  { id: 'slr1', label: '5. SLR(1) Simple', code: 'SLR1', icon: '🔍' },
  { id: 'clr1', label: '6. CLR(1) Canonical', code: 'CLR1', icon: '💎' },
  { id: 'lalr1', label: '7. LALR(1) Merged', code: 'LALR1', icon: '🔄' },
  { id: 'string-parser', label: '8. Pushdown Automaton', code: 'PDA', icon: '▶️' },
];

export default function TabNav({ activeSection, onSelectSection }) {
  return (
    <nav className="gl-tab-nav" aria-label="Compiler Studio Engines">
      <div className="gl-tab-nav-track">
        {SECTIONS.map((sec) => {
          const isActive = activeSection === sec.id;
          return (
            <button
              key={sec.id}
              id={`tab-${sec.id}`}
              type="button"
              className={`gl-tab-button ${isActive ? 'active' : ''}`}
              onClick={() => onSelectSection(sec.id)}
              role="tab"
              aria-selected={isActive}
            >
              <span className="gl-tab-code-badge">{sec.code}</span>
              <span className="gl-tab-label">{sec.label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
