import { Clock3, Inbox } from "lucide-react";
import type { DashboardKpis } from "@/shared/api/types";
import { KpiCard } from "@/shared/ui/kpi-card";
import { Skeleton } from "@/shared/ui/skeleton";

export function KpiRow({ kpis, loading }: { kpis?: DashboardKpis; loading?: boolean }) {
  if (loading || !kpis) {
    return (
      <div className="grid grid-cols-2 gap-space-md">
        {Array.from({ length: 2 }).map((_, i) => (
          <Skeleton key={i} className="min-h-kpi w-full" />
        ))}
      </div>
    );
  }
  const { horasFormativas } = kpis;
  const progress =
    horasFormativas.percentual != null
      ? Math.round(horasFormativas.percentual)
      : horasFormativas.requerido > 0
        ? Math.round((horasFormativas.atual / horasFormativas.requerido) * 100)
        : 0;
  return (
    <div className="grid grid-cols-2 gap-space-md">
      <KpiCard
        label="Horas formativas"
        value={`${horasFormativas.atual} / ${horasFormativas.requerido} h`}
        progress={progress}
        icon={<Clock3 className="h-5 w-5" aria-hidden />}
      />
      <KpiCard
        label="Atendimentos pendentes"
        value={String(kpis.atendimentosPendentes ?? 0)}
        icon={<Inbox className="h-5 w-5" aria-hidden />}
      />
    </div>
  );
}
