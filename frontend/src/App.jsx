import React, { useState, useEffect, useCallback } from 'react';
import Header from './components/Header';
import GrammarEditor from './components/GrammarEditor';
import TabNav from './components/TabNav';
import GrammarSummaryView from './components/GrammarSummaryView';
import FirstFollowView from './components/FirstFollowView';
import LL1SectionView from './components/LL1SectionView';
import LRSectionView from './components/LRSectionView';
import StringParserView from './components/StringParserView';
import { GRAMMAR_PRESETS, SAMPLE_ANALYSIS_DATA } from './data/sampleGrammarData';
import { grammarApi } from './api/grammarApi';

export default function App() {
  const [rawGrammar, setRawGrammar] = useState(GRAMMAR_PRESETS[0].grammar);
  const [inputString, setInputString] = useState(GRAMMAR_PRESETS[0].sampleInput);
  const [selectedPreset, setSelectedPreset] = useState(GRAMMAR_PRESETS[0].id);
  const [activeSection, setActiveSection] = useState('grammar');

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [backendHealth, setBackendHealth] = useState({ status: 'checking', info: null });
  const [isPinging, setIsPinging] = useState(false);

  // Analysis results data
  const [analysisData, setAnalysisData] = useState(SAMPLE_ANALYSIS_DATA);

  // Test Spring Boot backend connectivity
  const testConnection = useCallback(async () => {
    setIsPinging(true);
    try {
      const data = await grammarApi.checkHealth();
      setBackendHealth({ status: 'online', info: data });
    } catch {
      setBackendHealth({ status: 'offline', info: null });
    } finally {
      setIsPinging(false);
    }
  }, []);

  const handleGenerate = useCallback(async (grammarToAnalyze = rawGrammar) => {
    if (!grammarToAnalyze.trim()) {
      setError('Please provide at least one grammar rule (e.g., E -> E + T | T)');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const liveValidation = await grammarApi.validateGrammar(grammarToAnalyze);
      if (liveValidation && liveValidation.valid) {
        // Fetch all real parser analyses concurrently from Spring Boot backend
        const [firstFollowRes, ll1Res, lr0Res, slr1Res, clr1Res, lalr1Res] = await Promise.all([
          grammarApi.computeFirstFollow(grammarToAnalyze),
          grammarApi.computeLL1(grammarToAnalyze),
          grammarApi.computeLR0(grammarToAnalyze),
          grammarApi.computeSLR1(grammarToAnalyze),
          grammarApi.computeCLR1(grammarToAnalyze),
          grammarApi.computeLALR1(grammarToAnalyze),
        ]);

        setAnalysisData((prev) => ({
          ...prev,
          grammarSummary: {
            startSymbol: liveValidation.startSymbol,
            nonTerminals: liveValidation.nonTerminals,
            terminals: liveValidation.terminals,
            productions: liveValidation.productions.map((p) => ({ id: p.id, text: p.formattedRule })),
            augmentedProductions: liveValidation.augmentedProductions.map((p) => ({ id: p.id, text: p.formattedRule })),
          },
          firstFollow: firstFollowRes,
          ll1: ll1Res,
          lr0: lr0Res,
          slr1: slr1Res,
          clr1: clr1Res,
          lalr1: lalr1Res,
        }));
      } else if (liveValidation && !liveValidation.valid) {
        setError(liveValidation.errors ? liveValidation.errors.join(' | ') : 'Validation failed');
      }
    } catch (err) {
      setError(err.message || 'Backend connection error');
    } finally {
      setIsLoading(false);
    }
  }, [rawGrammar]);

  // Initial check on mount
  useEffect(() => {
    testConnection();
  }, [testConnection]);

  // Run analysis once backend is confirmed online
  useEffect(() => {
    if (backendHealth.status === 'online') {
      handleGenerate();
    }
  }, [backendHealth.status, handleGenerate]);

  const handleClear = () => {
    setRawGrammar('');
    setInputString('');
    setError(null);
  };

  const handleReset = () => {
    const defaultPreset = GRAMMAR_PRESETS[0];
    setSelectedPreset(defaultPreset.id);
    setRawGrammar(defaultPreset.grammar);
    setInputString(defaultPreset.sampleInput);
    setError(null);
    setAnalysisData(SAMPLE_ANALYSIS_DATA);
  };

  return (
    <div className="gl-app-root">
      {/* Top Navbar */}
      <Header
        backendHealth={backendHealth}
        onTestConnection={testConnection}
        isPinging={isPinging}
      />

      {/* Main Workbench Body */}
      <div className="gl-workbench-layout">
        {/* Left Column: Grammar Input & Controls */}
        <aside className="gl-sidebar-pane">
          <GrammarEditor
            rawGrammar={rawGrammar}
            setRawGrammar={setRawGrammar}
            inputString={inputString}
            setInputString={setInputString}
            selectedPreset={selectedPreset}
            setSelectedPreset={setSelectedPreset}
            onGenerate={handleGenerate}
            onClear={handleClear}
            onReset={handleReset}
            isLoading={isLoading}
            error={error}
          />
        </aside>

        {/* Right Column: 8 Sections Tabs & Results */}
        <main className="gl-main-results-pane">
          <div className="gl-results-wrapper gl-card">
            {/* Top Navigation Tabs */}
            <TabNav
              activeSection={activeSection}
              onSelectSection={setActiveSection}
            />

            {/* Content Sections */}
            <div className="gl-tab-panel-container">
              {/* Section 1: Grammar */}
              {activeSection === 'grammar' && (
                <GrammarSummaryView
                  grammarSummary={analysisData.grammarSummary}
                />
              )}

              {/* Section 2: FIRST & FOLLOW */}
              {activeSection === 'first-follow' && (
                <FirstFollowView
                  firstFollowData={analysisData.firstFollow}
                />
              )}

              {/* Section 3: LL(1) */}
              {activeSection === 'll1' && (
                <LL1SectionView ll1Data={analysisData.ll1} />
              )}

              {/* Section 4: LR(0) */}
              {activeSection === 'lr0' && (
                <LRSectionView
                  title="LR(0) Parsing Table & Canonical Item Sets"
                  subtitle="Bottom-up shift-reduce parsing using items [A -> α.β] with no lookahead."
                  algorithmType="LR(0)"
                  data={analysisData.lr0}
                />
              )}

              {/* Section 5: SLR(1) */}
              {activeSection === 'slr1' && (
                <LRSectionView
                  title="SLR(1) Simple LR Parsing Table"
                  subtitle="Reductions restricted to FOLLOW(A) sets to resolve LR(0) shift-reduce conflicts."
                  algorithmType="SLR(1)"
                  data={analysisData.slr1}
                />
              )}

              {/* Section 6: CLR(1) */}
              {activeSection === 'clr1' && (
                <LRSectionView
                  title="CLR(1) Canonical LR(1) Parsing Table"
                  subtitle="Full LR(1) items [A -> α.β, a] with precise lookaheads propagated during closure."
                  algorithmType="CLR(1)"
                  data={analysisData.clr1}
                />
              )}

              {/* Section 7: LALR(1) */}
              {activeSection === 'lalr1' && (
                <LRSectionView
                  title="LALR(1) Lookahead LR Parsing Table"
                  subtitle="Compact parsing table formed by merging CLR(1) item sets that share identical LR(0) cores."
                  algorithmType="LALR(1)"
                  data={analysisData.lalr1}
                />
              )}

              {/* Section 8: String Parser */}
              {activeSection === 'string-parser' && (
                <StringParserView
                  rawGrammar={rawGrammar}
                  traceData={analysisData.stringTrace}
                  defaultInput={inputString}
                />
              )}
            </div>
          </div>
        </main>
      </div>

      {/* Global Status Bar */}
      <footer className="gl-footer">
        <div className="gl-footer-left">
          <span>GrammarLab Compiler Workbench</span>
          <span>•</span>
          <span>Start: <strong className="gl-status-accent">{analysisData.grammarSummary?.startSymbol || 'E'}</strong></span>
          <span>•</span>
          <span>V<sub>N</sub>: <strong>{analysisData.grammarSummary?.nonTerminals?.length || 0}</strong></span>
          <span>•</span>
          <span>V<sub>T</sub>: <strong>{analysisData.grammarSummary?.terminals?.length || 0}</strong></span>
          <span>•</span>
          <span>Productions: <strong>{analysisData.grammarSummary?.productions?.length || 0}</strong></span>
        </div>
        <div className="gl-footer-right">
          <span>Java 21 Spring Boot</span>
          <span>•</span>
          <span>React 19 + Vite</span>
        </div>
      </footer>
    </div>
  );
}
