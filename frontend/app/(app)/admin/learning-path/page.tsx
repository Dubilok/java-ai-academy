"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import {
  fetchJobStatus,
  generateCourse,
  proposeCurriculum,
  proposeMoreLectures,
  proposeMoreModules,
} from "@/lib/queries/admin";
import type { CurriculumProposal, ModuleProposal } from "@/lib/schemas/admin";
import { JobsPanel } from "@/components/jobs-panel";

// ── Types ─────────────────────────────────────────────────────────────────────

type Step = "input" | "modules" | "lectures" | "generating";

interface LectureSelection {
  title: string;
  selected: boolean;
  taskCount: number;
}

interface ModuleSelection {
  proposal: ModuleProposal;
  selected: boolean;
  lectures: LectureSelection[];
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function StepIndicator({ current, step, label }: { current: Step; step: Step; label: string }) {
  const steps: Step[] = ["input", "modules", "lectures", "generating"];
  const isDone = steps.indexOf(step) < steps.indexOf(current);
  const isActive = step === current;

  return (
    <div className="flex items-center gap-2">
      <span
        className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold ${
          isDone
            ? "bg-success text-white"
            : isActive
              ? "bg-accent-java text-white"
              : "bg-white/10 text-text-muted"
        }`}
      >
        {isDone ? "✔" : steps.indexOf(step) + 1}
      </span>
      <span className={isActive ? "text-sm font-medium text-text-primary" : "text-sm text-text-muted"}>
        {label}
      </span>
    </div>
  );
}

function TaskCountPicker({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  return (
    <div className="flex items-center gap-1">
      <button
        onClick={() => onChange(Math.max(1, value - 1))}
        disabled={value <= 1}
        className="flex h-6 w-6 items-center justify-center rounded bg-white/10 text-text-muted transition-colors hover:bg-white/20 disabled:opacity-30"
      >
        −
      </button>
      <span className="w-6 text-center text-sm font-semibold text-text-primary">{value}</span>
      <button
        onClick={() => onChange(Math.min(10, value + 1))}
        disabled={value >= 10}
        className="flex h-6 w-6 items-center justify-center rounded bg-white/10 text-text-muted transition-colors hover:bg-white/20 disabled:opacity-30"
      >
        +
      </button>
    </div>
  );
}

// ── Step 1: Input ─────────────────────────────────────────────────────────────

function InputStep({ onPropose }: { onPropose: (technology: string) => void }) {
  const [technology, setTechnology] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handlePropose() {
    if (!technology.trim()) return;
    setError(null);
    setIsLoading(true);
    try {
      await onPropose(technology.trim());
    } catch (err: unknown) {
      const status =
        err instanceof Object && "response" in err
          ? (err as { response?: { status?: number } }).response?.status
          : undefined;
      if (status === 403) setError("Access denied (403). You need to be logged in as an admin account.");
      else if (status === 401) setError("Not authenticated (401). Please sign in first.");
      else if (status === 404) setError("Endpoint not found (404). Restart the backend with the latest code.");
      else if (status) setError(`Backend error (${status}). Check the Spring Boot logs.`);
      else setError("Cannot reach backend (localhost:8080). Is the server running?");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-lg">
      <h2 className="mb-2 text-lg font-semibold text-text-primary">What do you want to teach?</h2>
      <p className="mb-6 text-sm text-text-muted">
        Enter a Java technology or topic. AI will propose a full learning path with modules and lectures.
      </p>
      <input
        type="text"
        value={technology}
        onChange={(e) => setTechnology(e.target.value)}
        onKeyDown={(e) => { if (e.key === "Enter") void handlePropose(); }}
        placeholder="e.g. Spring Boot, Java Streams, Hibernate"
        className="mb-4 w-full rounded-lg border border-white/10 bg-bg-base px-4 py-3 text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-accent-blue"
      />
      {error && <p className="mb-4 text-sm text-error">{error}</p>}
      <button
        onClick={() => void handlePropose()}
        disabled={isLoading || !technology.trim()}
        className="flex items-center gap-2 rounded-lg bg-accent-java px-6 py-2.5 font-semibold text-white transition-opacity hover:opacity-90 disabled:opacity-50"
      >
        {isLoading ? (
          <>
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
            Asking AI to propose a curriculum…
          </>
        ) : (
          "Propose Learning Path →"
        )}
      </button>
    </div>
  );
}

// ── Step 2: Select modules ────────────────────────────────────────────────────

function ModulesStep({
  technology,
  proposal,
  modules,
  onToggle,
  onAddModules,
  onNext,
  onBack,
}: {
  technology: string;
  proposal: CurriculumProposal;
  modules: ModuleSelection[];
  onToggle: (idx: number) => void;
  onAddModules: (proposals: ModuleProposal[]) => void;
  onNext: () => void;
  onBack: () => void;
}) {
  const [isSuggesting, setIsSuggesting] = useState(false);
  const [suggestions, setSuggestions] = useState<ModuleProposal[]>([]);
  const [suggestError, setSuggestError] = useState<string | null>(null);
  const selectedCount = modules.filter((m) => m.selected).length;

  async function handleSuggestMore() {
    setIsSuggesting(true);
    setSuggestError(null);
    setSuggestions([]);
    try {
      const existing = modules.map((m) => m.proposal.moduleName);
      const proposed = await proposeMoreModules(technology, existing);
      setSuggestions(proposed);
    } catch {
      setSuggestError("AI could not suggest modules right now. Try again.");
    } finally {
      setIsSuggesting(false);
    }
  }

  function addSuggestion(suggestion: ModuleProposal) {
    onAddModules([suggestion]);
    setSuggestions((prev) => prev.filter((s) => s.moduleName !== suggestion.moduleName));
  }

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-text-primary">{proposal.courseName}</h2>
        <p className="mt-1 text-sm text-text-muted">{proposal.description}</p>
      </div>

      <h3 className="mb-3 text-sm font-semibold uppercase tracking-widest text-text-muted">
        Select modules to include
      </h3>

      <div className="mb-4 flex flex-col gap-2">
        {modules.map((module, idx) => (
          <button
            key={idx}
            onClick={() => onToggle(idx)}
            className={`flex items-center gap-3 rounded-xl border px-4 py-3 text-left transition-colors ${
              module.selected
                ? "border-accent-java/40 bg-accent-java/10"
                : "border-white/10 bg-bg-card hover:border-white/20"
            }`}
          >
            <span
              className={`flex h-5 w-5 shrink-0 items-center justify-center rounded border-2 text-xs font-bold transition-colors ${
                module.selected ? "border-accent-java bg-accent-java text-white" : "border-white/30 bg-transparent"
              }`}
            >
              {module.selected ? "✔" : ""}
            </span>
            <div className="min-w-0 flex-1">
              <p className="font-medium text-text-primary">{module.proposal.moduleName}</p>
              <p className="mt-0.5 text-xs text-text-muted">
                {module.proposal.lectureTopics.length} lecture
                {module.proposal.lectureTopics.length !== 1 ? "s" : ""}
              </p>
            </div>
          </button>
        ))}
      </div>

      {/* AI-suggested additional modules */}
      {suggestions.length > 0 && (
        <div className="mb-4 rounded-xl border border-accent-blue/30 bg-accent-blue/5 p-4">
          <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-accent-blue">
            AI suggests
          </p>
          <div className="flex flex-col gap-2">
            {suggestions.map((suggestion, idx) => (
              <div
                key={idx}
                className="flex items-center gap-3 rounded-lg border border-white/10 bg-bg-card px-3 py-2"
              >
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-text-primary">{suggestion.moduleName}</p>
                  <p className="text-xs text-text-muted">{suggestion.lectureTopics.length} lectures</p>
                </div>
                <button
                  onClick={() => addSuggestion(suggestion)}
                  className="shrink-0 rounded-lg bg-accent-blue px-3 py-1 text-xs font-semibold text-white hover:opacity-90"
                >
                  + Add
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {suggestError && <p className="mb-3 text-sm text-error">{suggestError}</p>}

      <button
        onClick={() => void handleSuggestMore()}
        disabled={isSuggesting}
        className="mb-6 flex items-center gap-2 rounded-lg border border-accent-blue/40 px-4 py-2 text-sm font-medium text-accent-blue transition-colors hover:bg-accent-blue/10 disabled:opacity-50"
      >
        {isSuggesting ? (
          <>
            <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-accent-blue/30 border-t-accent-blue" />
            Asking AI for more modules…
          </>
        ) : (
          "✨ Suggest more modules"
        )}
      </button>

      <div className="flex items-center justify-between">
        <button onClick={onBack} className="text-sm text-text-muted hover:text-text-primary">
          ← Back
        </button>
        <button
          onClick={onNext}
          disabled={selectedCount === 0}
          className="rounded-lg bg-accent-java px-5 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
        >
          Review Lectures ({selectedCount} module{selectedCount !== 1 ? "s" : ""}) →
        </button>
      </div>
    </div>
  );
}

// ── Step 3: Select lectures ───────────────────────────────────────────────────

function LecturesStep({
  technology,
  modules,
  defaultTaskCount,
  onToggleLecture,
  onSetTaskCount,
  onSetDefaultTaskCount,
  onAddLectures,
  onGenerate,
  onBack,
}: {
  technology: string;
  modules: ModuleSelection[];
  defaultTaskCount: number;
  onToggleLecture: (modIdx: number, lectIdx: number) => void;
  onSetTaskCount: (modIdx: number, lectIdx: number, count: number) => void;
  onSetDefaultTaskCount: (count: number) => void;
  onAddLectures: (modIdx: number, titles: string[]) => void;
  onGenerate: () => void;
  onBack: () => void;
}) {
  const [suggestingFor, setSuggestingFor] = useState<number | null>(null);
  const [suggestions, setSuggestions] = useState<Record<number, string[]>>({});
  const [suggestErrors, setSuggestErrors] = useState<Record<number, string>>({});

  const selectedModules = modules.filter((mod) => mod.selected);
  const totalTasks = selectedModules.reduce(
    (sum, m) => sum + m.lectures.filter((l) => l.selected).reduce((s, l) => s + l.taskCount, 0),
    0
  );
  const totalLectures = selectedModules.reduce(
    (sum, m) => sum + m.lectures.filter((l) => l.selected).length,
    0
  );

  async function handleSuggestLectures(modIdx: number) {
    setSuggestingFor(modIdx);
    setSuggestErrors((prev) => { const next = { ...prev }; delete next[modIdx]; return next; });
    setSuggestions((prev) => { const next = { ...prev }; delete next[modIdx]; return next; });
    try {
      const targetModule = modules[modIdx];
      if (!targetModule) return;
      const existing = targetModule.lectures.map((l) => l.title);
      const proposed = await proposeMoreLectures(technology, targetModule.proposal.moduleName, existing);
      setSuggestions((prev) => ({ ...prev, [modIdx]: proposed }));
    } catch {
      setSuggestErrors((prev) => ({ ...prev, [modIdx]: "AI could not suggest lectures. Try again." }));
    } finally {
      setSuggestingFor(null);
    }
  }

  function addLectureSuggestion(modIdx: number, title: string) {
    onAddLectures(modIdx, [title]);
    setSuggestions((prev) => ({
      ...prev,
      [modIdx]: (prev[modIdx] ?? []).filter((t) => t !== title),
    }));
  }

  return (
    <div>
      {/* Global task count */}
      <div className="mb-6 flex items-center gap-4 rounded-xl border border-white/10 bg-bg-card px-4 py-3">
        <div className="flex-1">
          <p className="text-sm font-medium text-text-primary">Tasks per lecture (default)</p>
          <p className="text-xs text-text-muted">Apply to all. Override per lecture below.</p>
        </div>
        <TaskCountPicker value={defaultTaskCount} onChange={onSetDefaultTaskCount} />
      </div>

      {/* Module accordions */}
      <div className="mb-6 flex flex-col gap-4">
        {selectedModules.map((selectedMod) => {
          const modIdx = modules.indexOf(selectedMod);
          const moduleSuggestions = suggestions[modIdx] ?? [];
          const moduleError = suggestErrors[modIdx];
          const isSuggesting = suggestingFor === modIdx;

          return (
            <div key={modIdx} className="overflow-hidden rounded-xl border border-white/10">
              <div className="bg-bg-card px-4 py-3">
                <p className="font-semibold text-text-primary">{selectedMod.proposal.moduleName}</p>
              </div>

              <div className="divide-y divide-white/5">
                {selectedMod.lectures.map((lecture, lectIdx) => (
                  <div key={lectIdx} className="flex items-center gap-3 px-4 py-2.5 hover:bg-white/[0.03]">
                    <button
                      onClick={() => onToggleLecture(modIdx, lectIdx)}
                      className={`flex h-4 w-4 shrink-0 items-center justify-center rounded border text-[10px] font-bold transition-colors ${
                        lecture.selected
                          ? "border-accent-blue bg-accent-blue text-white"
                          : "border-white/30 bg-transparent"
                      }`}
                    >
                      {lecture.selected ? "✔" : ""}
                    </button>
                    <p
                      className={`flex-1 text-sm ${
                        lecture.selected ? "text-text-primary" : "text-text-muted line-through"
                      }`}
                    >
                      {lecture.title}
                    </p>
                    {lecture.selected && (
                      <div className="flex shrink-0 items-center gap-2">
                        <span className="text-xs text-text-muted">tasks:</span>
                        <TaskCountPicker
                          value={lecture.taskCount}
                          onChange={(count) => onSetTaskCount(modIdx, lectIdx, count)}
                        />
                      </div>
                    )}
                  </div>
                ))}

                {/* AI-suggested additional lectures */}
                {moduleSuggestions.length > 0 && (
                  <div className="border-t border-accent-blue/20 bg-accent-blue/5 px-4 py-3">
                    <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-accent-blue">
                      AI suggests
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {moduleSuggestions.map((title, idx) => (
                        <button
                          key={idx}
                          onClick={() => addLectureSuggestion(modIdx, title)}
                          className="flex items-center gap-1.5 rounded-full border border-accent-blue/40 bg-bg-card px-3 py-1 text-xs text-text-primary transition-colors hover:border-accent-blue hover:bg-accent-blue/10"
                        >
                          <span className="text-accent-blue">+</span> {title}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Suggest lectures button row */}
                <div className="flex items-center px-4 py-2">
                  {moduleError && (
                    <p className="mr-3 text-xs text-error">{moduleError}</p>
                  )}
                  <button
                    onClick={() => void handleSuggestLectures(modIdx)}
                    disabled={isSuggesting}
                    className="flex items-center gap-1.5 text-xs font-medium text-accent-blue transition-colors hover:opacity-80 disabled:opacity-40"
                  >
                    {isSuggesting ? (
                      <>
                        <span className="h-3 w-3 animate-spin rounded-full border-2 border-accent-blue/30 border-t-accent-blue" />
                        Asking AI…
                      </>
                    ) : (
                      "✨ Suggest more lectures"
                    )}
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Summary + actions */}
      <div className="flex items-center justify-between">
        <button onClick={onBack} className="text-sm text-text-muted hover:text-text-primary">
          ← Back
        </button>
        <div className="flex items-center gap-4">
          <p className="text-sm text-text-muted">
            {totalLectures} lecture{totalLectures !== 1 ? "s" : ""} · {totalTasks} task
            {totalTasks !== 1 ? "s" : ""}
          </p>
          <button
            onClick={onGenerate}
            disabled={totalLectures === 0}
            className="rounded-lg bg-accent-java px-5 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
          >
            Generate Course →
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Step 4: Progress ──────────────────────────────────────────────────────────

function GeneratingStep({ jobId }: { jobId: string }) {
  const { data: jobStatus } = useQuery({
    queryKey: ["job", jobId],
    queryFn: () => fetchJobStatus(jobId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "SUCCEEDED" || status === "FAILED" ? false : 2_000;
    },
  });

  const isDone = jobStatus?.status === "SUCCEEDED";
  const isFailed = jobStatus?.status === "FAILED";
  const total = jobStatus?.totalItems ?? 0;
  const completed = jobStatus?.completedItems ?? 0;
  const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

  return (
    <div className="mx-auto max-w-lg text-center">
      {!isDone && !isFailed && (
        <>
          <div className="mb-6 flex justify-center">
            <span className="h-10 w-10 animate-spin rounded-full border-4 border-white/10 border-t-accent-java" />
          </div>
          <h2 className="mb-2 text-lg font-semibold text-text-primary">Generating your course…</h2>
          {jobStatus?.currentItem && (
            <p className="mb-4 text-sm text-text-muted">{jobStatus.currentItem}</p>
          )}
        </>
      )}
      {isDone && (
        <>
          <div className="mb-4 flex justify-center text-4xl">✅</div>
          <h2 className="mb-2 text-lg font-semibold text-success">Course generated!</h2>
        </>
      )}
      {isFailed && (
        <>
          <div className="mb-4 flex justify-center text-4xl">❌</div>
          <h2 className="mb-2 text-lg font-semibold text-error">Generation failed</h2>
          {jobStatus?.errorMessage && (
            <p className="mb-4 text-sm text-text-muted">{jobStatus.errorMessage}</p>
          )}
        </>
      )}
      {total > 0 && (
        <div className="mb-6">
          <div className="mb-1 flex justify-between text-xs text-text-muted">
            <span>{completed} / {total} tasks generated</span>
            <span>{percent}%</span>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-white/10">
            <div
              className={`h-full rounded-full transition-all duration-500 ${isDone ? "bg-success" : "bg-accent-java"}`}
              style={{ width: `${percent}%` }}
            />
          </div>
        </div>
      )}
      {isDone && jobStatus?.courseId && (
        <Link
          href={`/courses/${jobStatus.courseId}`}
          className="inline-block rounded-lg bg-accent-java px-6 py-2.5 font-semibold text-white hover:opacity-90"
        >
          View Course →
        </Link>
      )}
    </div>
  );
}

// ── Main wizard ───────────────────────────────────────────────────────────────

export default function LearningPathPage() {
  const [step, setStep] = useState<Step>("input");
  const [technology, setTechnology] = useState("");
  const [proposal, setProposal] = useState<CurriculumProposal | null>(null);
  const [modules, setModules] = useState<ModuleSelection[]>([]);
  const [defaultTaskCount, setDefaultTaskCount] = useState(1);
  const [jobId, setJobId] = useState<string | null>(null);

  async function handlePropose(tech: string) {
    const result = await proposeCurriculum(tech);
    setTechnology(tech);
    setProposal(result);
    setModules(
      result.modules.map((moduleProposal) => ({
        proposal: moduleProposal,
        selected: true,
        lectures: moduleProposal.lectureTopics.map((lectureTitle) => ({
          title: lectureTitle,
          selected: true,
          taskCount: 1,
        })),
      }))
    );
    setStep("modules");
  }

  function handleToggleModule(idx: number) {
    setModules((prev) =>
      prev.map((module, moduleIdx) => (moduleIdx === idx ? { ...module, selected: !module.selected } : module))
    );
  }

  function handleAddModules(proposals: ModuleProposal[]) {
    setModules((prev) => [
      ...prev,
      ...proposals.map((moduleProposal) => ({
        proposal: moduleProposal,
        selected: true,
        lectures: moduleProposal.lectureTopics.map((lectureTitle) => ({
          title: lectureTitle,
          selected: true,
          taskCount: defaultTaskCount,
        })),
      })),
    ]);
  }

  function handleToggleLecture(modIdx: number, lectIdx: number) {
    setModules((prev) =>
      prev.map((module, moduleIdx) =>
        moduleIdx !== modIdx
          ? module
          : {
              ...module,
              lectures: module.lectures.map((lecture, lectureIdx) =>
                lectureIdx !== lectIdx ? lecture : { ...lecture, selected: !lecture.selected }
              ),
            }
      )
    );
  }

  function handleAddLectures(modIdx: number, titles: string[]) {
    setModules((prev) =>
      prev.map((module, moduleIdx) =>
        moduleIdx !== modIdx
          ? module
          : {
              ...module,
              proposal: {
                ...module.proposal,
                lectureTopics: [...module.proposal.lectureTopics, ...titles],
              },
              lectures: [
                ...module.lectures,
                ...titles.map((title) => ({ title, selected: true, taskCount: defaultTaskCount })),
              ],
            }
      )
    );
  }

  function handleSetTaskCount(modIdx: number, lectIdx: number, count: number) {
    setModules((prev) =>
      prev.map((module, moduleIdx) =>
        moduleIdx !== modIdx
          ? module
          : {
              ...module,
              lectures: module.lectures.map((lecture, lectureIdx) =>
                lectureIdx !== lectIdx ? lecture : { ...lecture, taskCount: count }
              ),
            }
      )
    );
  }

  function handleSetDefaultTaskCount(count: number) {
    setDefaultTaskCount(count);
    setModules((prev) =>
      prev.map((module) => ({
        ...module,
        lectures: module.lectures.map((lecture) => ({ ...lecture, taskCount: count })),
      }))
    );
  }

  async function handleGenerate() {
    if (!proposal) return;
    const confirmedModules = modules
      .filter((module) => module.selected)
      .map((module) => ({
        moduleName: module.proposal.moduleName,
        lectures: module.lectures
          .filter((lecture) => lecture.selected)
          .map((lecture) => ({ lectureTitle: lecture.title, taskCount: lecture.taskCount })),
      }))
      .filter((module) => module.lectures.length > 0);

    const id = await generateCourse({
      technology,
      curriculum: {
        courseName: proposal.courseName,
        description: proposal.description,
        modules: confirmedModules,
      },
    });
    setJobId(id);
    setStep("generating");
  }

  return (
    <div>
      <div className="mb-8 flex items-center gap-6">
        <StepIndicator current={step} step="input" label="Technology" />
        <span className="text-text-muted/40">—</span>
        <StepIndicator current={step} step="modules" label="Modules" />
        <span className="text-text-muted/40">—</span>
        <StepIndicator current={step} step="lectures" label="Lectures & Tasks" />
        <span className="text-text-muted/40">—</span>
        <StepIndicator current={step} step="generating" label="Generate" />
      </div>

      {step === "input" && <InputStep onPropose={handlePropose} />}

      {step === "modules" && proposal && (
        <ModulesStep
          technology={technology}
          proposal={proposal}
          modules={modules}
          onToggle={handleToggleModule}
          onAddModules={handleAddModules}
          onNext={() => setStep("lectures")}
          onBack={() => setStep("input")}
        />
      )}

      {step === "lectures" && (
        <LecturesStep
          technology={technology}
          modules={modules}
          defaultTaskCount={defaultTaskCount}
          onToggleLecture={handleToggleLecture}
          onSetTaskCount={handleSetTaskCount}
          onSetDefaultTaskCount={handleSetDefaultTaskCount}
          onAddLectures={handleAddLectures}
          onGenerate={() => void handleGenerate()}
          onBack={() => setStep("modules")}
        />
      )}

      {step === "generating" && jobId && <GeneratingStep jobId={jobId} />}

      <div className="mt-10">
        <JobsPanel />
      </div>
    </div>
  );
}
