import { type HTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

const variants: Record<string, string> = {
  default: "bg-surface-subtle text-text-secondary",
  success: "bg-status-success-bg text-status-success",
  warning: "bg-status-warning-bg text-status-warning",
  danger: "bg-status-danger-bg text-status-danger",
  info: "bg-status-info-bg text-status-info",
};

export function Badge({
  className,
  variant = "default",
  ...props
}: HTMLAttributes<HTMLSpanElement> & { variant?: keyof typeof variants }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-radius-full px-space-sm py-space-xs text-caption font-medium",
        variants[variant],
        className,
      )}
      {...props}
    />
  );
}
