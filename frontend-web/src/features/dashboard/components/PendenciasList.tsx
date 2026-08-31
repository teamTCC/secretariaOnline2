import type { PendenciaItem } from "@/shared/api/types";
import { Badge } from "@/shared/ui/badge";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

function estadoVariant(estado?: string) {
  if (!estado) return "default" as const;
  if (["DEFERIDA", "APROVADA"].includes(estado.toUpperCase())) return "success" as const;
  if (["INDEFERIDA", "REJEITADA"].includes(estado.toUpperCase())) return "danger" as const;
  if (["EM_AJUSTE", "AGUARDANDO_CIENCIA"].includes(estado.toUpperCase())) return "warning" as const;
  return "info" as const;
}

function formatPrazo(iso?: string | null) {
  if (!iso) return null;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" }).format(date);
}

export function PendenciasList({
  items,
  loading,
}: {
  items?: PendenciaItem[];
  loading?: boolean;
}) {
  if (loading) {
    return (
      <div className="flex flex-col gap-space-sm">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full" />
        ))}
      </div>
    );
  }
  if (!items || items.length === 0) {
    return <EmptyState title="Nenhuma pendência no momento." />;
  }
  return (
    <ul>
      {items.map((item) => (
        <li
          key={item.id}
          className="flex items-start justify-between gap-space-md border-b border-border-default py-space-sm last:border-0"
        >
          <div className="flex flex-col gap-space-xs">
            <p className="text-body text-text-primary">{item.tipo.replaceAll("_", " ")}</p>
            {item.prazoEm ? (
              <p className="text-caption text-text-secondary">Prazo {formatPrazo(item.prazoEm)}</p>
            ) : null}
            <Badge variant={estadoVariant(item.estado)}>{item.estado.replaceAll("_", " ")}</Badge>
          </div>
          {item.acao ? <span className="text-caption text-text-muted">{item.acao}</span> : null}
        </li>
      ))}
    </ul>
  );
}
