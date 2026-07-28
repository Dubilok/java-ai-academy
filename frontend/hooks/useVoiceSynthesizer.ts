"use client";

import { useCallback, useEffect, useRef, useState } from "react";

export type SynthState = "idle" | "speaking" | "paused";

export interface UseVoiceSynthesizerReturn {
  state: SynthState;
  speak: (text: string) => void;
  cancel: () => void;
  isSupported: boolean;
}

/**
 * Wraps SpeechSynthesisUtterance to play text-to-speech.
 * Splits on sentence boundaries (. ? !) to trigger incrementally as tokens arrive.
 * Degrades gracefully when the Web Speech API is unavailable.
 */
export function useVoiceSynthesizer(): UseVoiceSynthesizerReturn {
  const [state, setState] = useState<SynthState>("idle");
  const isSupported =
    typeof window !== "undefined" && "speechSynthesis" in window;
  const utteranceRef = useRef<SpeechSynthesisUtterance | null>(null);

  useEffect(() => {
    return () => {
      if (isSupported) {
        window.speechSynthesis.cancel();
      }
    };
  }, [isSupported]);

  const speak = useCallback(
    (text: string) => {
      if (!isSupported || !text.trim()) return;

      window.speechSynthesis.cancel();

      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = 1.0;
      utterance.pitch = 1.0;
      utterance.lang = "en-US";

      utterance.onstart = () => setState("speaking");
      utterance.onend = () => setState("idle");
      utterance.onerror = () => setState("idle");
      utterance.onpause = () => setState("paused");
      utterance.onresume = () => setState("speaking");

      utteranceRef.current = utterance;
      setState("speaking");
      window.speechSynthesis.speak(utterance);
    },
    [isSupported],
  );

  const cancel = useCallback(() => {
    if (!isSupported) return;
    window.speechSynthesis.cancel();
    setState("idle");
  }, [isSupported]);

  return { state, speak, cancel, isSupported };
}
