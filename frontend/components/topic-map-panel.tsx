"use client";

interface TopicMapPanelProps {
  topicMap: Record<string, boolean>;
}

/**
 * Sidebar panel showing live topic coverage during a voice interview.
 * Green checkmark = covered, grey dot = not yet covered.
 */
export function TopicMapPanel({ topicMap }: TopicMapPanelProps) {
  const entries = Object.entries(topicMap);
  if (entries.length === 0) return null;

  const covered = entries.filter(([, isCovered]) => isCovered).length;

  return (
    <aside
      className="rounded-lg border border-white/10 bg-bg-card p-4"
      aria-label="Topic coverage"
    >
      <h2 className="mb-3 text-xs font-semibold uppercase tracking-widest text-text-muted">
        Topics
      </h2>
      <p className="mb-3 text-xs text-text-muted">
        {covered} / {entries.length} covered
      </p>
      <ul className="space-y-2" role="list">
        {entries.map(([topic, isCovered]) => (
          <li key={topic} className="flex items-center gap-2">
            <span
              className={`h-2 w-2 rounded-full flex-shrink-0 ${
                isCovered ? "bg-success" : "bg-text-muted/40"
              }`}
              aria-hidden="true"
            />
            <span
              className={`text-sm capitalize ${
                isCovered ? "text-success" : "text-text-muted"
              }`}
            >
              {topic.replace(/_/g, " ")}
            </span>
            {isCovered && (
              <span className="sr-only"> — covered</span>
            )}
          </li>
        ))}
      </ul>
    </aside>
  );
}
