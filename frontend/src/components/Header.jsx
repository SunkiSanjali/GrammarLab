import React, { useState } from 'react';

export default function Header({ backendHealth, onTestConnection, isPinging }) {
  const [showNotationModal, setShowNotationModal] = useState(false);

  return (
    <>
      <header className="gl-header">
        <div className="gl-header-brand">
          <div className="gl-logo-badge" title="GrammarLab Compiler Studio">
            <span className="gl-logo-symbol">&gt;_</span>
          </div>
          <div className="gl-title-group">
            <div className="gl-title-row">
              <h1 className="gl-app-title">GrammarLab</h1>
              <span className="gl-app-badge">COMPILER STUDIO</span>
              <span className="gl-app-version">v2.4 LTS</span>
            </div>
            <span className="gl-app-subtitle">Interactive Compiler Engineering Workbench • Top-Down &amp; Bottom-Up Automata</span>
          </div>
        </div>

        <div className="gl-header-actions">
          <div className="gl-status-pill">
            <span
              className={`gl-status-dot ${
                backendHealth.status === 'online'
                  ? 'online'
                  : backendHealth.status === 'offline'
                  ? 'offline'
                  : 'checking'
              }`}
            />
            <span className="gl-status-text">
              Engine:{' '}
              <strong
                className={
                  backendHealth.status === 'online'
                    ? 'gl-status-online-text'
                    : backendHealth.status === 'offline'
                    ? 'gl-status-offline-text'
                    : 'gl-status-checking-text'
                }
              >
                {backendHealth.status === 'online'
                  ? 'Java 21 • Online'
                  : backendHealth.status === 'offline'
                  ? 'Backend Offline'
                  : 'Connecting...'}
              </strong>
            </span>
          </div>

          <button
            id="btn-test-connection"
            className="gl-btn gl-btn-ghost gl-btn-sm"
            onClick={onTestConnection}
            disabled={isPinging}
            title="Ping Spring Boot REST Service"
          >
            {isPinging ? 'Pinging...' : '⚡ Ping API'}
          </button>

          <button
            type="button"
            className="gl-btn gl-btn-outline gl-btn-sm"
            onClick={() => setShowNotationModal(true)}
            title="Grammar syntax & specification reference"
          >
            <span style={{ marginRight: '4px' }}>📖</span> Notation Guide
          </button>
        </div>
      </header>

      {/* Notation Reference Modal */}
      {showNotationModal && (
        <div className="gl-modal-backdrop" onClick={() => setShowNotationModal(false)}>
          <div className="gl-modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="gl-modal-header">
              <div className="gl-modal-title-group">
                <span className="gl-badge gl-badge-cyan">SPECIFICATION</span>
                <h3 className="gl-modal-title">Context-Free Grammar Notation &amp; Token Rules</h3>
              </div>
              <button
                type="button"
                className="gl-btn-close"
                onClick={() => setShowNotationModal(false)}
                title="Close"
              >
                ✕
              </button>
            </div>

            <div className="gl-modal-body">
              <div className="gl-guide-grid">
                <div className="gl-guide-card">
                  <div className="gl-guide-card-title">🔤 Non-Terminals (V<sub>N</sub>)</div>
                  <p>Represented by uppercase symbols or multi-character identifiers:</p>
                  <code>E, T, F, Expr, Term, Factor, Stmt</code>
                </div>

                <div className="gl-guide-card">
                  <div className="gl-guide-card-title">🔠 Terminals (V<sub>T</sub>)</div>
                  <p>Represented by lowercase words or operator tokens:</p>
                  <code>id, num, while, +, *, (, ), =, ;, &gt;</code>
                </div>

                <div className="gl-guide-card">
                  <div className="gl-guide-card-title">➡️ Production Arrow</div>
                  <p>Separate left-hand side and right-hand side using:</p>
                  <code>-&gt; &nbsp;or&nbsp; →</code>
                </div>

                <div className="gl-guide-card">
                  <div className="gl-guide-card-title">⚡ Alternatives &amp; Epsilon</div>
                  <p>Separate multiple branches with pipe; use Greek epsilon:</p>
                  <code>| &nbsp;(pipe) &nbsp;and&nbsp; ε &nbsp;(epsilon)</code>
                </div>
              </div>

              <div className="gl-guide-example-box">
                <div className="gl-guide-example-header">Standard Textbook Example:</div>
                <pre className="gl-guide-code">
{`E -> E + T | T
T -> T * F | F
F -> ( E ) | id`}
                </pre>
              </div>

              <div className="gl-guide-notes">
                <p><strong>Note on Symbol Separation:</strong> Whitespace tokenization (e.g. <code>id + id</code>) is recommended to ensure unambiguous token recognition for multi-character identifiers.</p>
              </div>
            </div>

            <div className="gl-modal-footer">
              <button
                type="button"
                className="gl-btn gl-btn-primary"
                onClick={() => setShowNotationModal(false)}
              >
                Close Reference
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
