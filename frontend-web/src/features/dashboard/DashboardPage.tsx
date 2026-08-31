import type { ResourceWithLinks } from "@/shared/api/hateoas";
import { useAuth } from "@/shared/auth/AuthContext";
import { AlertBanner } from "@/shared/ui/alert-banner";
import { Card, CardHeader, CardTitle } from "@/shared/ui/card";
import { AtalhosTiles } from "@/features/dashboard/components/AtalhosTiles";
import { EventosList } from "@/features/dashboard/components/EventosList";
import { KpiRow } from "@/features/dashboard/components/KpiRow";
import { PendenciasList } from "@/features/dashboard/components/PendenciasList";
import { SolicitacoesTable } from "@/features/dashboard/components/SolicitacoesTable";
import { useDashboardAluno } from "@/features/dashboard/useDashboardAluno";

export function DashboardPage() {
  const { me } = useAuth();
  const query = useDashboardAluno();
  const data = query.data;
  const loading = query.isLoading;
  const atalhosResource: ResourceWithLinks | undefined = data
    ? {
        _links: Object.fromEntries(
          Object.entries(data._links).filter((entry): entry is [string, string] => Boolean(entry[1])),
        ),
      }
    : undefined;

  const curso =
    me?.metadata && typeof me.metadata === "object" && "curso" in me.metadata
      ? String(me.metadata.curso)
      : null;

  return (
    <div className="flex flex-col gap-space-lg">
      <header>
        <h1 className="text-h1 text-text-primary">{me ? `Olá, ${me.nome}` : "Olá"}</h1>
        <p className="text-caption text-text-secondary">
          {[curso, me?.grr].filter(Boolean).join(" · ") || "Painel do aluno"}
        </p>
      </header>

      {query.isError ? (
        <AlertBanner tone="danger" title="Não foi possível carregar o painel.">
          Tente novamente em instantes.
        </AlertBanner>
      ) : null}

      {data?._degraded ? (
        <AlertBanner tone="warning" title="Alguns dados do painel estão temporariamente indisponíveis." />
      ) : null}

      <KpiRow kpis={data?.kpis} loading={loading} />

      <div className="flex flex-col gap-space-lg lg:flex-row">
        <div className="flex min-w-0 flex-[2] flex-col gap-space-lg">
          <section aria-labelledby="pendencias-heading">
            <Card>
              <CardHeader>
                <CardTitle id="pendencias-heading">Pendências</CardTitle>
              </CardHeader>
              <PendenciasList items={data?.pendencias ?? undefined} loading={loading} />
            </Card>
          </section>

          <section aria-labelledby="solicitacoes-heading">
            <Card>
              <CardHeader>
                <CardTitle id="solicitacoes-heading">Últimas solicitações</CardTitle>
              </CardHeader>
              <SolicitacoesTable items={data?.ultimasSolicitacoes ?? undefined} loading={loading} />
            </Card>
          </section>

          <section aria-labelledby="eventos-heading">
            <Card>
              <CardHeader>
                <CardTitle id="eventos-heading">Próximos eventos</CardTitle>
              </CardHeader>
              <EventosList items={data?.eventos ?? undefined} loading={loading} />
            </Card>
          </section>
        </div>

        <aside className="flex min-w-0 flex-1 flex-col gap-space-lg">
          <AtalhosTiles resource={atalhosResource} />
        </aside>
      </div>
    </div>
  );
}
