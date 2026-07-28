"use client";

interface TranscriptBubbleProps {
  /** Committed (final) transcript text. */
  transcript: string;
  /** Interim (in-progress) transcript text shown in muted style. */
  interimTranscript: string;
  /** Whether the microphone is active. */
  isRecording: boolean;
}

/**
 * Chat bubble for the candidate's spoken transcript.
 * Displays committed transcript in full colour and interim text in muted style.
 * aria-live=polite announces updates to screen-reader users.
 */
export function TranscriptBubble({
  transcript,
  interimTranscript,
  isRecording,
}: TranscriptBubbleProps) {
  const hasContent = transcript || interimTranscript;

  return (
    <div className="flex items-start justify-end gap-3">
      <div
        className="max-w-prose rounded-2xl rounded-tr-none bg-accent-java/10 px-4 py-3"
        aria-live="polite"
        aria-label="Your response transcript"
      >
        {hasContent ? (
          <p className="text-sm leading-relaxed text-text-primary">
            {transcript}
            {interimTranscript && (
              <span className="text-text-muted"> {interimTranscript}</span>
            )}
          </p>
        ) : (
          <p className="text-sm italic text-text-muted">
            {isRecording ? "Listening…" : "Your answer will appear here."}
          </p>
        )}
      </div>

      {/* Avatar */}
      <div
        className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-accent-java/20 text-sm font-bold text-accent-java"
        aria-hidden="true"
      >
        You
      </div>
    </div>
  );
}
