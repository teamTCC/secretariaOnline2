import * as CheckboxPrimitive from "@radix-ui/react-checkbox";
import { Check } from "lucide-react";
import { cn } from "@/shared/lib/cn";

export function Checkbox({
  className,
  ...props
}: CheckboxPrimitive.CheckboxProps) {
  return (
    <CheckboxPrimitive.Root
      className={cn(
        "flex h-5 w-5 shrink-0 items-center justify-center rounded-radius-sm border border-border-strong bg-surface-elevated focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary data-[state=checked]:bg-brand-primary data-[state=checked]:text-text-inverse",
        className,
      )}
      {...props}
    >
      <CheckboxPrimitive.Indicator>
        <Check className="h-3.5 w-3.5" aria-hidden />
      </CheckboxPrimitive.Indicator>
    </CheckboxPrimitive.Root>
  );
}
