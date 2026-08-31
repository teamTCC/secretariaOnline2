export const dashboardKeys = {
  all: ["dashboard"] as const,
  aluno: () => [...dashboardKeys.all, "aluno"] as const,
};
