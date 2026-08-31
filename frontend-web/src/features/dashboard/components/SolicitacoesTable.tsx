import type { SolicitacaoItem } from "@/shared/api/types";
import { Badge } from "@/shared/ui/badge";
import { EmptyState } from "@/shared/ui/empty-state";
import { Skeleton } from "@/shared/ui/skeleton";

function estadoVariant(estado: string) {
  const key = estado.toUpperCase();
  if (["DEFERIDA", "APROVADA"].includes(key)) return "success" as const;
  if (["INDEFERIDA", "REJEITADA"].includes(key)) return "danger" as const;
  if (["EM_AJUSTE"].includes(key)) return "warning" as const;
  return "info" as const;
}

function formatWhen(iso?: string | null) {
  if (!iso) return "—";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" }).format(date);
}

export function SolicitacoesTable({
  items,
  loading,
}: {
  items?: SolicitacaoItem[];
  loading?: boolean;
}) {
  if (loading) {
    return (
      <div className="flex flex-col gap-space-sm">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    );
  }
  if (!items || items.length === 0) {
    return <EmptyState title="Você ainda não abriu solicitações." />;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-caption">
        <thead>
          <tr className="border-b border-border-default text-text-secondary">
            <th className="py-space-sm font-medium">Identificador</th>
            <th className="py-space-sm font-medium">Tipo</th>
            <th className="py-space-sm font-medium">Estado</th>
            <th className="py-space-sm font-medium">Aberta em</th>
          </tr>
        </thead>
        <tbody>
          {items.map((row) => (
            <tr key={row.id} className="border-b border-border-default last:border-0">
              <td className="py-space-sm text-text-primary">{row.id.slice(0, 8)}</td>
              <td className="py-space-sm text-text-secondary">{row.tipo.replaceAll("_", " ")}</td>
              <td className="py-space-sm">
                <Badge variant={estadoVariant(row.estado)}>{row.estado.replaceAll("_", " ")}</Badge>
              </td>
              <td className="py-space-sm text-text-secondary">{formatWhen(row.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
