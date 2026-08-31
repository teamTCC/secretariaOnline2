export const queryKeys = {
  me: ['me'] as const,
  csrf: ['csrf'] as const,
  dashboard: (perfil: string) => ['dashboard', perfil] as const,
  dataExport: (jobId: string) => ['me', 'data-export', jobId] as const,
  contato: ['publico', 'contato'] as const,
  protocolo: (ano: string, numero: string) => ['publico', 'protocolo', ano, numero] as const,
  certificado: (hash: string) => ['publico', 'certificado', hash] as const,
  jwks: ['jwks'] as const,
}
