-- V013: FAQ seed data — perguntas frequentes para sistema acadêmico UFPR

INSERT INTO faq_item (id, categoria, pergunta, resposta, ordem, ativo) VALUES
(uuid_generate_v7(), 'SOLICITACOES',
 'Como abrir uma solicitação de reaproveitamento de disciplina?',
 'Acesse o menu Solicitações → Nova Solicitação → selecione "Reaproveitamento de Disciplina". Preencha o formulário com o código da disciplina cursada anteriormente e anexe o histórico escolar.',
 1, TRUE),

(uuid_generate_v7(), 'SOLICITACOES',
 'Qual o prazo para resposta da secretaria após abertura de solicitação?',
 'O prazo padrão é de 10 dias úteis. Você receberá um e-mail quando houver atualização no status.',
 2, TRUE),

(uuid_generate_v7(), 'ESTAGIO',
 'Quais documentos são necessários para declarar um estágio?',
 'São necessários: Termo de compromisso de estágio assinado pela empresa e pela UFPR, apólice de seguro, e plano de atividades. Todos os documentos devem ser digitalizados em PDF.',
 1, TRUE),

(uuid_generate_v7(), 'TCC',
 'Como registrar meu TCC no sistema?',
 'Acesse o menu TCC → Novo TCC. Informe o título provisório, área de concentração e seu orientador. A confirmação pelo orientador é feita automaticamente por e-mail.',
 1, TRUE),

(uuid_generate_v7(), 'FORMATIVAS',
 'Quantas horas formativas são necessárias para concluir o curso?',
 'O total de horas exigidas varia por curso. Consulte a grade curricular do seu curso ou acesse o painel do aluno para ver seu progresso atual.',
 1, TRUE),

(uuid_generate_v7(), 'CONTA',
 'Como alterar minha senha?',
 'Acesse Perfil → Segurança → Alterar Senha. Você precisará informar sua senha atual. Caso tenha esquecido, utilize a opção "Esqueci minha senha" na tela de login.',
 1, TRUE),

(uuid_generate_v7(), 'CONTA',
 'Como ativar notificações no aplicativo mobile?',
 'Após instalar o aplicativo, acesse Perfil → Notificações e ative as notificações de interesse. Certifique-se de que as permissões de notificação estão habilitadas no seu dispositivo.',
 2, TRUE),

(uuid_generate_v7(), 'GERAL',
 'Como exportar meus dados pessoais (LGPD)?',
 'Acesse Perfil → Privacidade → Exportar meus dados. O sistema irá gerar um arquivo JSON com todos os seus dados cadastrais e enviará o link de download por e-mail. O link expira em 24 horas.',
 1, TRUE);
