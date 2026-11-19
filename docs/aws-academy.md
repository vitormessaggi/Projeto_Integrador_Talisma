# Implantação no AWS Academy Learner Lab

Para criar os recursos pela console com nomes, tipos, campos e comandos completos, comece pelo [passo a passo detalhado](aws-passo-a-passo-completo.md). Este arquivo complementa a operação, pipeline e restauração.

Este roteiro usa **us-east-1**, frontend Next.js no computador do aluno, API Java em EC2, RDS MySQL e fotos em S3. A instalação é acadêmica, com dados fictícios. Os recursos ainda precisam ser criados na conta do laboratório; possuir um link para a console não fornece acesso por API nem confirma permissões.

## 1. Conferir o laboratório

1. No AWS Academy, abra Learner Lab, confira o saldo e inicie **Start Lab**. Aguarde o indicador verde e abra **AWS**. Use a conta temporária do laboratório, não uma conta pessoal.
2. Confirme **N. Virginia / us-east-1**. Leia a seção de serviços permitidos do seu Learner Lab. As permissões concretas variam por turma: confira EC2, RDS MySQL, S3, VPC e o instance profile disponível, frequentemente `LabInstanceProfile` associado a `LabRole`.
3. Não crie usuários IAM nem chaves permanentes. Não cole credenciais em chats, GitHub ou arquivos versionados. Se usar AWS CLI no computador para transferir fotos, configure somente as credenciais temporárias de **AWS Details**, incluindo session token, no perfil local; elas expiram.
4. Registre em notas privadas: instance ID, IP atual, VPC, subnets, security groups, endpoint RDS, nome do bucket e revisão Git. Confira o saldo antes e depois da apresentação. RDS e armazenamento podem consumir crédito mesmo com EC2 parada; siga o comportamento indicado pelo seu laboratório.

## 2. Rede, EC2 e RDS

1. Use a VPC padrão se ela existir e for permitida. Caso contrário, crie uma VPC com uma subnet pública para EC2 e duas subnets privadas em zonas diferentes para o DB subnet group. A API precisa de saída HTTPS para S3; não é necessário NAT Gateway com EC2 na subnet pública.
2. Crie `talisma-api-sg`: SSH 22 apenas do seu IP público `/32`; TCP 80 e 443 para acesso ao laboratório. A porta 80 atende o desafio ACME e redireciona o restante para HTTPS. Não abra 8080, 8025, 1025 ou 3306 à internet.
3. Crie `talisma-rds-sg`: entrada TCP 3306 com **origem no security group da EC2**, sem IP público e sem `0.0.0.0/0`.
4. Crie uma EC2 Ubuntu Server 24.04 LTS x86_64, `t3.medium` se autorizada pelo laboratório, EBS gp3 criptografado de 20 GiB. Isso fornece os 4 GiB previstos pela especificação; a imagem usa até 1 GiB. Se a classe estiver bloqueada, confirme uma classe permitida com o professor antes de criar substituta. Escolha key pair de laboratório e guarde a chave fora do projeto. Associe `talisma-api-sg` e o instance profile autorizado. Habilite IMDSv2 obrigatório, endpoint de metadados e **hop limit 2** para o SDK no container.
5. Crie RDS MySQL **8.4**, Single-AZ, classe `db.t3.micro` se permitida, armazenamento mínimo de 20 GiB criptografado, banco inicial `talisma`, acesso público **Não**, VPC e DB subnet group anteriores. Use `talisma-rds-sg`. Não ative adicionais pagos sem necessidade. Habilite backups por sete dias quando o laboratório permitir. Se a versão/classe estiver indisponível, documente a restrição; MySQL 8.0.46 foi usado na validação local, não equivale à validação em RDS 8.4.
6. Guarde a senha administrativa do RDS de modo privado. Ela serve para restaurar e criar contas limitadas; não será a senha do backend.

## 3. Bucket S3

1. Crie bucket de propósito geral com nome globalmente único, como `talisma-lab-ID-ALEATORIO`, em us-east-1.
2. Mantenha **Block all public access**, Object Ownership **Bucket owner enforced**, ACLs desabilitadas e criptografia SSE-S3. Deixe versionamento **desligado** nesta demonstração para não manter versões removidas sem tratamento específico. Não configure hospedagem de site nem CORS: somente a API acessa o bucket.
3. Confira se o perfil da EC2 permite `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` para esse bucket e `s3:ListBucket` para conferência. Se o Learner Lab não permitir ajustar IAM, utilize o papel fornecido e peça ao professor a liberação necessária. Não torne o bucket público para contornar `AccessDenied`.
4. A conta precisa acessar S3 via HTTPS. Use uma política de negação a transporte sem TLS, se sua turma permitir política de bucket. Não substitua políticas existentes sem revisão.
5. Crie uma regra Lifecycle apenas para o prefixo `backups/`, expiração de sete dias, se guardar backups nesse bucket. Nunca aplique a regra às fotos ativas, que ficam na raiz com UUID.

