import React from 'react';
import { GRAMMAR_PRESETS } from '../data/sampleGrammarData';

export default function GrammarEditor({
  rawGrammar,
  setRawGrammar,
  inputString,
  setInputString,
  selectedPreset,
  setSelectedPreset,
  onGenerate,
  onClear,
  onReset,
  isLoading,
  error,
}) {
  const lineCount = Math.max(1, (rawGrammar.match(/\n/g) || []).length + 1);
  const lineNumbers = Array.from({ length: lineCount }, (_, i) => i + 1);

  const handlePresetChange = (e) => {
    const presetId = e.target.value;
    setSelectedPreset(presetId);
    const preset = GRAMMAR_PRESETS.find((p) => p.id === presetId);
    if (preset) {
      setRawGrammar(preset.grammar);
      if (preset.sampleInput) {
        setInputString(preset.sampleInput);
      }
    }
  };

  const handleInsertSymbol = (sym) => {
    setRawGrammar((prev) => prev + sym);
  };

  const handleKeyDown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      if (!isLoading && rawGrammar.trim()) {
        onGenerate();
      }
    }
  };

  return (
    <div className="gl-card gl-editor-card">
      <div className="gl-card-header gl-editor-header">
        <div className="gl-editor-header-top">
          <div className="gl-card-title-group">
            <span className="gl-badge-accent">SPECIFICATION</span>
            <h2 className="gl-card-title">Grammar Rules</h2>
          </div>
          <span className="gl-stats-badge">{lineCount} Rules • {rawGrammar.length} Chars</span>
        </div>

        {/* Preset Selector */}
        <div className="gl-preset-selector">
          <label htmlFor="grammar-presets-select" className="gl-input-label">
            Preset:
          </label>
          <select
            id="grammar-presets-select"
            className="gl-select gl-preset-select"
            value={selectedPreset}
            onChange={handlePresetChange}
          >
            {GRAMMAR_PRESETS.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="gl-card-body">
        {/* Quick Insert Symbols toolbar */}
        <div className="gl-symbol-toolbar">
          <span className="gl-toolbar-label">Tokens:</span>
          <button
            type="button"
            className="gl-pill-btn"
            onClick={() => handleInsertSymbol(' -> ')}
            title="Insert arrow ->"
          >
            {'->'}
          </button>
          <button
            type="button"
            className="gl-pill-btn"
            onClick={() => handleInsertSymbol(' | ')}
            title="Insert alternative |"
          >
            {' | '}
          </button>
          <button
            type="button"
            className="gl-pill-btn gl-pill-btn-cyan"
            onClick={() => handleInsertSymbol('ε')}
            title="Insert Greek epsilon ε"
          >
            {'ε'}
          </button>
          <button
            type="button"
            className="gl-pill-btn"
            onClick={() => handleInsertSymbol(' id ')}
            title="Insert identifier token"
          >
            id
          </button>
          <button
            type="button"
            className="gl-pill-btn"
            onClick={() => handleInsertSymbol(' num ')}
            title="Insert number token"
          >
            num
          </button>
          <button
            type="button"
            className="gl-pill-btn"
            onClick={() => handleInsertSymbol(' + ')}
            title="Insert +"
          >
            +
          </button>
          <button
            type="button"
            className="gl-pill-btn"
            onClick={() => handleInsertSymbol(' * ')}
            title="Insert *"
          >
            *
          </button>
          <button
            type="button"
            className="gl-pill-btn"
            onClick={() => handleInsertSymbol(' ( ')}
            title="Insert ("
          >
            (
          </button>
          <button
            type="button"
            className="gl-pill-btn"
            onClick={() => handleInsertSymbol(' ) ')}
            title="Insert )"
          >
            )
          </button>
        </div>

        {/* Editor with Line Numbers */}
        <div className="gl-editor-workspace">
          <div className="gl-line-numbers" aria-hidden="true">
            {lineNumbers.map((num) => (
              <div key={num} className="gl-line-number">
                {num}
              </div>
            ))}
          </div>

          <textarea
            id="grammar-input-textarea"
            className="gl-textarea-editor"
            value={rawGrammar}
            onChange={(e) => setRawGrammar(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="E -> E + T | T&#10;T -> T * F | F&#10;F -> ( E ) | id"
            rows={Math.max(6, lineCount)}
            spellCheck="false"
          />
        </div>

        {/* Test Input String Input */}
        <div className="gl-input-string-row">
          <div className="gl-field-container">
            <label htmlFor="test-input-string" className="gl-input-label">
              Input String Tape (Tokens separated by spaces):
            </label>
            <div className="gl-input-with-chips">
              <input
                id="test-input-string"
                type="text"
                className="gl-text-input gl-code-font"
                value={inputString}
                onChange={(e) => setInputString(e.target.value)}
                placeholder="e.g. id + id * id"
              />
            </div>
          </div>
        </div>

        {/* Error message */}
        {error && (
          <div className="gl-alert gl-alert-danger" role="alert">
            <span className="gl-alert-icon">⚠️</span>
            <div>
              <strong>Validation Diagnostic:</strong> {error}
            </div>
          </div>
        )}

        {/* Action Controls */}
        <div className="gl-editor-actions">
          <div className="gl-btn-group-left">
            <button
              id="btn-clear-grammar"
              type="button"
              className="gl-btn gl-btn-ghost gl-btn-sm"
              onClick={onClear}
              title="Clear grammar specification"
            >
              Clear
            </button>
            <button
              id="btn-reset-grammar"
              type="button"
              className="gl-btn gl-btn-ghost gl-btn-sm"
              onClick={onReset}
              title="Reset to default textbook grammar"
            >
              Reset
            </button>
          </div>

          <button
            id="btn-generate-analysis"
            type="button"
            className="gl-btn gl-btn-primary"
            onClick={onGenerate}
            disabled={isLoading || !rawGrammar.trim()}
            title="Compile grammar and compute all parsing tables (Ctrl+Enter)"
          >
            {isLoading ? (
              <>
                <span className="gl-spinner" /> Compiling Engine...
              </>
            ) : (
              <>
                <span className="gl-icon">⚡</span> Run Compiler Analysis
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
