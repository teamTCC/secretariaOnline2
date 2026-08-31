import type { EventoItem } from "@/shared/api/types";
import { Badge } from "@/shared/ui/badge";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

function formatWhen(iso?: string | null) {
  if (!iso) return "Data a definir";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

export function EventosList({ items, loading }: { items?: EventoItem[]; loading?: boolean }) {
  if (loading) {
    return (
      <div className="flex flex-col gap-space-sm">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-16 w-full" />
        ))}
      </div>
    );
  }
  if (!items || items.length === 0) {
    return <EmptyState title="Nenhum evento próximo." />;
  }
  return (
    <ul className="flex flex-col gap-space-sm">
      {items.map((evento) => (
        <li
          key={evento.id}
          className="flex items-start justify-between gap-space-md rounded-radius-md border border-border-default p-space-md"
        >
          <div>
            <p className="text-body text-text-primary">{evento.titulo}</p>
            <p className="text-caption text-text-secondary">{formatWhen(evento.fimEm)}</p>
          </div>
          <Badge variant="info">{evento.chCreditadas} h</Badge>
        </li>
      ))}
    </ul>
  );
}
