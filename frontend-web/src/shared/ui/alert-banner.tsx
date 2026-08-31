import { type HTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

const toneClass: Record<string, string> = {
  danger: "border-status-danger bg-status-danger-bg text-status-danger",
  warning: "border-status-warning bg-status-warning-bg text-status-warning",
  success: "border-status-success bg-status-success-bg text-status-success",
  info: "border-status-info bg-status-info-bg text-status-info",
};

export function AlertBanner({
  className,
  tone = "info",
  title,
  children,
  ...props
}: HTMLAttributes<HTMLDivElement> & { tone?: keyof typeof toneClass; title?: string }) {
  return (
    <div
      role="alert"
      aria-live="polite"
      className={cn(
        "rounded-radius-md border px-space-md py-space-sm text-body",
        toneClass[tone],
        className,
      )}
      {...props}
    >
      {title ? <p className="font-medium">{title}</p> : null}
      {children ? <div className={title ? "mt-space-xs text-caption" : undefined}>{children}</div> : null}
    </div>
  );
}
