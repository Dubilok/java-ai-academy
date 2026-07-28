"use client";

import { useState, useCallback, useRef } from "react";
import Link from "next/link";
import { InterviewerBubble } from "@/components/interviewer-bubble";
import { TranscriptBubble } from "@/components/transcript-bubble";
import { TopicMapPanel } from "@/components/topic-map-panel";
import { useVoiceSynthesizer } from "@/hooks/useVoiceSynthesizer";
import { useSpeechRecorder } from "@/hooks/useSpeechRecorder";
import { startVoiceSession, submitVoiceAnswer, finishSession } from "@/lib/queries/interview";
import type { FinishSessionResponse } from "@/lib/schemas/interview";

type PageState =
  | { phase: "SETUP" }
  | { phase: "CONNECTING" }
  | { phase: "INTERVIEWER_SPEAKING"; sessionId: string; turnIndex: number; topicMap: Record<string, boolean>; isFinalTurn: boolean }
  | { phase: "LISTENING"; sessionId: string; turnIndex: number; topicMap: Record<string, boolean>; isFinalTurn: boolean }
  | { phase: "PROCESSING"; sessionId: string; turnIndex: number; topicMap: Record<string, boolean> }
  | { phase: "FINISHED"; report: FinishSessionResponse };

export default function VoiceInterviewPage() {
  const [technology, setTechnology] = useState("Java");
  const [pageState, setPageState] = useState<PageState>({ phase: "SETUP" });
  const [streamingQuestion, setStreamingQuestion] = useState("");
  const [isQuestionStreaming, setIsQuestionStreaming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const synthesizer = useVoiceSynthesizer();
  const recorder = useSpeechRecorder();

  const startTimer = useCallback(() => {
    setElapsedSeconds(0);
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
    }, 1000);
  }, []);

  const stopTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const handleNewQuestion = useCallback(
    (question: string, topicMap: Record<string, boolean>, sessionId: string, turnIndex: number, isFinalTurn: boolean) => {
      setStreamingQuestion("");
      setIsQuestionStreaming(false);
      synthesizer.cancel();
      setPageState({ phase: "INTERVIEWER_SPEAKING", sessionId, turnIndex, topicMap, isFinalTurn });
      if (synthesizer.isSupported) {
        synthesizer.speak(question);
      }
    },
    [synthesizer],
  );

  const handleStart = useCallback(async () => {
    setError(null);
    setPageState({ phase: "CONNECTING" });
    try {
      const session = await startVoiceSession(technology, 5);
      const firstQuestion = session.firstVoiceQuestion ?? "";
      const sessionId = session.sessionId;
      setStreamingQuestion(firstQuestion);
      handleNewQuestion(firstQuestion, {}, sessionId, 0, false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to start session");
      setPageState({ phase: "SETUP" });
    }
  }, [technology, handleNewQuestion]);

  const handleStartListening = useCallback(() => {
    if (pageState.phase !== "INTERVIEWER_SPEAKING") return;
    synthesizer.cancel();
    recorder.resetTranscript();
    startTimer();
    setPageState({
      phase: "LISTENING",
      sessionId: pageState.sessionId,
      turnIndex: pageState.turnIndex,
      topicMap: pageState.topicMap,
      isFinalTurn: pageState.isFinalTurn,
    });
    recorder.startRecording();
  }, [pageState, synthesizer, recorder, startTimer]);

  const handleSubmitAnswer = useCallback(async () => {
    if (pageState.phase !== "LISTENING") return;
    stopTimer();
    recorder.stopRecording();

    const { sessionId, turnIndex, topicMap } = pageState;
    const transcript = recorder.transcript + recorder.interimTranscript;

    setPageState({ phase: "PROCESSING", sessionId, turnIndex, topicMap });
    setStreamingQuestion("");
    setIsQuestionStreaming(true);
    setError(null);

    try {
      const result = await submitVoiceAnswer(
        sessionId,
        transcript,
        turnIndex,
        elapsedSeconds,
        (token) => setStreamingQuestion((prev) => prev + token),
      );
      setIsQuestionStreaming(false);

      if (result.isFinalTurn) {
        // Ask to finish
        setPageState({
          phase: "INTERVIEWER_SPEAKING",
          sessionId,
          turnIndex: turnIndex + 1,
          topicMap: result.topicMap,
          isFinalTurn: true,
        });
        setStreamingQuestion(result.question);
        if (synthesizer.isSupported) synthesizer.speak(result.question);
      } else {
        handleNewQuestion(result.question, result.topicMap, sessionId, turnIndex + 1, false);
        setStreamingQuestion(result.question);
      }
    } catch (err) {
      setIsQuestionStreaming(false);
      setError(err instanceof Error ? err.message : "Failed to submit answer");
      setPageState({ phase: "INTERVIEWER_SPEAKING", sessionId, turnIndex, topicMap: pageState.topicMap, isFinalTurn: false });
    }
  }, [pageState, recorder, elapsedSeconds, synthesizer, handleNewQuestion, stopTimer]);

  const handleFinish = useCallback(async () => {
    if (pageState.phase !== "INTERVIEWER_SPEAKING" && pageState.phase !== "LISTENING") return;
    const sessionId = "sessionId" in pageState ? pageState.sessionId : "";
    stopTimer();
    synthesizer.cancel();
    setError(null);
    try {
      const report = await finishSession(sessionId);
      setPageState({ phase: "FINISHED", report });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to finish session");
    }
  }, [pageState, synthesizer, stopTimer]);

  const isRecording = recorder.state === "recording";
  const topicMap =
    "topicMap" in pageState && pageState.topicMap ? pageState.topicMap : {};

  // ── FINISHED ────────────────────────────────────────────────────────────────
  if (pageState.phase === "FINISHED") {
    const { report } = pageState;
    return (
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-3xl px-4 py-12">
        <h1 className="mb-2 text-2xl font-bold text-text-primary">Interview Complete</h1>
        <p className="mb-6 text-text-muted">
          Overall score:{" "}
          <span className="font-bold text-success">{report.overallScore}/100</span>
        </p>
        <div className="space-y-3">
          {report.report?.dimensions?.map((dim) => (
            <div key={dim.name} className="rounded-lg border border-white/10 bg-bg-card p-4">
              <div className="mb-1 flex items-center justify-between">
                <span className="text-sm font-medium capitalize text-text-primary">
                  {dim.name.replace(/([A-Z])/g, " $1").trim()}
                </span>
                <span className="text-sm font-bold text-accent-blue">{dim.score}/100</span>
              </div>
              <p className="text-xs text-text-muted">{dim.feedback}</p>
            </div>
          ))}
        </div>
        <Link
          href="/interview"
          className="mt-8 inline-block rounded-lg bg-accent-blue px-6 py-2 text-sm font-medium text-white hover:bg-accent-blue/80"
        >
          Back to Interview Hub
        </Link>
      </main>
    );
  }

  // ── SETUP ───────────────────────────────────────────────────────────────────
  if (pageState.phase === "SETUP") {
    return (
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-xl px-4 py-12">
        <h1 className="mb-2 text-2xl font-bold text-text-primary">Voice Interview</h1>
        <p className="mb-8 text-text-muted">
          Practice speaking your answers aloud. The AI interviewer will ask follow-up questions
          based on your responses.
        </p>
        {error && (
          <p role="alert" className="mb-4 rounded-lg bg-error/10 px-4 py-3 text-sm text-error">
            {error}
          </p>
        )}
        <div className="space-y-4">
          <div>
            <label htmlFor="technology" className="mb-1 block text-sm font-medium text-text-muted">
              Technology
            </label>
            <input
              id="technology"
              type="text"
              value={technology}
              onChange={(e) => setTechnology(e.target.value)}
              className="w-full rounded-lg border border-white/10 bg-bg-card px-3 py-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-blue"
              placeholder="e.g. Java, Spring Boot, Kafka"
            />
          </div>
          <button
            onClick={handleStart}
            disabled={!technology.trim()}
            className="w-full rounded-lg bg-accent-java px-6 py-3 text-sm font-semibold text-white disabled:opacity-50"
          >
            Start Voice Interview
          </button>
        </div>
      </main>
    );
  }

  // ── CONNECTING ──────────────────────────────────────────────────────────────
  if (pageState.phase === "CONNECTING") {
    return (
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-xl px-4 py-12 text-center">
        <p className="text-text-muted" role="status" aria-live="polite">Starting interview…</p>
      </main>
    );
  }

  // ── ACTIVE SESSION ──────────────────────────────────────────────────────────
  const isListening = pageState.phase === "LISTENING";
  const isProcessing = pageState.phase === "PROCESSING";
  const isFinalTurn = "isFinalTurn" in pageState && pageState.isFinalTurn;

  return (
    <main id="main-content" tabIndex={-1} className="mx-auto max-w-4xl px-4 py-8">
      <div className="flex gap-6">
        {/* Chat area */}
        <div className="flex flex-1 flex-col gap-6">
          <div className="flex items-center justify-between">
            <h1 className="text-lg font-bold text-text-primary">
              Voice Interview — <span className="text-text-muted">{technology}</span>
            </h1>
            {(isListening || "isFinalTurn" in pageState) && (
              <span className="text-sm tabular-nums text-text-muted">
                {Math.floor(elapsedSeconds / 60)}:{String(elapsedSeconds % 60).padStart(2, "0")}
              </span>
            )}
          </div>

          {error && (
            <p role="alert" className="rounded-lg bg-error/10 px-4 py-3 text-sm text-error">
              {error}
            </p>
          )}

          <InterviewerBubble
            streamingText={streamingQuestion}
            isStreaming={isQuestionStreaming}
            onSentenceBoundary={synthesizer.isSupported ? synthesizer.speak : undefined}
          />

          <TranscriptBubble
            transcript={recorder.transcript}
            interimTranscript={recorder.interimTranscript}
            isRecording={isRecording}
          />

          {/* Controls */}
          <div className="flex gap-3">
            {pageState.phase === "INTERVIEWER_SPEAKING" && !isFinalTurn && (
              <button
                onClick={handleStartListening}
                className="flex-1 rounded-lg bg-success px-4 py-3 text-sm font-semibold text-white"
              >
                {recorder.isSupported ? "Start Speaking" : "Type Your Answer"}
              </button>
            )}

            {isListening && (
              <button
                onClick={handleSubmitAnswer}
                className="flex-1 rounded-lg bg-accent-blue px-4 py-3 text-sm font-semibold text-white"
              >
                Submit Answer
              </button>
            )}

            {isProcessing && (
              <div className="flex flex-1 items-center justify-center rounded-lg bg-bg-card px-4 py-3">
                <span className="text-sm text-text-muted" role="status" aria-live="polite">
                  Generating next question…
                </span>
              </div>
            )}

            {isFinalTurn && pageState.phase === "INTERVIEWER_SPEAKING" && (
              <button
                onClick={handleFinish}
                className="flex-1 rounded-lg bg-accent-java px-4 py-3 text-sm font-semibold text-white"
              >
                Finish & Get Evaluation
              </button>
            )}
          </div>
        </div>

        {/* Topic map sidebar */}
        <div className="hidden w-56 lg:block">
          <TopicMapPanel topicMap={topicMap} />
        </div>
      </div>
    </main>
  );
}
