import React from 'react';

export default function ParsingTableView({
  type = 'LR', // 'LL1' | 'LR'
  actionHeaders = [],
  gotoHeaders = [],
  headers = [], // For LL1
  rows = [],
  tableRows = [], // For LR
}) {
  if (type === 'LL1') {
    return (
      <div className="gl-table-container">
        <table className="gl-data-table gl-matrix-table">
          <thead>
            <tr>
              <th className="gl-sticky-col">Non-Terminal</th>
              {headers.map((h) => (
                <th key={h} className="gl-header-terminal">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.nonTerminal}>
                <td className="gl-sticky-col">
                  <span className="gl-nt-badge">{row.nonTerminal}</span>
                </td>
                {headers.map((h) => {
                  const val = row.cells[h];
                  const isConflict = val && val.includes('Conflict');
                  return (
                    <td key={h} className={isConflict ? 'gl-cell-conflict' : ''}>
                      {val ? (
                        <span
                          className={`gl-table-entry ${
                            isConflict ? 'gl-entry-conflict' : 'gl-entry-rule'
                          }`}
                        >
                          {val}
                        </span>
                      ) : (
                        <span className="gl-cell-empty">—</span>
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  // LR Parsing Table (Action + Goto)
  return (
    <div className="gl-table-container">
      <table className="gl-data-table gl-matrix-table">
        <thead>
          {/* Top super-headers */}
          <tr className="gl-super-header-row">
            <th rowSpan={2} className="gl-sticky-col gl-state-col">
              State
            </th>
            <th colSpan={actionHeaders.length} className="gl-super-header gl-super-action">
              ACTION (Input Terminals &amp; $)
            </th>
            {gotoHeaders.length > 0 && (
              <th colSpan={gotoHeaders.length} className="gl-super-header gl-super-goto">
                GOTO (Non-Terminals)
              </th>
            )}
          </tr>
          {/* Column sub-headers */}
          <tr>
            {actionHeaders.map((t) => (
              <th key={t} className="gl-header-terminal">
                {t}
              </th>
            ))}
            {gotoHeaders.map((nt) => (
              <th key={nt} className="gl-header-nonterminal">
                {nt}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {tableRows.map((row) => (
            <tr key={row.state}>
              <td className="gl-sticky-col gl-state-col">
                <span className="gl-state-pill">{row.state}</span>
              </td>

              {/* ACTION cells */}
              {actionHeaders.map((t) => {
                const val = row.actions ? row.actions[t] : undefined;
                if (!val) {
                  return (
                    <td key={t} className="gl-cell-empty-td">
                      <span className="gl-cell-empty">—</span>
                    </td>
                  );
                }

                const isConflict = val.includes('/') || val.includes('Conflict');
                const isShift = val.startsWith('s') && !isConflict;
                const isReduce = val.startsWith('r') && !isConflict;
                const isAccept = val === 'acc';

                let badgeClass = 'gl-entry-neutral';
                if (isConflict) badgeClass = 'gl-entry-conflict';
                else if (isShift) badgeClass = 'gl-entry-shift';
                else if (isReduce) badgeClass = 'gl-entry-reduce';
                else if (isAccept) badgeClass = 'gl-entry-accept';

                return (
                  <td key={t} className={isConflict ? 'gl-cell-conflict' : ''}>
                    <span className={`gl-action-badge ${badgeClass}`}>{val}</span>
                  </td>
                );
              })}

              {/* GOTO cells */}
              {gotoHeaders.map((nt) => {
                const val = row.gotos ? row.gotos[nt] : undefined;
                return (
                  <td key={nt}>
                    {val ? (
                      <span className="gl-goto-state-badge">{val}</span>
                    ) : (
                      <span className="gl-cell-empty">—</span>
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
