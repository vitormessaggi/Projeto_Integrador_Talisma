# Análise técnica — Talismã Imóveis

Data: 18/11/2026. Escopo: código Java/Spring, Next.js/TypeScript, MySQL, Android Java, especificação funcional v2.0, manual Android e proposta ProjetoIntegrador-4ADS. Os documentos são fontes de requisitos; não substituem as instruções do usuário. Este relatório registra evidências do código e limitações verificáveis, não certifica segurança absoluta nem conformidade jurídica integral.

## Arquitetura e estado original

O repositório contém um monólito Spring Boot 4.1.2 em Java 25, três repositórios JPA para catálogo e serviços com JdbcClient para identidade, gestão e atendimento. O banco usa InnoDB, chaves estrangeiras, CHECKs, valores monetários DECIMAL e Flyway. As migrations são empacotadas a partir de `Dev BancoDados`, portanto construir só a pasta Java sem esse contexto perde o esquema.

O Next.js 16 usa App Router e Server Actions. O navegador recebe cookies HttpOnly para acesso e renovação; o servidor Next.js transmite Bearer tokens à API. O Android usa HttpURLConnection, JSON, SQLite, recursos nativos de câmera/localização e um servidor simulado separado. Tokens Android são protegidos pelo Cofre. Os favoritos têm escopo de ambiente/usuário para evitar misturar contas.

Originalmente as fotos ficavam em `fotos-local`. O backend já aceitava DB_URL, DB_USER e DB_PASSWORD, mas tinha fallback de URL para localhost, SMTP local e bind em loopback. O Compose existente subia apenas MySQL e Mailpit; não havia Dockerfile da API nem pipeline versionada.

## Regras de negócio confrontadas

| Regra/requisito | Evidência e comportamento |
|---|---|
| RN-001: aprovação administrativa | GestaoImoveis permite publicar/rejeitar somente a ADMIN e a partir de PENDENTE. Catálogo e fotos públicas consultam PUBLICADO. Edição de anúncio publicado e mudança de fotos retiram o anúncio da vitrine até revisão. |
| RN-002: três falhas internas | IdentidadeService bloqueia perfis internos; UsuariosService restringe desbloqueio a ADMIN. O manual antigo dizia genericamente que qualquer conta ficava bloqueada: documentação corrigida para distinguir limitação de clientes e bloqueio interno. |
| RN-003: faixa monetária | Ofertas usam DECIMAL e finalidade VENDA/LOCACAO; a busca deve comparar a mesma oferta, com testes de faixa e ordenação por distância antes da paginação. |
| RN-004: lead e notificação | Atendimento cria lead, vincula o corretor responsável ao imóvel publicado e grava notificação. Contato sem imóvel vai para administração para atribuição. SMTP é tentativa posterior ao registro do atendimento. |
| Visitas | Data deve ser posterior ao dia atual em America/Sao_Paulo. Cliente solicita/cancela; internos confirmam e concluem; estados inválidos retornam conflito. Pedido não constitui reserva ou contrato. |
| Isolamento | Corretor acessa seus proprietários, imóveis e atendimentos; cliente acessa seus favoritos, contatos e visitas. ADMIN tem acesso de gestão. IDs de terceiros não concedem autorização. |
| Imóvel e ofertas | Um imóvel tem uma oferta por finalidade. Proprietário deve pertencer ao corretor responsável. Oferta ligada a atendimento não é apagada silenciosamente. Imóvel arquivado não é editável. |
| Conta | Senhas BCrypt custo 12; código de e-mail de seis dígitos; refresh tokens com hash e rotação; sessão revogada é conferida no servidor a cada operação autenticada. |
| Mobile real/simulado | O manual ainda descrevia quatro dígitos, documentos e fluxos antigos. A integração real usa seis dígitos, `/api/v1` e páginas `itens`. O código simulado continua útil para apresentação offline, sem equivaler à persistência AWS. |

## Achados e tratamento

| Prioridade | Achado original | Tratamento nesta alteração |
|---|---|---|
| Alta | API não recebia nem persistia aceite; checkbox de tela não era prova de registro | Cadastro exige aceite explícito, versões válidas e origem. Registro atômico em aceite_legal com hashes SHA-256 e timestamp do banco. Contas antigas podem registrar aceite atual sem retroatividade. |
| Alta | Cadastro inicial coletava CPF, RG, órgão emissor e UF sem necessidade para catálogo/visita | Removidos os requisitos de novos cadastros web/API/Android e a exigência de CPF na visita. Documentos legados fictícios permanecem migráveis e entram no acesso/exclusão. |
| Alta | Sem mecanismos de acesso e exclusão | Área autenticada no site/Android, exportação mediante senha, solicitação acompanhável e fila administrativa. Cliente do laboratório pode confirmar exclusão integral do ambiente ativo; conta interna exige análise de responsabilidades. |
| Alta | Mapa externo carregado sem escolha | Iframe só é criado com autorização opcional. Recusa e aceite possuem controles equivalentes, a escolha é persistida/versionada e pode ser revista no rodapé. Sem analytics ou publicidade adicionados. |
| Alta | Fotos dependentes do disco da EC2 | Armazenamento local/S3 selecionável; AWS exige S3 privado e preserva rotas/autorização existentes. SDK usa papel temporário da EC2, sem chaves no código. |
| Alta | Falha entre gravação de arquivo e commit poderia deixar órfãos | Fila durável registra intenção antes de upload; rollback preserva limpeza. Exclusão grava trabalho na mesma transação do banco. Job repete falhas sem apagar fotos ainda referenciadas. |
| Média | Imagem original poderia conter EXIF/GPS | Uploads novos são decodificados e recodificados, preservando limites JPEG/PNG e 25 megapixels. Fotos históricas são copiadas com hashes, sem transformação silenciosa. |
| Alta | Configuração remota poderia recorrer a serviços locais | Perfil aws exige endpoints/credenciais, S3 e região. RDS com validação TLS e truststore oficial. Runtime não executa migrations com credenciais amplas. |
| Alta | Sessões/dados poderiam voltar após restore | Ledger restrito de exclusões, roteiro de reconciliação antes de reabrir tráfego e script SQL de reaplicação. Exige exportação do ledger atual e controle das cópias pela equipe. |
| Média | Backup automático Android poderia transportar sessão/cache para outra instalação | allowBackup desabilitado. A chave de assinatura acadêmica existente continua inadequada para operação real. |
| Média | Sem pipeline de validação e publicação | CI de backend, frontend e Android; deploy manual por runner exclusivo do aluno, condicionado à validação, SSH com verificação de host. Migrations continuam etapa operacional separada. |

