"use client";

import confetti from "canvas-confetti";
import { useEffect, type ReactNode } from "react";

// ── JUnit parser ─────────────────────────────────────────────────────────────

interface TestResult {
  name: string;
  passed: boolean;
  error: string | null;
}

interface JUnitSummary {
  results: TestResult[];
  total: number;
  passedCount: number;
  failedCount: number;
}

function parseJUnit(raw: string): JUnitSummary | null {
  // Strip sponsoring noise
  const cleaned = raw.replace(/^Thanks for using JUnit!.*$/m, "").trim();

  // 1. Extract leaf test results from the tree.
  // Tree leaf lines look like: "   ├─ testAdd() ✔" or "   └─ testSubtract() ✘"
  // We identify leaves by the presence of "(" in the name (methods, not classes).
  const treePattern = /[├└]─\s+(.+?)\s*([✔✘])\s*$/;
  const results: TestResult[] = [];
  for (const line of cleaned.split("\n")) {
    const match = treePattern.exec(line);
    if (match) {
      const name = (match[1] ?? "").trim();
      if (name.includes("(")) {
        results.push({ name, passed: (match[2] ?? "") === "✔", error: null });
      }
    }
  }

  // 2. Extract error messages from the Failures section.
  // Each failure block looks like:
  //   JUnit Jupiter:SolutionTest:testSubtract()
  //     MethodSource [...]
  //     => org.opentest4j.AssertionFailedError: expected: <5> but was: <0>
  //         at SolutionTest...
  const failuresBody = /Failures \(\d+\):([\s\S]*?)(?:\nTest run|\n\[\s+\d|\Z)/.exec(cleaned);
  if (failuresBody) {
    const blocks = (failuresBody[1] ?? "").split(/\n(?=\s{1,4}JUnit )/);
    for (const block of blocks) {
      // The first non-empty line is "  JUnit Jupiter:ClassName:testName()"
      const blockLines = block.split("\n");
      const headerMatch = /JUnit [^:]+:[^:]+:(.+?)\s*$/.exec(blockLines[0] ?? "");
      if (!headerMatch) continue;
      const testName = (headerMatch[1] ?? "").trim();

      // Find the "=> ..." line and strip the Java exception class prefix
      const arrowLine = blockLines.find((line) => /=>\s/.test(line));
      let errorMsg: string | null = null;
      if (arrowLine) {
        const afterArrow = /=>\s+(.+)/.exec(arrowLine)?.[1] ?? "";
        // Strip "org.opentest4j.AssertionFailedError: " and similar prefixes
        errorMsg = afterArrow.replace(/^[\w.$]+(?:Error|Exception):\s*/i, "").trim() || afterArrow.trim();
      }

      const result = results.find((r) => r.name === testName);
      if (result) result.error = errorMsg;
    }
  }

  // 3. Parse stat lines "[  N tests found  ]"
  const total =
    parseInt(/\[\s+(\d+) tests? found\s+\]/.exec(cleaned)?.[1] ?? "0") || results.length;
  const failedCount =
    parseInt(/\[\s+(\d+) tests? failed\s+\]/.exec(cleaned)?.[1] ?? "0") ||
    results.filter((r) => !r.passed).length;
  const passedCount =
    parseInt(/\[\s+(\d+) tests? successful\s+\]/.exec(cleaned)?.[1] ?? "0") ||
    results.filter((r) => r.passed).length;

  return { results, total, passedCount, failedCount };
}

// ── Terminal renderer ─────────────────────────────────────────────────────────

function TerminalContent({ logs, isPassed }: { logs: string | null; isPassed: boolean }) {
  if (!logs) {
    return (
      <p className={isPassed ? "text-success" : "text-error"}>
        {isPassed ? "✔ All tests passed!" : "✗ Tests failed."}
      </p>
    );
  }

  const summary = parseJUnit(logs);

  // If parsing extracted nothing useful, fall back to plain dump
  if (!summary || summary.results.length === 0) {
    return (
      <pre className="whitespace-pre-wrap text-[#8b949e] leading-relaxed">{logs}</pre>
    );
  }

  const { results, total, failedCount } = summary;
  const failed = results.filter((r) => !r.passed);
  const passed = results.filter((r) => r.passed);

  return (
    <div className="flex flex-col gap-3 py-1 font-sans">
      {/* Summary line */}
      <p className={`text-sm font-semibold ${failedCount > 0 ? "text-error" : "text-success"}`}>
        {failedCount > 0
          ? `${failedCount} of ${total} test${total !== 1 ? "s" : ""} failed`
          : `All ${total} test${total !== 1 ? "s" : ""} passed`}
      </p>

      {/* Failed tests */}
      {failed.length > 0 && (
        <div className="flex flex-col gap-1.5">
          {failed.map((test, idx) => (
            <div key={idx} className="rounded-lg border border-error/25 bg-error/8 px-3 py-2.5">
              <div className="flex items-start gap-2">
                <span className="mt-0.5 shrink-0 text-sm text-error">✗</span>
                <div className="min-w-0 flex-1">
                  <code className="text-xs font-bold text-error">{test.name}</code>
                  {test.error && (
                    <p className="mt-1 text-xs leading-relaxed text-[#f59e0b]">{test.error}</p>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Passed tests */}
      {passed.length > 0 && (
        <div className="flex flex-col gap-0.5">
          {passed.map((test, idx) => (
            <div key={idx} className="flex items-center gap-2 px-1 py-0.5">
              <span className="text-xs text-success/50">✔</span>
              <code className="text-xs text-white/30">{test.name}</code>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ── VerdictPanel ──────────────────────────────────────────────────────────────

type SubmissionStatus = "PENDING" | "PASSED" | "FAILED";

interface VerdictPanelProps {
  status: SubmissionStatus;
  logs: string | null;
  durationMs: number | null;
  isOpen: boolean;
  isLoading: boolean;
  onToggle: () => void;
}

export function VerdictPanel({
  status,
  logs,
  durationMs,
  isOpen,
  isLoading,
  onToggle,
}: VerdictPanelProps): ReactNode {
  useEffect(() => {
    if (status === "PASSED") {
      confetti({
        particleCount: 120,
        spread: 80,
        origin: { y: 0.7 },
        colors: ["#EA580C", "#10B981", "#3B82F6", "#F1F5F9"],
      });
    }
  }, [status]);

  const isPassed = !isLoading && status === "PASSED";
  const isFailed = !isLoading && status === "FAILED";

  const barBg = isPassed
    ? "bg-success/10 border-success/30"
    : isFailed
      ? "bg-error/10 border-error/30"
      : "bg-bg-card border-white/10";

  const panelHeightClass = isOpen ? "h-64" : "h-10";

  return (
    <div className={`flex flex-col border-t transition-all duration-200 ${panelHeightClass} ${barBg}`}>
      {/* Status bar / toggle */}
      <button
        onClick={onToggle}
        className="flex h-10 shrink-0 items-center gap-2.5 px-4 text-xs font-medium transition-colors hover:bg-white/5"
        aria-expanded={isOpen}
        aria-controls="terminal-output"
      >
        {isLoading && (
          <span className="h-2 w-2 animate-pulse rounded-full bg-accent-java" />
        )}
        {isPassed && (
          <svg className="h-3.5 w-3.5 text-success" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
          </svg>
        )}
        {isFailed && (
          <svg className="h-3.5 w-3.5 text-error" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
          </svg>
        )}
        {!isLoading && status === "PENDING" && (
          <span className="h-2 w-2 rounded-full bg-white/30" />
        )}

        <span
          className={isPassed ? "text-success" : isFailed ? "text-error" : "text-text-muted"}
          aria-live="assertive"
        >
          {isLoading
            ? "Running tests…"
            : isPassed
              ? "All tests passed"
              : isFailed
                ? "Tests failed"
                : "Terminal"}
        </span>

        {durationMs !== null && !isLoading && (
          <span className="text-text-muted">{durationMs} ms</span>
        )}

        <svg
          className={`ml-auto h-3.5 w-3.5 text-text-muted transition-transform ${isOpen ? "rotate-180" : ""}`}
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path fillRule="evenodd" d="M14.707 12.707a1 1 0 01-1.414 0L10 9.414l-3.293 3.293a1 1 0 01-1.414-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 010 1.414z" clipRule="evenodd" />
        </svg>
      </button>

      {/* Content */}
      {isOpen && (
        <div
          id="terminal-output"
          role="log"
          aria-live="polite"
          aria-label="Test output"
          className="flex-1 overflow-y-auto bg-[#0d1117] px-4 pb-4 pt-3"
        >
          {isLoading && (
            <p className="font-mono text-xs text-text-muted">
              <span className="text-accent-java">$</span> Running tests in sandbox…
            </p>
          )}
          {(isPassed || isFailed) && (
            <TerminalContent logs={logs} isPassed={isPassed} />
          )}
        </div>
      )}
    </div>
  );
}
