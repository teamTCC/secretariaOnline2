import { type ReactNode } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/shared/auth/AuthContext";
import { Skeleton } from "@/shared/ui/skeleton";

export function AuthGuard({ allowFirstAccess = false }: { allowFirstAccess?: boolean }) {
  const { status, needsFirstAccess } = useAuth();
  const location = useLocation();

  if (status === "boot") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface-default p-space-lg">
        <Skeleton className="h-12 w-64" />
      </div>
    );
  }

  if (status === "anon") {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (needsFirstAccess && !allowFirstAccess) {
    return <Navigate to="/primeiro-acesso" replace />;
  }

  if (!needsFirstAccess && allowFirstAccess) {
    return <Navigate to="/inicio" replace />;
  }

  return <Outlet />;
}

export function GuestOnly({ children }: { children: ReactNode }) {
  const { status, needsFirstAccess } = useAuth();

  if (status === "boot") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface-default p-space-lg">
        <Skeleton className="h-12 w-64" />
      </div>
    );
  }

  if (status === "authed" && needsFirstAccess) {
    return <Navigate to="/primeiro-acesso" replace />;
  }

  if (status === "authed") {
    return <Navigate to="/inicio" replace />;
  }

  return children;
}
