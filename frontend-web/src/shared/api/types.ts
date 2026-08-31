export type ProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Array<{ field?: string; message?: string }>;
};

export type LoginRequest = {
  identificador: string;
  senha: string;
};

export type LoginResponse = {
  mustChangePassword: boolean;
  mustAcceptLgpd: boolean;
};

export type CsrfResponse = {
  token: string;
  headerName: string;
  parameterName: string;
};

export type FirstAccessRequest = {
  novaSenha: string;
  aceiteLgpd: boolean;
};

export type MeResponse = {
  id: string;
  nome: string;
  email: string;
  grr?: string | null;
  ativo?: boolean;
  metadata?: Record<string, unknown> | null;
  roles: string[];
  mustChangePassword: boolean;
  mustAcceptLgpd: boolean;
  _links?: Record<string, string>;
};

export type HorasFormativasKpi = {
  atual: number;
  requerido: number;
  percentual?: number;
};

export type DashboardKpis = {
  horasFormativas: HorasFormativasKpi;
  atendimentosPendentes?: number | null;
};

export type PendenciaItem = {
  id: string;
  tipo: string;
  estado: string;
  prazoEm?: string | null;
  acao?: string | null;
  _link: string;
};

export type EventoItem = {
  id: string;
  titulo: string;
  chCreditadas: number;
  fimEm?: string | null;
  _link: string;
};

export type SolicitacaoItem = {
  id: string;
  tipo: string;
  estado: string;
  createdAt?: string | null;
};

export type DashboardAlunoLinks = {
  self: string;
  novaSolicitacao?: string | null;
  formativas?: string;
  eventos?: string;
};

export type DashboardAlunoResponse = {
  kpis: DashboardKpis;
  pendencias?: PendenciaItem[] | null;
  eventos?: EventoItem[] | null;
  ultimasSolicitacoes?: SolicitacaoItem[] | null;
  _links: DashboardAlunoLinks;
  _degraded?: boolean | null;
};
