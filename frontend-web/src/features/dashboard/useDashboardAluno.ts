import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import type { DashboardAlunoResponse } from "@/shared/api/types";
import { useAuth } from "@/shared/auth/AuthContext";
import { dashboardKeys } from "@/features/dashboard/queryKeys";

export function useDashboardAluno() {
  const { session, needsFirstAccess } = useAuth();
  return useQuery({
    queryKey: dashboardKeys.aluno(),
    queryFn: async () => {
      const res = await apiClient.get<DashboardAlunoResponse>("/bff/dashboard/aluno");
      return res.data;
    },
    enabled: session && !needsFirstAccess,
    staleTime: 30_000,
  });
}
