-- Demo disciplinas for entity-select (GET /academico/disciplinas). TADS + ES.

INSERT INTO disciplina (id_curso, codigo, nome, carga_horaria_total, creditos, ativa)
SELECT c.id, v.codigo, v.nome, v.carga, v.creditos, TRUE
FROM curso c
CROSS JOIN (
    VALUES
        ('ADS001', 'Algoritmos e Programação', 60, 4),
        ('ADS002', 'Banco de Dados', 60, 4),
        ('ADS003', 'Engenharia de Software', 60, 4),
        ('ADS004', 'Redes de Computadores', 60, 4),
        ('ADS005', 'Trabalho de Conclusão de Curso', 120, 8)
) AS v(codigo, nome, carga, creditos)
WHERE c.sigla = 'TADS'
ON CONFLICT (id_curso, codigo) DO NOTHING;

INSERT INTO disciplina (id_curso, codigo, nome, carga_horaria_total, creditos, ativa)
SELECT c.id, v.codigo, v.nome, v.carga, v.creditos, TRUE
FROM curso c
CROSS JOIN (
    VALUES
        ('ES001', 'Fundamentos de Engenharia de Software', 60, 4),
        ('ES002', 'Arquitetura de Software', 60, 4)
) AS v(codigo, nome, carga, creditos)
WHERE c.sigla = 'ES'
ON CONFLICT (id_curso, codigo) DO NOTHING;
