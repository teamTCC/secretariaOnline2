import { Menu, X } from "lucide-react";
import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useActions } from "@/shared/api/hateoas";
import { useAuth } from "@/shared/auth/AuthContext";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/cn";

function initials(nome: string) {
  return nome
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

export function AppLayout() {
  const { me, logout } = useAuth();
  const actions = useActions(me ?? undefined);
  const [open, setOpen] = useState(false);

  return (
    <div className="flex min-h-screen bg-surface-default">
      {open ? (
        <button
          type="button"
          className="fixed inset-0 z-30 bg-surface-overlay lg:hidden"
          aria-label="Fechar menu"
          onClick={() => setOpen(false)}
        />
      ) : null}
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-border-default bg-surface-elevated transition-transform lg:static lg:translate-x-0",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div className="flex h-16 items-center justify-between px-space-lg">
          <p className="text-body font-semibold text-brand-primary">SecretariaOnline2</p>
          <button
            type="button"
            className="lg:hidden"
            aria-label="Fechar navegação"
            onClick={() => setOpen(false)}
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <nav className="flex-1 overflow-y-auto px-space-sm py-space-md">
          {actions.can("dashboard") ? (
            <NavLink
              to="/inicio"
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                cn(
                  "flex min-h-touch items-center rounded-radius-md px-space-md text-body",
                  isActive
                    ? "bg-surface-subtle font-medium text-brand-primary"
                    : "text-text-secondary hover:bg-surface-subtle",
                )
              }
            >
              Início
            </NavLink>
          ) : null}
        </nav>
        <div className="flex h-16 items-center justify-between border-t border-border-default px-space-md">
          <div className="flex items-center gap-space-sm">
            <span
              className="flex h-8 w-8 items-center justify-center rounded-radius-full bg-brand-primary text-caption text-text-inverse"
              aria-hidden
            >
              {initials(me?.nome ?? "A")}
            </span>
            <span className="max-w-32 truncate text-caption text-text-primary">{me?.nome}</span>
          </div>
          <Button variant="ghost" onClick={() => void logout()}>
            Sair
          </Button>
        </div>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border-default bg-surface-default px-space-lg">
          <div className="flex items-center gap-space-md">
            <button
              type="button"
              className="lg:hidden"
              aria-label="Abrir navegação"
              onClick={() => setOpen(true)}
            >
              <Menu className="h-5 w-5" />
            </button>
            <h2 className="text-h3 text-text-primary">Início</h2>
          </div>
        </header>
        <main className="flex-1 overflow-y-auto p-space-lg">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
