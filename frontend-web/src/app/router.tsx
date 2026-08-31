import { createBrowserRouter, Navigate } from 'react-router-dom'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { NovaSolicitacaoPage } from '../features/solicitacoes/NovaSolicitacaoPage'
import { SolicitacaoDetailPage } from '../features/solicitacoes/SolicitacaoDetailPage'
import { SolicitacoesListPage } from '../features/solicitacoes/SolicitacoesListPage'
import { MePage } from '../features/perfil/MePage'
import { CertificadoPage } from '../features/publico/CertificadoPage'
import { ContatoPage } from '../features/publico/ContatoPage'
import { ErroPage } from '../features/publico/ErroPage'
import { ForgotPage } from '../features/publico/ForgotPage'
import { LoginPage } from '../features/publico/LoginPage'
import { OttPage } from '../features/publico/OttPage'
import { PrimeiroAcessoPage } from '../features/publico/PrimeiroAcessoPage'
import { ProtocoloPage } from '../features/publico/ProtocoloPage'
import { ResetPage } from '../features/publico/ResetPage'
import { AuthGuard } from '../shared/auth/AuthGuard'
import { Shell } from '../shared/ui/Shell'
import { HealthPage } from './HealthPage'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/recuperar-senha', element: <ForgotPage /> },
  { path: '/nova-senha', element: <ResetPage /> },
  { path: '/contato', element: <ContatoPage /> },
  { path: '/erro/:incidentId?', element: <ErroPage /> },
  { path: '/publico/solicitacoes/:ano/:numero', element: <ProtocoloPage /> },
  { path: '/publico/solicitacoes', element: <ProtocoloPage /> },
  { path: '/publico/verificar-certificado/:hash', element: <CertificadoPage /> },
  { path: '/publico/verificar-certificado', element: <CertificadoPage /> },
  { path: '/auth/ott', element: <OttPage /> },
  { path: '/health-front', element: <HealthPage /> },
  {
    element: <AuthGuard />,
    children: [
      {
        element: <Shell />,
        children: [
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/me', element: <MePage /> },
          { path: '/me-raw', element: <Navigate to="/me" replace /> },
          { path: '/primeiro-acesso', element: <PrimeiroAcessoPage /> },
          { path: '/solicitacoes', element: <SolicitacoesListPage /> },
          { path: '/solicitacoes/nova', element: <NovaSolicitacaoPage /> },
          { path: '/solicitacoes/:id', element: <SolicitacaoDetailPage /> },
        ],
      },
    ],
  },
])
