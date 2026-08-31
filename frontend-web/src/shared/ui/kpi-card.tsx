import { type ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

export function KpiCard({
  label,
  value,
  hint,
  progress,
  icon,
  className,
}: {
  label: string;
  value: string;
  hint?: string;
  progress?: number;
  icon?: ReactNode;
  className?: string;
}) {
  const clamped = progress === undefined ? undefined : Math.min(100, Math.max(0, progress));
  return (
    <article
      className={cn(
        "flex min-h-kpi flex-col gap-space-sm rounded-radius-lg border border-border-default bg-surface-elevated p-space-md shadow-shadow-sm",
        className,
      )}
    >
      <div className="flex items-start justify-between gap-space-sm">
        <p className="text-caption text-text-secondary">{label}</p>
        {icon ? <span className="text-brand-primary">{icon}</span> : null}
      </div>
      <p className="text-h2 text-text-primary">{value}</p>
      {clamped !== undefined ? (
        <div
          className="h-1.5 w-full overflow-hidden rounded-radius-full bg-surface-subtle"
          role="progressbar"
          aria-valuenow={clamped}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label={label}
        >
          <div
            className="h-full rounded-radius-full bg-brand-primary"
            style={{ width: `${clamped}%` }}
          />
        </div>
      ) : null}
      {hint ? <p className="text-caption text-text-muted">{hint}</p> : null}
    </article>
  );
}
