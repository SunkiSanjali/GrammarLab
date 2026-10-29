import React, { useState, useEffect } from 'react';
import { grammarApi } from '../api/grammarApi';

export default function StringParserView({ rawGrammar, traceData, defaultInput = 'id + id * id' }) {
  const [steps, setSteps] = useState(traceData || []);
  const [currentStep, setCurrentStep] = useState(traceData ? traceData.length : 1);
  const [isPlaying, setIsPlaying] = useState(false);
  const [parserType, setParserType] = useState('LL(1)');
  const [customInput, setCustomInput] = useState(defaultInput);
  const [isSimulating, setIsSimulating] = useState(false);
  const [statusMessage, setStatusMessage] = useState(null);
  const [isAccepted, setIsAccepted] = useState(
    traceData && traceData.length > 0 && traceData[traceData.length - 1].action.toLowerCase().includes('accept')
  );

  const maxStep = steps.length;

  // Sync when parent traceData updates
  useEffect(() => {
    if (traceData && traceData.length > 0) {
      setSteps(traceData);
      setCurrentStep(traceData.length);
      setIsAccepted(traceData[traceData.length - 1].action.toLowerCase().includes('accept'));
    }
  }, [traceData]);

  const runSimulation = async () => {
    if (!rawGrammar || !rawGrammar.trim()) {
      setStatusMessage('Please enter a grammar in the editor before running simulation.');
      setIsAccepted(false);
      return;
    }
    setIsSimulating(true);
    setStatusMessage(null);
    try {
      const res = await grammarApi.simulateParse(rawGrammar, parserType, customInput);
      if (res.steps && res.steps.length > 0) {
        setSteps(res.steps);
        setCurrentStep(res.steps.length);
        setIsAccepted(res.accepted);
        setStatusMessage(res.message);
      } else {
        setIsAccepted(res.accepted || false);
        setStatusMessage(res.message || 'Simulation completed with 0 steps.');
      }
    } catch (err) {
      setStatusMessage('Simulation error: ' + (err.message || 'Unknown error'));
      setIsAccepted(false);
    } finally {
      setIsSimulating(false);
    }
  };

  useEffect(() => {
    let timer;
    if (isPlaying) {
      timer = setInterval(() => {
        setCurrentStep((prev) => {
          if (prev >= maxStep) {
            setIsPlaying(false);
            return prev;
          }
          return prev + 1;
        });
      }, 650);
    }
    return () => clearInterval(timer);
  }, [isPlaying, maxStep]);

  const handleNext = () => {
    setCurrentStep((s) => Math.min(maxStep, s + 1));
  };

  const handlePrev = () => {
    setCurrentStep((s) => Math.max(1, s - 1));
  };

  const handleReset = () => {
    setIsPlaying(false);
    setCurrentStep(1);
  };

  const isSyntaxError = statusMessage && statusMessage.includes('Syntax Error');

  return (
    <div className="gl-tab-content gl-console-panel">
      {/* Console Header */}
      <div className="gl-section-header">
        <div>
          <div className="gl-section-title-row">
            <span className="gl-badge gl-badge-cyan">PDA EXECUTION</span>
            <h3 className="gl-section-title">Pushdown Automaton Debugger Console</h3>
          </div>
          <p className="gl-section-desc">
            Step-by-step trace of state stack transitions, symbol shifts, and grammar reductions.
          </p>
        </div>

        <div className="gl-verdict-box">
          {isAccepted ? (
            <div className="gl-verdict-pill gl-verdict-accepted">
              <span className="gl-verdict-dot" />
              <span className="gl-verdict-text">✓ ACCEPTED // 0 ERRORS</span>
            </div>
          ) : isSyntaxError ? (
            <div className="gl-verdict-pill gl-verdict-rejected">
              <span className="gl-verdict-dot" />
              <span className="gl-verdict-text">✕ SYNTAX REJECTED</span>
            </div>
          ) : (
            <div className="gl-verdict-pill gl-verdict-neutral">
              <span className="gl-verdict-dot" />
              <span className="gl-verdict-text">● TRACE READY</span>
            </div>
          )}
        </div>
      </div>

      {statusMessage && (
        <div
          className={`gl-alert ${isAccepted ? 'gl-alert-success' : 'gl-alert-danger'}`}
          style={{ marginBottom: '1.25rem' }}
        >
          <span className="gl-alert-icon">{isAccepted ? '✓' : '⚠️'}</span>
          <div>
            <strong>Parser Output:</strong> {statusMessage}
          </div>
        </div>
      )}

      {/* Parser Configuration Toolbar */}
      <div className="gl-simulator-toolbar">
        <div className="gl-field-group">
          <label htmlFor="parser-algorithm-select" className="gl-input-label">Algorithm Engine:</label>
          <select
            id="parser-algorithm-select"
            className="gl-select"
            value={parserType}
            onChange={(e) => setParserType(e.target.value)}
          >
            <option value="LL(1)">LL(1) Predictive</option>
            <option value="LR(0)">LR(0) Canonical Shift-Reduce</option>
            <option value="SLR(1)">SLR(1) Simple Shift-Reduce</option>
            <option value="CLR(1)">CLR(1) Canonical Shift-Reduce</option>
            <option value="LALR(1)">LALR(1) Lookahead Shift-Reduce</option>
          </select>
        </div>

        <div className="gl-field-group" style={{ flex: 1 }}>
          <label htmlFor="sim-test-string" className="gl-input-label">Input Tape Buffer:</label>
          <input
            id="sim-test-string"
            type="text"
            className="gl-text-input gl-code-font"
            value={customInput}
            onChange={(e) => setCustomInput(e.target.value)}
            placeholder="Tokens separated by spaces (e.g. id + id)"
          />
        </div>

        <button
          type="button"
          className="gl-btn gl-btn-primary"
          onClick={runSimulation}
          disabled={isSimulating}
          style={{ alignSelf: 'flex-end', height: '38px' }}
        >
          {isSimulating ? (
            <>
              <span className="gl-spinner" /> Simulating...
            </>
          ) : (
            <>
              <span className="gl-icon">⚡</span> Execute Trace
            </>
          )}
        </button>

        {/* Step Controls */}
        <div className="gl-playback-controls" style={{ alignSelf: 'flex-end' }}>
          <button
            type="button"
            className="gl-btn gl-btn-ghost gl-btn-sm"
            onClick={handlePrev}
            disabled={currentStep <= 1 || isPlaying}
            title="Step Back"
          >
            ◀ Step Back
          </button>

          <button
            type="button"
            className={`gl-btn gl-btn-sm ${isPlaying ? 'gl-btn-warning' : 'gl-btn-teal'}`}
            onClick={() => setIsPlaying(!isPlaying)}
          >
            {isPlaying ? '⏸ Pause' : '▶ Play Auto'}
          </button>

          <button
            type="button"
            className="gl-btn gl-btn-ghost gl-btn-sm"
            onClick={handleNext}
            disabled={currentStep >= maxStep || isPlaying}
            title="Step Forward"
          >
            Step Next ▶
          </button>

          <button
            type="button"
            className="gl-btn gl-btn-ghost gl-btn-sm"
            onClick={handleReset}
            title="Reset to step 1"
          >
            ↺ Reset
          </button>
        </div>
      </div>

      {/* Step Progress Bar */}
      <div className="gl-step-progress-bar">
        <div className="gl-step-progress-info">
          <span className="gl-step-counter">
            STEP <strong>{String(currentStep).padStart(2, '0')}</strong> / {String(maxStep || 1).padStart(2, '0')}
          </span>
          <span className="gl-step-pct">
            {Math.round(((currentStep || 0) / Math.max(1, maxStep)) * 100)}% Execution
          </span>
        </div>
        <div className="gl-progress-track">
          <div
            className="gl-progress-fill"
            style={{ width: `${(currentStep / Math.max(1, maxStep)) * 100}%` }}
          />
        </div>
      </div>

      {/* Parsing Trace Table */}
      <div className="gl-table-container gl-console-table-container">
        <table className="gl-data-table gl-matrix-table gl-trace-table">
          <thead>
            <tr>
              <th style={{ width: '64px', textAlign: 'center' }}>Step</th>
              <th>State Stack</th>
              <th>Grammar Symbol Stack</th>
              <th>Remaining Input Tape</th>
              <th>Parser Action</th>
              <th>Production Rule</th>
            </tr>
          </thead>
          <tbody>
            {steps.slice(0, currentStep).map((s, idx) => {
              const isCurrent = idx + 1 === currentStep;
              const isShift = s.action.startsWith('Shift') || s.action.startsWith('s');
              const isReduce = s.action.startsWith('Reduce') || s.action.startsWith('r');
              const isAcc = s.action.toLowerCase().includes('accept');
              const isErr = s.action.toLowerCase().includes('error');

              let actionBadgeClass = 'gl-entry-neutral';
              if (isAcc) actionBadgeClass = 'gl-entry-accept';
              else if (isErr) actionBadgeClass = 'gl-entry-conflict';
              else if (isShift) actionBadgeClass = 'gl-entry-shift';
              else if (isReduce) actionBadgeClass = 'gl-entry-reduce';

              return (
                <tr
                  key={s.step}
                  className={`gl-trace-row ${isCurrent ? 'gl-row-active-step' : ''}`}
                >
                  <td style={{ textAlign: 'center' }}>
                    <span className="gl-step-badge">{String(s.step).padStart(2, '0')}</span>
                  </td>
                  <td>
                    <code className="gl-stack-code">{s.stateStack}</code>
                  </td>
                  <td>
                    <code className="gl-symbol-code">{s.symbolStack}</code>
                  </td>
                  <td>
                    <code className="gl-input-code">{s.inputBuffer}</code>
                  </td>
                  <td>
                    <span className={`gl-action-badge ${actionBadgeClass}`}>
                      {s.action}
                    </span>
                  </td>
                  <td>
                    {s.rule ? (
                      <code className="gl-rule-text">{s.rule}</code>
                    ) : (
                      <span className="gl-cell-empty">—</span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
