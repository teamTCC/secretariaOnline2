import {
  Award,
  Briefcase,
  CalendarDays,
  ClipboardList,
  FilePlus,
  GraduationCap,
} from "lucide-react";
import type { ReactNode } from "react";
import { useActions, type ResourceWithLinks } from "@/shared/api/hateoas";
import { Card, CardHeader, CardTitle } from "@/shared/ui/card";

const TILES: Array<{ rel: string; label: string; icon: ReactNode }> = [
  { rel: "novaSolicitacao", label: "Nova solicitação", icon: <FilePlus className="h-6 w-6" aria-hidden /> },
  { rel: "solicitacoes", label: "Minhas solicitações", icon: <ClipboardList className="h-6 w-6" aria-hidden /> },
  { rel: "formativas", label: "Horas formativas", icon: <GraduationCap className="h-6 w-6" aria-hidden /> },
  { rel: "eventos", label: "Eventos", icon: <CalendarDays className="h-6 w-6" aria-hidden /> },
  { rel: "certificados", label: "Certificados", icon: <Award className="h-6 w-6" aria-hidden /> },
  { rel: "estagio", label: "Estágio", icon: <Briefcase className="h-6 w-6" aria-hidden /> },
];

export function AtalhosTiles({ resource }: { resource?: ResourceWithLinks }) {
  const actions = useActions(resource);
  const visible = TILES.filter((tile) => actions.can(tile.rel));
  if (visible.length === 0) return null;
  return (
    <Card>
      <CardHeader>
        <CardTitle>Atalhos</CardTitle>
      </CardHeader>
      <div className="grid grid-cols-2 gap-space-sm">
        {visible.map((tile) => (
          <button
            key={tile.rel}
            type="button"
            title="Disponível na próxima versão"
            className="flex min-h-touch flex-col items-start gap-space-xs rounded-radius-md border border-border-default bg-surface-elevated p-space-sm text-left text-caption text-text-primary hover:bg-surface-subtle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary"
          >
            <span className="text-brand-primary">{tile.icon}</span>
            {tile.label}
          </button>
        ))}
      </div>
    </Card>
  );
}
