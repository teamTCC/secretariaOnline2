-- V010: Seed — Authorities and Roles
-- These are the canonical FGAC codes referenced throughout the application

-- ============================================================
-- AUTHORITIES
-- ============================================================
INSERT INTO authority (code, descricao) VALUES
  ('auth.first_access',          'Completar primeiro acesso'),
  ('user.update_own_profile',    'Atualizar próprio perfil'),
  ('user.update_own_password',   'Alterar própria senha'),
  ('user.manage_students',       'Gerenciar cadastro de alunos'),
  ('user.manage_all',            'Gerenciar todos os usuários'),
  ('user.reset_password',        'Resetar senha de usuários'),
  ('iam.manage_roles',           'Gerenciar roles e authorities'),
  ('dashboard.view_own',         'Visualizar dashboard do aluno'),
  ('dashboard.view_self_professor','Visualizar dashboard do professor'),
  ('dashboard.view_secretary',   'Visualizar dashboard da secretaria'),
  ('request.open',               'Abrir solicitação'),
  ('request.view_own',           'Visualizar próprias solicitações'),
  ('request.internal_open',      'Abrir solicitação em nome de aluno'),
  ('request.deliberate',         'Deliberar solicitações'),
  ('request.view_curso',         'Visualizar solicitações do curso'),
  ('request.reopen',             'Reabrir solicitação'),
  ('event.manage',               'Criar e gerenciar eventos de presença'),
  ('event.host',                 'Operar evento ao vivo'),
  ('attendance.view_open',       'Ver eventos com presença aberta'),
  ('attendance.check_in',        'Confirmar presença em evento'),
  ('formative.submit',           'Submeter atividade formativa'),
  ('formative.view_own',         'Visualizar próprias atividades formativas'),
  ('formative.review',           'Revisar atividades formativas (CAAF)'),
  ('internship.view_own',        'Visualizar próprio estágio'),
  ('internship.upload_doc_own',  'Enviar documentos do próprio estágio'),
  ('internship.review',          'Revisar estágios (COE)'),
  ('internship.supervise',       'Supervisionar estágio como professor'),
  ('tcc.view_own',               'Visualizar próprio TCC'),
  ('tcc.upload_final',           'Enviar versão final do TCC'),
  ('tcc.supervise',              'Orientar TCC'),
  ('tcc.examine',                'Participar de banca do TCC'),
  ('communication.read',         'Ler comunicados'),
  ('communication.publish_class','Publicar comunicado para turma'),
  ('communication.publish',      'Publicar comunicado global'),
  ('certificate.view_own',       'Visualizar próprios certificados'),
  ('system.admin',               'Administração do sistema'),
  ('system.observe',             'Acesso a métricas e logs'),
  ('audit.read',                 'Ler trilha de auditoria')
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- ROLES
-- ============================================================
INSERT INTO role (code, descricao) VALUES
  ('ALUNO',       'Estudante ativo'),
  ('EGRESSO',     'Ex-aluno'),
  ('PROFESSOR',   'Docente'),
  ('COORDENADOR', 'Coordenador de curso'),
  ('SECRETARIO',  'Secretaria acadêmica'),
  ('CAAF',        'Comissão de Atividades Formativas'),
  ('COE',         'Comissão de Orientação de Estágios'),
  ('ADMIN',       'Administrador do sistema')
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- ROLE_AUTHORITY assignments
-- ============================================================
-- ALUNO
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'ALUNO' AND a.code IN (
  'auth.first_access', 'user.update_own_profile', 'user.update_own_password',
  'dashboard.view_own', 'request.open', 'request.view_own',
  'attendance.view_open', 'attendance.check_in',
  'formative.submit', 'formative.view_own',
  'internship.view_own', 'internship.upload_doc_own',
  'tcc.view_own', 'tcc.upload_final',
  'communication.read', 'certificate.view_own'
) ON CONFLICT DO NOTHING;

-- PROFESSOR
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'PROFESSOR' AND a.code IN (
  'auth.first_access', 'user.update_own_profile', 'user.update_own_password',
  'dashboard.view_self_professor', 'request.deliberate', 'request.view_curso',
  'event.manage', 'event.host',
  'formative.review',
  'internship.supervise',
  'tcc.supervise', 'tcc.examine',
  'communication.read', 'communication.publish_class'
) ON CONFLICT DO NOTHING;

-- COORDENADOR (inherits PROFESSOR + extras)
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'COORDENADOR' AND a.code IN (
  'auth.first_access', 'user.update_own_profile', 'user.update_own_password',
  'dashboard.view_self_professor', 'dashboard.view_secretary',
  'request.deliberate', 'request.view_curso', 'request.internal_open',
  'event.manage', 'event.host',
  'formative.review', 'internship.review', 'internship.supervise',
  'tcc.supervise', 'tcc.examine',
  'communication.read', 'communication.publish_class', 'communication.publish',
  'user.manage_students'
) ON CONFLICT DO NOTHING;

-- SECRETARIO
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'SECRETARIO' AND a.code IN (
  'auth.first_access', 'user.update_own_profile', 'user.update_own_password',
  'dashboard.view_secretary',
  'request.deliberate', 'request.view_curso', 'request.internal_open', 'request.reopen',
  'event.manage', 'user.manage_students', 'user.reset_password',
  'communication.read', 'communication.publish',
  'audit.read'
) ON CONFLICT DO NOTHING;

-- CAAF
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'CAAF' AND a.code IN (
  'auth.first_access', 'user.update_own_profile',
  'dashboard.view_secretary',
  'formative.review', 'request.deliberate', 'request.view_curso',
  'communication.read'
) ON CONFLICT DO NOTHING;

-- COE
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'COE' AND a.code IN (
  'auth.first_access', 'user.update_own_profile',
  'dashboard.view_secretary',
  'internship.review', 'request.deliberate', 'request.view_curso',
  'communication.read'
) ON CONFLICT DO NOTHING;

-- ADMIN
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;
