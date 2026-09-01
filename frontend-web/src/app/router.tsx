import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AtendimentosPage } from '../features/academico/AtendimentosPage'
import { CertificadosPage } from '../features/academico/CertificadosPage'
import { EventoSessaoPage } from '../features/academico/EventoSessaoPage'
import { EventosAlunoPage } from '../features/academico/EventosAlunoPage'
import { FaqPage } from '../features/academico/FaqPage'
import { FormativasPage } from '../features/academico/FormativasPage'
import { InboxPage } from '../features/academico/InboxPage'
import { CaafPoolPage } from '../features/staff/CaafPoolPage'
import { CoePoolPage } from '../features/staff/CoePoolPage'
import { EventoHostDetailPage } from '../features/staff/EventoHostDetailPage'
import { EventosHostPage } from '../features/staff/EventosHostPage'
import { PublicarAvisoPage } from '../features/staff/PublicarAvisoPage'
import { EstagioDetailPage } from '../features/vinculos/EstagioDetailPage'
import { EstagiosPage } from '../features/vinculos/EstagiosPage'
import { TccDetailPage } from '../features/vinculos/TccDetailPage'
import { TccsPage } from '../features/vinculos/TccsPage'
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
          { path: '/formativas', element: <FormativasPage /> },
          { path: '/eventos', element: <EventosAlunoPage /> },
          { path: '/eventos/:id/presenca', element: <EventoSessaoPage /> },
          { path: '/certificados', element: <CertificadosPage /> },
          { path: '/atendimentos', element: <AtendimentosPage /> },
          { path: '/comunicados', element: <InboxPage /> },
          { path: '/faq', element: <FaqPage /> },
          { path: '/suporte', element: <FaqPage /> },
          { path: '/estagios', element: <EstagiosPage /> },
          { path: '/estagios/:id', element: <EstagioDetailPage /> },
          { path: '/tccs', element: <TccsPage /> },
          { path: '/tccs/:id', element: <TccDetailPage /> },
          { path: '/prof/eventos', element: <EventosHostPage /> },
          { path: '/prof/eventos/:id', element: <EventoHostDetailPage /> },
          { path: '/prof/comunicado', element: <PublicarAvisoPage /> },
          { path: '/comissoes/caaf', element: <CaafPoolPage /> },
          { path: '/comissoes/coe', element: <CoePoolPage /> },
        ],
      },
    ],
  },
])
