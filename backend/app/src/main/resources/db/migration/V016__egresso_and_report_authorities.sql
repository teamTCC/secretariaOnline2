-- V016: FGAC gaps — alumni.view_own, report.view_secretary, report.view_coordinator

INSERT INTO authority (code, descricao) VALUES
  ('alumni.view_own',        'Visualizar próprios dados de ex-aluno'),
  ('report.view_secretary',  'Acessar relatórios analíticos da secretaria'),
  ('report.view_coordinator','Acessar relatórios analíticos de coordenação')
ON CONFLICT (code) DO NOTHING;

-- EGRESSO: capabilities mínimas após registro de diploma (F5.11)
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'EGRESSO' AND a.code IN (
  'user.update_own_profile',
  'user.update_own_password',
  'alumni.view_own',
  'certificate.view_own',
  'communication.read'
) ON CONFLICT DO NOTHING;

-- report.view_secretary: SECRETARIO, COORDENADOR, CAAF, COE
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code IN ('SECRETARIO', 'COORDENADOR', 'CAAF', 'COE')
  AND a.code = 'report.view_secretary'
ON CONFLICT DO NOTHING;

-- report.view_coordinator: COORDENADOR, ADMIN
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code IN ('COORDENADOR', 'ADMIN')
  AND a.code = 'report.view_coordinator'
ON CONFLICT DO NOTHING;

-- ADMIN: V010 atribuiu TODAS as authorities existentes na época do seed.
-- Authorities criadas depois precisam ser concedidas explicitamente.
INSERT INTO role_authority (id_role, id_authority)
SELECT r.id, a.id FROM role r, authority a
WHERE r.code = 'ADMIN' AND a.code IN (
  'alumni.view_own',
  'report.view_secretary',
  'report.view_coordinator'
) ON CONFLICT DO NOTHING;
