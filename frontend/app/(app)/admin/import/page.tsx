"use client";

import { useQuery, useMutation } from "@tanstack/react-query";
import { useState } from "react";
import Link from "next/link";
import { fetchAdminCourses, fetchAdminModules, importContent } from "@/lib/queries/admin";

const SYSTEM_PROMPT = `You are the Content Architect for the Java AI Academy platform. Your sole purpose is to generate high-quality, original Java curriculum content in a precise JSON format.

Given a technology topic (e.g., "Java 21 Records", "Spring Boot REST"), produce ONE lecture paired with ONE programming task. The output must be valid, self-contained, and ready for automated quality verification.

Respond with ONLY the following JSON object — no markdown fences, no preamble, no explanation:

{
  "lecture": {
    "title": "<concise title, max 80 chars>",
    "contentMarkdown": "<lecture body in Markdown, minimum 500 words>"
  },
  "task": {
    "title": "<task title, max 80 chars>",
    "description": "<what the student must implement, min 100 chars>",
    "difficulty": "<EASY | MEDIUM | HARD>",
    "xpReward": <integer 50-500>
  },
  "templateCode": "<starter Java class named Solution with TODO comments>",
  "solutionCode": "<complete reference solution>",
  "testCode": "<complete JUnit 5 test class named TaskTest that imports and tests Solution>"
}

SANDBOX CLASSPATH — CRITICAL CONSTRAINT:
The verification sandbox has ONLY Java 21 stdlib (java.*, javax.*) and JUnit 5 (org.junit.jupiter.*).
No external JARs: no Hibernate, Spring, Jackson, Guava, Lombok, or anything outside the JDK + JUnit 5.
Any non-stdlib import causes a compile failure.

FOR FRAMEWORK TOPICS (Hibernate, Spring, Kafka, etc.):
- The LECTURE may freely explain the framework using real code examples.
- The TASK must test the UNDERLYING JAVA CONCEPT using only stdlib + JUnit 5.
Examples: "Hibernate Entity Mapping" → implement an in-memory HashMap repository.
"Spring DI" → implement a constructor-injection container with a Map.
"Spring @Transactional" → implement a unit-of-work that rolls back on exception.

CONTENT RULES:
Lecture: Write original content only. Use clear progressive explanations. All code examples must compile with Java 21. Use Markdown with ## headings, fenced code blocks. Minimum 500 words of prose.
Task: The task must exercise the lecture concept. templateCode must be a valid Java class named Solution with a method stub and // TODO comments. solutionCode must be a complete working Solution class that passes all tests. testCode must be a complete JUnit 5 class named TaskTest with at minimum 3 @Test methods.
Difficulty: EASY = single method, simple logic, 50-100 XP. MEDIUM = multiple methods, collections, 150-300 XP. HARD = concurrency, generics, design patterns, 350-500 XP.
ALL imports in templateCode, solutionCode, and testCode must be from java.*, javax.*, or org.junit.jupiter.* only.`;

