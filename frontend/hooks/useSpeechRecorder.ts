"use client";

import { useCallback, useEffect, useRef, useState } from "react";

export type RecorderState =
  | "idle"
  | "recording"
  | "processing"
  | "unavailable";

export interface UseSpeechRecorderReturn {
  state: RecorderState;
  transcript: string;
  interimTranscript: string;
  isSupported: boolean;
  startRecording: () => void;
  stopRecording: () => void;
  resetTranscript: () => void;
}

const SILENCE_TIMEOUT_MS = 2000;

// Minimal Speech Recognition API types (not in all TS DOM lib versions)
interface SpeechRecognitionAlternative {
  transcript: string;
}
interface SpeechRecognitionResult {
  readonly isFinal: boolean;
  readonly length: number;
  [index: number]: SpeechRecognitionAlternative | undefined;
}
interface SpeechRecognitionResultList {
  readonly length: number;
  [index: number]: SpeechRecognitionResult | undefined;
}
interface SpeechRecognitionResultEvent extends Event {
  readonly resultIndex: number;
  readonly results: SpeechRecognitionResultList;
}
interface SpeechRecognitionInstance extends EventTarget {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  start(): void;
  stop(): void;
  abort(): void;
  onstart: ((this: SpeechRecognitionInstance, ev: Event) => unknown) | null;
  onend: ((this: SpeechRecognitionInstance, ev: Event) => unknown) | null;
  onerror: ((this: SpeechRecognitionInstance, ev: Event) => unknown) | null;
  onresult:
    | ((
        this: SpeechRecognitionInstance,
        ev: SpeechRecognitionResultEvent,
      ) => unknown)
    | null;
}
interface SpeechRecognitionConstructor {
  new (): SpeechRecognitionInstance;
}

function getSpeechRecognitionCtor(): SpeechRecognitionConstructor | undefined {
  if (typeof window === "undefined") return undefined;
  const win = window as unknown as Record<string, unknown>;
  const ctor =
    win["SpeechRecognition"] ?? win["webkitSpeechRecognition"] ?? undefined;
  return ctor as SpeechRecognitionConstructor | undefined;
}

/**
 * Wraps the Web Speech Recognition API.
 * Auto-stops after 2 seconds of silence (no interim results).
 * Falls back to a text-input compatible state ("unavailable") when API is absent.
 */
export function useSpeechRecorder(): UseSpeechRecorderReturn {
  const [state, setState] = useState<RecorderState>("idle");
  const [transcript, setTranscript] = useState("");
  const [interimTranscript, setInterimTranscript] = useState("");
  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null);
  const silenceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const isSupported = getSpeechRecognitionCtor() != null;

  const clearSilenceTimer = useCallback(() => {
    if (silenceTimerRef.current != null) {
      clearTimeout(silenceTimerRef.current);
      silenceTimerRef.current = null;
    }
  }, []);

  const resetSilenceTimer = useCallback(
    (recognition: SpeechRecognitionInstance) => {
      clearSilenceTimer();
      silenceTimerRef.current = setTimeout(() => {
        recognition.stop();
      }, SILENCE_TIMEOUT_MS);
    },
    [clearSilenceTimer],
  );

  useEffect(() => {
    return () => {
      clearSilenceTimer();
      recognitionRef.current?.abort();
    };
  }, [clearSilenceTimer]);

  const startRecording = useCallback(() => {
    const Ctor = getSpeechRecognitionCtor();
    if (!Ctor) {
      setState("unavailable");
      return;
    }
    if (state === "recording") return;

    const recognition = new Ctor();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = "en-US";

    recognition.onstart = () => {
      setState("recording");
      resetSilenceTimer(recognition);
    };

    recognition.onresult = (event: SpeechRecognitionResultEvent) => {
      resetSilenceTimer(recognition);
      let finalText = "";
      let interimText = "";
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i];
        if (result != null) {
          const alternative = result[0];
          if (alternative != null) {
            if (result.isFinal) {
              finalText += alternative.transcript;
            } else {
              interimText += alternative.transcript;
            }
          }
        }
      }
      if (finalText) {
        setTranscript((prev) => prev + finalText);
      }
      setInterimTranscript(interimText);
    };

    recognition.onerror = () => {
      clearSilenceTimer();
      setState("idle");
    };

    recognition.onend = () => {
      clearSilenceTimer();
      setInterimTranscript("");
      setState("idle");
    };

    recognitionRef.current = recognition;
    recognition.start();
  }, [state, resetSilenceTimer, clearSilenceTimer]);

  const stopRecording = useCallback(() => {
    clearSilenceTimer();
    recognitionRef.current?.stop();
    setState("processing");
  }, [clearSilenceTimer]);

  const resetTranscript = useCallback(() => {
    setTranscript("");
    setInterimTranscript("");
  }, []);

  return {
    state,
    transcript,
    interimTranscript,
    isSupported,
    startRecording,
    stopRecording,
    resetTranscript,
  };
}
