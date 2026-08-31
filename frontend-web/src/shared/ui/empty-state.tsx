import { type ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

export function EmptyState({
  title,
  description,
  className,
  action,
}: {
  title: string;
  description?: string;
  className?: string;
  action?: ReactNode;
}) {
  return (
    <div
      className={cn(
        "flex flex-col items-start gap-space-xs rounded-radius-md border border-dashed border-border-default p-space-md",
        className,
      )}
    >
      <p className="text-body font-medium text-text-primary">{title}</p>
      {description ? <p className="text-caption text-text-secondary">{description}</p> : null}
      {action}
    </div>
  );
}
