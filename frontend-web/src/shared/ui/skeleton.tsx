import { type HTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "animate-pulse rounded-radius-md bg-surface-subtle motion-reduce:animate-none",
        className,
      )}
      {...props}
    />
  );
}
