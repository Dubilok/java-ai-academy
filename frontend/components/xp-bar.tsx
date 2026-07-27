interface XpBarProps {
  xpPoints: number;
  level: number;
}

function xpForLevel(level: number): number {
  return (level - 1) * (level - 1) * 100;
}

export function XpBar({ xpPoints, level }: XpBarProps) {
  const currentLevelXp = xpForLevel(level);
  const nextLevelXp = xpForLevel(level + 1);
  const toNext = Math.max(0, nextLevelXp - xpPoints);
  const progress =
    nextLevelXp > currentLevelXp
      ? Math.min(100, Math.round(((xpPoints - currentLevelXp) / (nextLevelXp - currentLevelXp)) * 100))
      : 100;

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between text-xs">
        <span className="font-semibold text-accent-java">Level {level}</span>
        <span className="text-text-muted">
          {toNext > 0 ? `${toNext.toLocaleString()} XP to level ${level + 1}` : "Max level"}
        </span>
      </div>
      <div
        className="relative h-2.5 w-full overflow-hidden rounded-full bg-white/10"
        role="progressbar"
        aria-valuenow={progress}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`XP progress to level ${level + 1}: ${progress}%`}
      >
        <div
          className="h-full rounded-full transition-all duration-700"
          style={{
            width: `${progress}%`,
            background: "linear-gradient(90deg, #EA580C, #3B82F6)",
          }}
        />
      </div>
      <div className="flex items-center justify-between text-xs text-text-muted">
        <span>{xpPoints.toLocaleString()} XP</span>
        <span>{nextLevelXp.toLocaleString()} XP</span>
      </div>
    </div>
  );
}
