import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { AppLayout } from "@/app/AppLayout";
import { AppProviders } from "@/app/providers";
import { AuthGuard, GuestOnly } from "@/shared/auth/AuthGuard";
import { Skeleton } from "@/shared/ui/skeleton";

const LoginPage = lazy(() =>
  import("@/features/auth/LoginPage").then((m) => ({ default: m.LoginPage })),
);
const PrimeiroAcessoPage = lazy(() =>
  import("@/features/auth/PrimeiroAcessoPage").then((m) => ({ default: m.PrimeiroAcessoPage })),
);
const DashboardPage = lazy(() =>
  import("@/features/dashboard/DashboardPage").then((m) => ({ default: m.DashboardPage })),
);

function RouteFallback() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-default p-space-lg">
      <Skeleton className="h-12 w-64" />
    </div>
  );
}

function PlaceholderPage({ title }: { title: string }) {
  return (
    <section>
      <h1 className="text-h2 text-text-primary">{title}</h1>
      <p className="mt-space-sm text-body text-text-secondary">
        Este módulo entra nas próximas sprints. O atalho existe porque a API já emitiu o link.
      </p>
    </section>
  );
}

function Root() {
  return (
    <AppProviders>
      <Outlet />
    </AppProviders>
  );
}

export const router = createBrowserRouter([
  {
    element: <Root />,
    children: [
      {
        path: "/login",
        element: (
          <GuestOnly>
            <Suspense fallback={<RouteFallback />}>
              <LoginPage />
            </Suspense>
          </GuestOnly>
        ),
      },
      {
        element: <AuthGuard allowFirstAccess />,
        children: [
          {
            path: "/primeiro-acesso",
            element: (
              <Suspense fallback={<RouteFallback />}>
                <PrimeiroAcessoPage />
              </Suspense>
            ),
          },
        ],
      },
      {
        element: <AuthGuard />,
        children: [
          {
            element: <AppLayout />,
            children: [
              {
                path: "/inicio",
                element: (
                  <Suspense fallback={<RouteFallback />}>
                    <DashboardPage />
                  </Suspense>
                ),
              },
              { path: "/solicitacoes/nova", element: <PlaceholderPage title="Nova solicitação" /> },
              { path: "/solicitacoes", element: <PlaceholderPage title="Solicitações" /> },
              { path: "/formativas", element: <PlaceholderPage title="Formativas" /> },
              { path: "/eventos", element: <PlaceholderPage title="Eventos" /> },
              { path: "/certificados", element: <PlaceholderPage title="Certificados" /> },
              { path: "/estagios", element: <PlaceholderPage title="Estágio" /> },
              { path: "/tccs", element: <PlaceholderPage title="TCC" /> },
            ],
          },
        ],
      },
      { path: "/", element: <Navigate to="/inicio" replace /> },
      { path: "*", element: <Navigate to="/inicio" replace /> },
    ],
  },
]);
