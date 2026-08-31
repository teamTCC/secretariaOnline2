import { type LabelHTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

export function Label({ className, ...props }: LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    <label className={cn("text-caption font-medium text-text-secondary", className)} {...props} />
  );
}