## 4. Preparar a EC2

Conecte com `ssh -i CAMINHO_DA_CHAVE ubuntu@IP_PUBLICO`. Verifique a impressão digital da chave do host na primeira conexão. Instale Docker Engine e Compose pelo [repositório oficial do Docker para Ubuntu](https://docs.docker.com/engine/install/ubuntu/), além de Nginx, Python 3, unzip, AWS CLI v2, cliente MySQL e OpenJDK 21 para gerar o truststore; a API usa Java 25 dentro do container. Confirme `docker compose version`, `aws --version`, `mysql --version` e `keytool -help`.

O usuário com acesso a Docker possui privilégios equivalentes a root: use somente o usuário de implantação do laboratório. Crie `/opt/talisma` pertencente a ele, com permissão 700, e envie o código com `git archive`/SCP ou clone usando seu mecanismo privado de acesso. Nunca envie `.git`, `.env`, fotos ou dumps junto à imagem. O Dockerfile deve ser construído com **a raiz do repositório como contexto**, pois inclui as migrations de `Dev BancoDados`.

Baixe o [bundle oficial de certificados do RDS](https://truststore.pki.rds.amazonaws.com/us-east-1/us-east-1-bundle.pem) para `/opt/talisma/rds-ca.pem`. O bundle contém várias CAs: importe **cada certificado** em `/opt/talisma/rds-truststore.p12`, tipo PKCS12, senha `changeit` (contém somente certificados públicos). Uma forma de separar e importar, na EC2:

```bash
cd /opt/talisma
mkdir -p ca-rds
awk '/-----BEGIN CERTIFICATE-----/{n++} n {print > ("ca-rds/ca-" n ".pem")}' rds-ca.pem
for ca in ca-rds/*.pem; do
  keytool -importcert -noprompt -storetype PKCS12 -keystore rds-truststore.p12 \
    -storepass changeit -alias "$(basename "$ca" .pem)" -file "$ca"
done
chmod 644 rds-truststore.p12
```

## 5. Migrar os dados locais

1. Pare a API local e interrompa gravações no Android/site. Faça a cópia final com o script abaixo; ele não altera a origem, exige pasta nova, guarda o Flyway e verifica hashes das fotos. Pré-requisito: Python 3.11+ e MySQL CLI no PATH, ou parâmetros `--mysql` e `--mysqldump` com os caminhos dos executáveis.

```powershell
python scripts/migrar-dados.py exportar --pasta .local/backup-migracao --usuario USUARIO_LOCAL --port 3306 --fotos "Dev BackEnd/imoveisapi/fotos-local"
```

2. Restrinja o acesso ao backup e transfira sua pasta para `/opt/talisma/backup-migracao` por SCP. Preserve o original no computador.
3. Na EC2, restaure em `talisma` **vazio**. O script recusa destino já populado e valida TLS/hostname com a CA do RDS. Use o endpoint DNS do RDS, nunca o IP do banco.

```bash
python3 scripts/migrar-dados.py restaurar --pasta /opt/talisma/backup-migracao \
  --host ENDPOINT_RDS --usuario ADMIN_RDS --ca /opt/talisma/rds-ca.pem
python3 scripts/migrar-dados.py s3 --pasta /opt/talisma/backup-migracao --bucket NOME_BUCKET
```

4. O envio S3 usa o papel da EC2, confere objetos existentes antes de copiar e baixa cada arquivo novamente para comparar SHA-256. Interrompa o corte se houver arquivo ausente, divergência de contagens, erro de permissões ou incompatibilidade de dump. Não remova o histórico Flyway, não execute `repair` ou baseline para esconder divergências.
5. A cópia preserva documentos legados fictícios. Novos cadastros não os coletam. Não execute novamente seeds de demonstração sobre a base restaurada.

## 6. Contas do banco e configuração da API

Após restaurar, conecte ao RDS por `mysql --host=ENDPOINT_RDS --user=ADMIN_RDS -p --ssl-mode=VERIFY_IDENTITY --ssl-ca=/opt/talisma/rds-ca.pem talisma`.

Crie `talisma_app` com SELECT, INSERT, UPDATE e DELETE somente em `talisma.*`; crie `talisma_migracao` com os privilégios de esquema necessários ao Flyway, separados da conta de execução. Exija SSL para ambas. Use senhas únicas e o prompt interativo, não copie senhas para o histórico do shell. O administrador do RDS pode criar essas contas; não use o usuário master no runtime.

Crie `/opt/talisma/runtime.env` com `chmod 600` em editor privado. Não há `.env.example`: a lista de configuração fica nesta documentação. Defina:

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:mysql://ENDPOINT_RDS:3306/talisma?connectionTimeZone=UTC&sslMode=VERIFY_IDENTITY&trustCertificateKeyStoreUrl=file:/run/secrets/rds-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=changeit&fallbackToSystemTrustStore=false` |
| `DB_USER` | `talisma_app` |
| `DB_PASSWORD` | Senha privada da conta de execução |
| `APP_JWT_KEY` | Chave aleatória Base64 de pelo menos 32 bytes; preserve para rollback |
| `S3_BUCKET` | Nome do bucket privado |
| `AWS_DEFAULT_REGION` | `us-east-1` |
| `APP_MAIL_FROM` | Endereço fictício, como `no-reply@example.test` |

Não coloque access key, secret key ou session token AWS neste arquivo: o SDK usa o papel da EC2. O perfil `aws` exige os parâmetros remotos e desabilita migrations automáticas no runtime.

Construa a imagem e aplique migrations com as credenciais separadas:

```bash
export APP_IMAGE_TAG=REVISAO_GIT
docker compose -f infra/aws/compose.yaml build api
read -r -p 'Usuário de migrations: ' DB_MIGRATION_USER
read -r -s -p 'Senha de migrations: ' DB_MIGRATION_PASSWORD; echo
export DB_MIGRATION_USER DB_MIGRATION_PASSWORD
docker compose -f infra/aws/compose.yaml run --rm --no-deps \
  -e SPRING_PROFILES_ACTIVE=aws,migrar \
  -e DB_MIGRATION_USER -e DB_MIGRATION_PASSWORD api
unset DB_MIGRATION_USER DB_MIGRATION_PASSWORD
bash infra/aws/deploy.sh
```

As migrations novas são V5 (aceites, solicitações e limpeza de fotos) e V6 (identificador mínimo de exclusão para restauração). A imagem inicia como usuário sem privilégios, com filesystem somente leitura e `/tmp` limitado. O primeiro administrador já migrado pode ser utilizado; em base nova, execute o perfil `provisionar-admin` conforme o guia local, com os parâmetros AWS e sem manter a senha no runtime.

## 7. HTTPS sem domínio

Use Certbot **5.4 ou superior**, instalado pela [distribuição oficial](https://certbot.eff.org/instructions), pois há suporte a certificados para IP público. Não use certificado autoassinado e não desabilite validação TLS no Android.

1. Configure inicialmente apenas o bloco HTTP de `infra/aws/nginx.conf`, substituindo `IP_PUBLICO` pelo IPv4 atual; crie `/var/www/acme` e valide `sudo nginx -t`. Ative Nginx para servir o desafio na porta 80. Ainda não carregue o bloco 443, porque o certificado não existe.
2. Solicite o certificado pelo webroot. O comando pode pedir e-mail e aceite dos termos do emissor: o operador deve fornecer as informações e confirmar o acordo.

```bash
sudo certbot certonly --webroot --webroot-path /var/www/acme \
  --preferred-profile shortlived --ip-address IP_PUBLICO --cert-name talisma-api
```

3. Copie o bloco HTTPS do arquivo do projeto, confira os caminhos `/etc/letsencrypt/live/talisma-api/`, rode `sudo nginx -t` e recarregue Nginx. Configure deploy hook de renovação com `systemctl reload nginx` e confirme que o timer do Certbot está ativo. Execute `sudo certbot renew --dry-run` e verifique a data do certificado. A validade é de aproximadamente seis dias, portanto renovação automática e verificação antes da apresentação são necessárias.
4. Teste externamente `https://IP_PUBLICO/api/v1/legal`; deve responder sem alerta de certificado. `/actuator/health` fica restrito a localhost e é consultado pelo script de deploy.

## 8. Site local, Android e Mailpit

No computador, na pasta do frontend:

```powershell
$env:API_URL='https://IP_PUBLICO'
$env:COOKIE_SECURE='false' # Apenas para localhost HTTP; em hospedagem HTTPS usar true.
npm ci
npm run dev
```

O site usa chamadas no servidor Next.js, portanto não exige tornar o RDS público nem configurar CORS amplo na API. O computador precisa conseguir acessar o IP da EC2. Para exibir o site em outro aparelho via rede local, configure HTTPS no frontend antes de transportar credenciais fora do computador.

No Android, use JDK **21 para executar o Gradle 8.14.3**, Android SDK 36 e Java 11 como linguagem do app. A API continua em Java 25. Compile com:

```powershell
.\gradlew.bat :app:assembleDebug -PapiUrl=https://IP_PUBLICO
```

Ou defina `api.url=https://IP_PUBLICO` no `local.properties` ignorado pelo Git. O app rejeita HTTP remoto; as exceções de cleartext são exclusivas do emulador/localhost. Desligue o modo demonstração e use a API real. A assinatura versionada existente é somente acadêmica e não deve ser usada em publicação comercial.

Para ler e-mails de teste, abra um túnel no computador:

```powershell
ssh -i CAMINHO_DA_CHAVE -N -L 8025:127.0.0.1:8025 ubuntu@IP_PUBLICO
```

Visite `http://127.0.0.1:8025`. Nada é enviado a e-mails reais; o Mailpit privado pode perder mensagens ao recriar o container.

## 9. Pipeline e rollback

`validar.yml` testa backend com MySQL 8.4 descartável em Docker, verifica os documentos legais, compila/linta Android com JDK 21 e valida/builda o frontend com testes Playwright de privacidade.

Para deploy manual, configure um runner GitHub Actions temporário no computador do aluno, exclusivo deste repositório, rótulo `talisma-lab`. Configure SSH local e `known_hosts` com fingerprint verificada. Defina variáveis do repositório `LAB_HOST` (IP ou DNS sem protocolo) e `LAB_USER` (`ubuntu`). Não armazene chave privada em código ou no runner compartilhado. Execute `Publicar API no Learner Lab` por `workflow_dispatch`: validação deve passar antes do envio. Não execute o runner em contribuições não confiáveis; ao terminar, desconecte e remova o registro do runner no GitHub.

A pipeline não recebe senha de migrations e não aplica mudanças de esquema automaticamente. Antes da primeira publicação de uma versão que altera banco, aplique migrations pelo procedimento privado acima. Mantenha imagens da versão anterior e registre o tag. Para rollback, execute `APP_IMAGE_TAG=TAG_ANTERIOR docker compose -f infra/aws/compose.yaml up -d --no-build` e verifique saúde e compatibilidade do esquema. Não faça downgrade de migrations automaticamente.

## 10. Backup, exclusão e encerramento

- Configure retenção de snapshots por sete dias e registre a cópia de fotos correspondente. Não restaure backup sem conciliar exclusões posteriores.
- Depois de exclusões e antes de qualquer restauração, exporte o ledger restrito via `GET /api/v1/admin/privacidade/exclusoes` com conta administrativa. Guarde-o fora do backup que será restaurado. Ele contém apenas protocolo, identificador técnico e data, sem documentos ou credenciais.
- Restaure em banco novo, aplique migrations e preencha `scripts/reaplicar-exclusoes.sql` com os IDs confirmados do ledger. Revogue todas as sessões restauradas antes de reabrir tráfego. Em perda total, reconcilie também protocolos de exclusão preservados pela equipe; não reative uma cópia sem essa conferência.
- Quando todos os backups que possam conter um titular tiverem expirado e o atendimento estiver encerrado, remova o registro mínimo correspondente sob controle administrativo. Esta etapa depende de conhecer os snapshots efetivamente existentes; não é purgada às cegas pela aplicação.
- Ao reiniciar o Learner Lab, confirme IP da EC2, estado do RDS, acesso S3 e validade do certificado. Se o IP mudar, atualize Nginx, emita certificado para o novo IP, atualize `API_URL`, o build Android, `LAB_HOST` e a chave de host SSH verificada.
- Ao encerrar a atividade, exporte evidências necessárias, elimine recursos de demonstração e cópias desnecessárias pela console, remova runner e credenciais temporárias locais e confira o saldo. A criação/remoção efetiva dos recursos deve ser verificada na conta do laboratório.

## Referências

[Conexões seguras RDS MySQL](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/securing-mysql-connections.html), [credenciais EC2 no SDK Java](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/ec2-iam-roles.html), [IMDSv2 para containers](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-IMDS-new-instances.html), [Certbot e certificados de IP](https://letsencrypt.org/2026/03/11/shorter-certs-certbot).
