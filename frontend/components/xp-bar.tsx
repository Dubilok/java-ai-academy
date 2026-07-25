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
  const progress =
    nextLevelXp > currentLevelXp
      ? Math.min(100, Math.round(((xpPoints - currentLevelXp) / (nextLevelXp - currentLevelXp)) * 100))
      : 100;

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center justify-between text-sm">
        <span className="font-semibold text-accent-java">Level {level}</span>
        <span className="text-text-muted">
          {xpPoints.toLocaleString()} / {nextLevelXp.toLocaleString()} XP
        </span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-white/10">
        <div
          className="h-full rounded-full bg-accent-java"
          style={{ width: `${progress}%` }}
          role="progressbar"
          aria-valuenow={progress}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={`XP progress: ${progress}%`}
        />
      </div>
    </div>
  );
}
