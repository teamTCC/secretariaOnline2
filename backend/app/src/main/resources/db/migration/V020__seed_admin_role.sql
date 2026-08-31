-- Bind the demo admin user to the ADMIN role (V011 created the user without a role).
INSERT INTO usuario_role (id_usuario, id_role)
SELECT u.id, r.id
FROM usuario u
CROSS JOIN role r
WHERE u.email = 'admin@ufpr.br'
  AND r.code = 'ADMIN'
ON CONFLICT (id_usuario, id_role) DO NOTHING;