export default function ImportPage() {
  const [selectedCourseId, setSelectedCourseId] = useState("");
  const [selectedModuleId, setSelectedModuleId] = useState("");
  const [rawJson, setRawJson] = useState("");
  const [promptCopied, setPromptCopied] = useState(false);

  const { data: courses = [] } = useQuery({
    queryKey: ["admin-courses"],
    queryFn: fetchAdminCourses,
  });

  const { data: modules = [] } = useQuery({
    queryKey: ["admin-modules", selectedCourseId],
    queryFn: () => fetchAdminModules(selectedCourseId),
    enabled: selectedCourseId !== "",
  });

  const mutation = useMutation({
    mutationFn: () => importContent(selectedModuleId, rawJson.trim()),
  });

  async function handleCopyPrompt() {
    await navigator.clipboard.writeText(SYSTEM_PROMPT);
    setPromptCopied(true);
    setTimeout(() => setPromptCopied(false), 2000);
  }

  function handleImport() {
    mutation.reset();
    mutation.mutate();
  }

  const isDisabled = !selectedModuleId || !rawJson.trim() || mutation.isPending;

  const inputClass =
    "rounded-lg border border-white/10 bg-bg-base px-3 py-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-blue";

  return (
    <div className="space-y-8">
      {/* Step 1 — Get the prompt */}
      <section className="rounded-xl bg-bg-card p-6">
        <h2 className="mb-1 text-base font-semibold text-text-primary">
          Step 1 — Generate content in Claude.ai (free, no API cost)
        </h2>
        <p className="mb-4 text-sm text-text-muted">
          Open{" "}
          <a
            href="https://claude.ai"
            target="_blank"
            rel="noreferrer"
            className="text-accent-blue hover:underline"
          >
            claude.ai
          </a>
          , start a new chat, paste the system prompt below as your first message, then describe the
          topic you want (e.g.{" "}
          <span className="font-mono text-text-primary">&quot;Java 21 Records&quot;</span>). Copy
          the JSON response and paste it in Step 3.
        </p>
        <button
          onClick={() => void handleCopyPrompt()}
          className="rounded-lg bg-accent-blue px-4 py-2 text-sm font-semibold text-white hover:opacity-90"
        >
          {promptCopied ? "✓ Copied!" : "Copy system prompt"}
        </button>
        <details className="mt-3">
          <summary className="cursor-pointer text-xs text-text-muted hover:text-text-primary">
            Preview prompt
          </summary>
          <pre className="mt-2 max-h-48 overflow-y-auto rounded-lg bg-bg-base p-3 text-xs text-text-muted">
            {SYSTEM_PROMPT}
          </pre>
        </details>
      </section>

      {/* Step 2 — Pick target module */}
      <section className="rounded-xl bg-bg-card p-6">
        <h2 className="mb-4 text-base font-semibold text-text-primary">
          Step 2 — Choose where to import
        </h2>
        <div className="flex flex-col gap-3">
          <select
            value={selectedCourseId}
            onChange={(e) => {
              setSelectedCourseId(e.target.value);
              setSelectedModuleId("");
            }}
            className={inputClass + " w-full max-w-md"}
          >
            <option value="">— Select course —</option>
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.title} ({course.technology})
              </option>
            ))}
          </select>

          {selectedCourseId && (
            <select
              value={selectedModuleId}
              onChange={(e) => setSelectedModuleId(e.target.value)}
              className={inputClass + " w-full max-w-md"}
            >
              <option value="">— Select module —</option>
              {modules.map((mod) => (
                <option key={mod.id} value={mod.id}>
                  {mod.orderIndex}. {mod.title}
                </option>
              ))}
            </select>
          )}
        </div>
      </section>

      {/* Step 3 — Paste JSON */}
      <section className="rounded-xl bg-bg-card p-6">
        <h2 className="mb-4 text-base font-semibold text-text-primary">
          Step 3 — Paste the JSON from Claude.ai
        </h2>
        <textarea
          value={rawJson}
          onChange={(e) => setRawJson(e.target.value)}
          rows={14}
          placeholder={'{\n  "lecture": { ... },\n  "task": { ... },\n  "templateCode": "...",\n  "solutionCode": "...",\n  "testCode": "..."\n}'}
          className={inputClass + " w-full font-mono text-xs leading-relaxed"}
          aria-label="Paste generated JSON here"
        />

        <div className="mt-4 flex items-center gap-4">
          <button
            onClick={handleImport}
            disabled={isDisabled}
            className="rounded-lg bg-accent-java px-5 py-2.5 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
          >
            {mutation.isPending ? "Verifying in sandbox…" : "Import & verify"}
          </button>
          {mutation.isPending && (
            <span className="text-xs text-text-muted">
              Running sandbox check — this takes a few seconds…
            </span>
          )}
        </div>

        {/* Success */}
        {mutation.isSuccess && (
          <div className="mt-4 rounded-lg border border-success/30 bg-success/10 p-4">
            <p className="font-semibold text-success">✓ Content imported and sandbox-verified!</p>
            <p className="mt-1 text-sm text-text-muted">
              Lecture ID:{" "}
              <span className="font-mono text-text-primary">{mutation.data.lectureId}</span>
            </p>
            <Link
              href={`/courses/${mutation.data.courseId}`}
              className="mt-2 inline-block text-sm text-accent-blue hover:underline"
            >
              View course →
            </Link>
          </div>
        )}

        {/* Error */}
        {mutation.isError && (
          <div className="mt-4 rounded-lg border border-error/30 bg-error/10 p-4">
            <p className="font-semibold text-error">Import failed</p>
            <pre className="mt-1 max-h-48 overflow-y-auto whitespace-pre-wrap text-xs text-text-muted">
              {mutation.error instanceof Error
                ? mutation.error.message
                : "Unknown error — check the backend logs."}
            </pre>
          </div>
        )}
      </section>
    </div>
  );
}