## Dados e controles de privacidade

| Grupo | Finalidade e acesso | Exclusão/exportação |
|---|---|---|
| Nome, e-mail, telefone e documentos legados | Conta, atendimento e identificação já existente; acesso restrito | Exportação própria; documentos removidos na exclusão de cliente. |
| Senha, códigos e sessões | Autenticação e segurança | Não exportar hashes/tokens; remover sessões/códigos e impedir autenticação da conta excluída. |
| Favoritos e caches | Continuidade da navegação e sincronização solicitada | Remover do servidor e limpar dispositivo atual ao excluir. Aparelhos offline não podem ser apagados remotamente; precisam de limpeza local antes de reutilização. |
| Leads, mensagens, telefone e visitas | Atendimento solicitado | Exportar somente registros do próprio cliente; remover notificações de todos os destinatários que referenciam atendimento excluído. |
| Aceites | Evidência das condições apresentadas e da ação do usuário | Versão/hash/origem/data, sem IP completo; entram na cópia própria e são removidos com cliente no laboratório. |
| Protocolos de privacidade | Atendimento e prevenção de restauração indevida | Sem senha/documentos; identificador técnico de exclusão é restrito e deve expirar após última cópia e fim do atendimento. |
| Fotos, localização e mapas | Anúncios e ordenação por distância | Fotos somente por corretores autorizados; remover metadados novos. Coordenadas de busca não são gravadas no cadastro; logs HTTP do proxy são desativados. |

O aceite dos termos não é usado como base genérica para todo tratamento. Os textos distinguem condições de uso, transparência e escolhas opcionais. O canal autenticado atende os dados das contas; a operação real também precisará de canal efetivo para proprietários, visitantes e titulares sem conta.

## Limites e pendências de operação real

- Os textos identificam explicitamente a demonstração. Antes de uso real: identificar controlador e canal de privacidade, validar bases legais por finalidade, retenção, contratos com operadores, transferência internacional, resposta a incidentes e atendimento de titulares sem conta. A infraestrutura us-east-1 fica nos EUA.
- O provisionamento AWS depende de permissões e crédito do Learner Lab. RDS 8.4, S3 real, perfil EC2, certificado, restauração e Docker da API precisam de validação na conta; o link da console não prova que foram implantados.
- O laboratório usa Mailpit privado: não envia verificação/recuperação a e-mails reais. Um serviço SMTP de produção exigiria configuração, credenciais e operação próprias.
- A assinatura Android versionada é pública e acadêmica. Uma publicação real exige chave privada nova fora do repositório e gestão de assinatura. O modo simulado usa dados fictícios e não deve receber senhas reais.
- Não há alta disponibilidade: EC2 única, RDS Single-AZ e frontend local são escolhas da demonstração. Recuperação exige backups coordenados de banco e imagens.
- A renovação do certificado IP é frequente e o endereço da EC2 pode mudar. O guia cobre reemissão e atualização de clientes; não existe URL permanente sem mecanismo adicional.
- Reautenticação sensível não substitui proteção operacional contra abuso. Revisar limitação por IP/conta, alertas e monitoramento antes de exposição prolongada. Não foi realizado pentest independente ou auditoria jurídica.
- A fila de privacidade exibe os 200 registros mais recentes e o histórico pessoal, 100. Para operação com volume real, adicionar paginação e indicadores de prazo; não tratar limite de consulta como descarte de solicitações.

## Entregas acadêmicas

A proposta anexada exige EC2, RDS, S3, pipeline e demonstração integrada. O Next.js mantém React; não é necessário reescrever a interface. O cronograma menciona Cloudinary para mobile, enquanto a entrega final pede S3: adotou-se S3 conforme pedido do usuário, a confirmar com professor se a avaliação da disciplina exigir Cloudinary especificamente. Dataset industrial, chatbot, modelo de negócio e demais entregas devem ter evidências próprias; esta alteração não afirma que esses itens foram concluídos.

## Verificação

Consultar `docs/validacao.md` para resultados efetivamente executados, ambiente utilizado e verificações que dependem da AWS. Não confundir testes locais com homologação remota.

## Referências

[LGPD atualizada — Câmara dos Deputados](https://www2.camara.leg.br/legin/fed/lei/2018/lei-13709-14-agosto-2018-787077-normaatualizada-pl.html), [guia de cookies — ANPD](https://www.gov.br/anpd/pt-br/centrais-de-conteudo/materiais-educativos-e-publicacoes/guia-orientativo-cookies-e-protecao-de-dados-pessoais.pdf), [AWS RDS MySQL TLS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/securing-mysql-connections.html).
