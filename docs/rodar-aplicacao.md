# Como rodar o Talismã Imóveis

O site e o Android consultam a mesma origem HTTPS da AWS. Na EC2, o Nginx encaminha `/api/v1/` ao Spring Boot e `POST /chat` à Lia. O Spring Boot acessa Aurora MySQL, S3 e o Mailpit privado.

## Caminho rápido para testar

1. [Reabra o Learner Lab e confira os serviços da EC2](#31-reutilizar-a-implantação-existente).
2. [Confirme a URL HTTPS, inicie o site e acesse o Mailpit](#36-abrir-o-site-e-o-mailpit-com-a-api-da-aws).
3. [Instale o Android](#4-rodar-o-android) se for testar no emulador ou em um aparelho.
4. [Siga o roteiro manual](#5-testar-os-fluxos). Use os passos de provisionamento somente para montar outra instalação.

Ao testar com a AWS, deixe o backend, a Lia e o Mailpit rodando na EC2. No seu computador, execute o Next.js e, se necessário, o Android Studio. O Docker Desktop local só é necessário se você o usar para rodar contêineres ou os testes Java com Testcontainers.

## 1. Preparar o computador

- Instale Git e Node.js 24 com npm para rodar o frontend.
- Para compilar o Android, instale Android Studio, JDK 21, SDK 36 e configure um emulador ou aparelho com depuração USB.
- No Windows, use um caminho sem acentos para a cópia de trabalho do Android. O plugin do Gradle pode recusar caminhos como `Área de Trabalho`.
- Para operar a AWS, use o console do Learner Lab em `us-east-1` e o método de acesso já configurado para a EC2. A execução normal do site e do Android exige internet e só precisa da URL HTTPS da API.

## 2. Escolher a URL pública

Use um nome DNS estável que você controla, apontado à EC2 e coberto por um certificado público. Com as permissões e o orçamento do laboratório, um Elastic IP mantém o IPv4 durante paradas da EC2. Se a instância usar IPv4 dinâmico, atualize o DNS ao reiniciar o lab. Com IPv4 direto e dinâmico, será preciso atualizar `API_URL`, emitir o certificado para o novo IP e reinstalar o Android. [Endereços Elastic IP na AWS](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/elastic-ip-addresses-eip.html)

Quando o laboratório estiver encerrado ou os serviços estiverem parados, site e Android ficam sem acesso aos dados online.

## 3. Rodar com a AWS Academy

### 3.1. Reutilizar a implantação existente

1. Abra o Learner Lab e clique em **Start Lab**. Abra a AWS e selecione **N. Virginia (`us-east-1`)**.
2. Em **Aurora and RDS**, confira se `talisma-aurora` e sua gravadora estão disponíveis. Em **EC2**, inicie `talisma-api-ec2` se estiver parada. Copie o ID da instância para o Session Manager e o IPv4 público atual para montar a URL da API.
3. Acesse a EC2 por SSH com a chave do laboratório e a identidade do host verificada. Se a chave estiver indisponível e a instância estiver online no Systems Manager, use **Session Manager** ou **Run Command** conforme as permissões do laboratório.
4. Na EC2, confira o estado da aplicação:

```bash
sudo docker ps -a --format 'table {{.Names}}\t{{.Status}}'
sudo systemctl status nginx --no-pager
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8000/health
curl -fsS http://127.0.0.1:8025/readyz
```

Na implantação existente, os contêineres se chamam `talisma-api`, `talisma-chatbot` e `talisma-mailpit`. Se estiverem parados, use `sudo docker start talisma-mailpit talisma-api talisma-chatbot` e repita as verificações depois da inicialização. Em uma implantação feita pelo Compose versionado, use `docker compose -f infra/aws/compose.yaml ps` e, se necessário, `docker compose -f infra/aws/compose.yaml up -d` na pasta da revisão, com o `APP_IMAGE_TAG` correspondente.

Se o contêiner da Lia ainda precisar ser criado e a API Java estiver rodando avulsa, publique apenas o chatbot depois de validar a origem HTTPS do catálogo.

```bash
export APP_IMAGE_TAG=REVISAO_GIT
export CHAT_CATALOG_URL=https://HOST_PUBLICO
docker compose -f infra/aws/compose.yaml up -d --no-deps --build chatbot
curl -fsS http://127.0.0.1:8000/health
```

Se a instalação existente usa Docker avulso e a rede `talisma-lab`, a Lia pode consultar `http://talisma-api:8080` nessa rede privada. Na raiz da revisão extraída na EC2, faça a primeira implantação da Lia com estes comandos Bash. Se o Docker antigo não tiver Buildx e ainda oferecer o construtor clássico, prefixe o comando `docker build` com `DOCKER_BUILDKIT=0`.

```bash
REVISAO=REVISAO_GIT
docker build -t "talisma-chatbot:$REVISAO" 'Dev Chatbot'
opcoes_env=()
if [ -r /opt/talisma/chatbot.env ]; then
  opcoes_env=(--env-file /opt/talisma/chatbot.env)
fi
docker run -d --name talisma-chatbot --restart unless-stopped \
  --network talisma-lab -p 127.0.0.1:8000:8000 --memory 900m \
  --cap-drop ALL --security-opt no-new-privileges \
  "${opcoes_env[@]}" -e API_URL=http://talisma-api:8080 \
  -e OMP_NUM_THREADS=1 -e MKL_NUM_THREADS=1 -e OPENBLAS_NUM_THREADS=1 \
  "talisma-chatbot:$REVISAO"
curl -fsS http://127.0.0.1:8000/health
```

Acrescente `location = /chat` ao bloco HTTPS ativo e `limit_req_zone` ao contexto `http`, conforme o [modelo](../infra/aws/nginx.conf). Rode `sudo nginx -t && sudo systemctl reload nginx`.

Confirme externamente que o certificado público do Nginx cobre o endereço atual. A seção 3.5 descreve a emissão e a renovação. Se o Nginx estiver servindo certificados antigos no mesmo endereço de escuta, confira qual bloco é o padrão da porta 443 e valide que clientes conectados por IPv4 recebem o certificado novo.

### 3.2. Provisionar uma instalação nova

Siga a ordem de recursos em [AWS Academy](aws.md). Crie uma VPC com uma subnet pública com Internet Gateway para EC2 e duas subnets privadas em zonas distintas para o Aurora. Habilite resolução DNS e hostnames na VPC. No grupo da EC2, permita 443 e, para o desafio de certificado, 80. Se usar SSH, limite 22 ao IP do operador. No grupo do banco, permita 3306 apenas a partir do grupo da EC2.

Para o Aurora, selecione criação padrão, **Aurora MySQL**, versão compatível com **MySQL 8.4**, armazenamento criptografado, uma instância gravadora `db.t4g.medium` sem IP público e banco inicial `talisma`. Use o grupo de subnets privadas, backups de sete dias e senha administrativa gerenciada pelo Secrets Manager. Registre o endpoint de escrita. Crie o bucket de propósito geral com acesso público bloqueado, SSE-S3 e controle de propriedade **Bucket owner enforced**.

Na EC2 Ubuntu, instale Docker Engine com Compose 2.24 ou posterior, Nginx, Python 3, cliente MySQL oficial, AWS CLI v2 e um JDK com `keytool`. O Java 25 da aplicação vem da imagem Docker. Confirme se `docker compose version`, `aws --version`, `mysql --version` e `keytool -help` funcionam. Verifique se o perfil da EC2 permite ler, gravar e excluir objetos no bucket. [Docker para Ubuntu](https://docs.docker.com/engine/install/ubuntu/) · [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)

Prepare um diretório privado para a aplicação:

```bash
sudo install -d -m 700 -o "$(id -un)" /opt/talisma /opt/talisma/projeto
```

Do computador, a partir da raiz e com o IPv4 e caminho da chave preenchidos:

```powershell
git archive --format=tar -o "$env:TEMP/talisma-release.tar" HEAD
scp -i CAMINHO_DA_CHAVE "$env:TEMP/talisma-release.tar" ubuntu@IP_PUBLICO:/opt/talisma/release.tar
```

`git archive` leva o conteúdo commitado de `HEAD`. Confira se ele contém a revisão que você deseja publicar. Na EC2, extraia na pasta vazia `/opt/talisma/projeto`:

```bash
tar -xf /opt/talisma/release.tar -C /opt/talisma/projeto
cd /opt/talisma
curl -fsS https://truststore.pki.rds.amazonaws.com/us-east-1/us-east-1-bundle.pem -o rds-ca.pem
mkdir -p ca-rds
awk '/-----BEGIN CERTIFICATE-----/{n++} n {print > ("ca-rds/ca-" n ".pem")}' rds-ca.pem
for ca in ca-rds/*.pem; do
  keytool -importcert -noprompt -storetype PKCS12 -keystore rds-truststore.p12 \
    -storepass changeit -alias "$(basename "$ca" .pem)" -file "$ca"
done
chmod 644 rds-truststore.p12
```

O truststore contém somente certificados públicos. Gere-o uma vez por bundle em uma instalação nova.

### 3.3. Configurar o banco e o runtime

Na EC2, conecte com o administrador e a senha gerenciada pelo Secrets Manager. Substitua `ENDPOINT_RDS` pelo nome DNS do endpoint de escrita:

```bash
mysql --host=ENDPOINT_RDS --user=talisma_admin -p \
  --ssl-mode=VERIFY_IDENTITY --ssl-ca=/opt/talisma/rds-ca.pem talisma
```

Se for importar uma base existente, faça a [migração opcional](#37-migrar-dados-e-fotos-opcional) antes do primeiro Flyway na AWS. Para uma base nova, crie as duas contas usando senhas longas distintas, registradas fora do Git:

```sql
CREATE USER 'talisma_app'@'%' IDENTIFIED BY 'SENHA_EXECUCAO' REQUIRE SSL;
CREATE USER 'talisma_migracao'@'%' IDENTIFIED BY 'SENHA_MIGRACAO' REQUIRE SSL;
GRANT SELECT, INSERT, UPDATE, DELETE ON talisma.* TO 'talisma_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES ON talisma.* TO 'talisma_migracao'@'%';
```

Crie `/opt/talisma/runtime.env` em um editor, com permissão `600`. Substitua os marcadores. Gere `APP_JWT_KEY` com `openssl rand -base64 32` e preserve a chave em atualizações e rollbacks.

```dotenv
DB_URL=jdbc:mysql://ENDPOINT_RDS:3306/talisma?connectionTimeZone=UTC&sslMode=VERIFY_IDENTITY&trustCertificateKeyStoreUrl=file:/run/secrets/rds-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=changeit&fallbackToSystemTrustStore=false
DB_USER=talisma_app
DB_PASSWORD=SENHA_EXECUCAO
APP_JWT_KEY=CHAVE_BASE64
S3_BUCKET=NOME_BUCKET
AWS_DEFAULT_REGION=us-east-1
APP_MAIL_FROM=no-reply@talisma.example
```

Para habilitar a geração opcional da Lia, crie `/opt/talisma/chatbot.env` com permissão `600` e configure `GROQ_API_KEY` e, se desejado, `GROQ_MODEL`. O arquivo é lido pelo contêiner Python. O serviço também responde com orientações e consulta o catálogo quando a Groq está desativada.

O SDK usa as credenciais temporárias do perfil da EC2. Mantenha a senha do administrador do banco e a senha da conta Flyway fora do arquivo de runtime.

### 3.4. Construir a API e a Lia, aplicar migrations e iniciar os serviços

Na EC2, a partir do projeto extraído:

```bash
cd /opt/talisma/projeto
export APP_IMAGE_TAG=REVISAO_GIT
docker compose -f infra/aws/compose.yaml build api
read -r -s -p 'Senha da conta talisma_migracao: ' DB_MIGRATION_PASSWORD; echo
export DB_MIGRATION_USER=talisma_migracao DB_MIGRATION_PASSWORD
docker compose -f infra/aws/compose.yaml run --rm --no-deps \
  -e SPRING_PROFILES_ACTIVE=aws,migrar -e DB_MIGRATION_USER -e DB_MIGRATION_PASSWORD api
unset DB_MIGRATION_USER DB_MIGRATION_PASSWORD
bash infra/aws/deploy.sh
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:8000/health
```

A imagem da Lia baixa o modelo de embeddings e indexa o PDF no build. Reserve espaço em disco para o modelo e para o cache Docker. Na EC2 existente, confira memória e disco com `free -h` e `df -h` antes de publicar. Se Java e Python forem rodar juntos, use uma instância com pelo menos 4 GiB como ponto de partida e verifique o consumo com `docker stats --no-stream`. Substitua `REVISAO_GIT` por um identificador da revisão. Se o Flyway falhar, corrija o erro antes do deploy. Para criar o primeiro administrador da aplicação em um banco vazio, execute uma vez no mesmo terminal:

```bash
read -r -p 'Nome do administrador: ' ADMIN_NOME
read -r -p 'E-mail do administrador: ' ADMIN_EMAIL
read -r -s -p 'Senha do administrador: ' ADMIN_SENHA; echo
export ADMIN_NOME ADMIN_EMAIL ADMIN_SENHA
docker compose -f infra/aws/compose.yaml run --rm --no-deps \
  -e SPRING_PROFILES_ACTIVE=aws,provisionar-admin -e ADMIN_NOME -e ADMIN_EMAIL -e ADMIN_SENHA api
unset ADMIN_NOME ADMIN_EMAIL ADMIN_SENHA
```

### 3.5. Configurar HTTPS no Nginx

O arquivo [nginx.conf](../infra/aws/nginx.conf) contém os dois blocos. Em uma instalação nova, copie primeiro só o bloco HTTP para `/etc/nginx/conf.d/talisma.conf`. Se já existir uma configuração do Talismã, identifique o arquivo ativo com `sudo nginx -T` e edite nele os blocos do serviço. Substitua `IP_PUBLICO` pelo IPv4 atual, crie `/var/www/acme` com `sudo install -d -m 755 /var/www/acme` e aplique com `sudo nginx -t && sudo systemctl reload nginx`. Use o DNS estável em `server_name` quando disponível e emita o certificado com `sudo certbot certonly --webroot --webroot-path /var/www/acme -d HOST_PUBLICO --cert-name talisma-api`. Se estiver usando um IPv4 diretamente, emita o certificado com Certbot 5.4 ou superior:

```bash
sudo certbot certonly --webroot --webroot-path /var/www/acme \
  --preferred-profile shortlived --ip-address IP_PUBLICO --cert-name talisma-api
```

Adicione ou atualize o bloco 443. Confira em `sudo certbot certificates` os caminhos de `fullchain.pem` e `privkey.pem`, valide com `sudo nginx -t` e aplique com `sudo systemctl reload nginx`. Configure um hook de deploy para recarregar o Nginx após a renovação, confira o timer do Certbot e execute `sudo certbot renew --dry-run`. O certificado público por IPv4 dura cerca de seis dias. [Certbot e certificados por IP](https://letsencrypt.org/2026/03/11/shorter-certs-certbot/)

### 3.6. Abrir o site e o Mailpit com a API da AWS

No Windows, troque `HOST_PUBLICO` pelo DNS ou IPv4 atual coberto pelo certificado. Verifique primeiro se o catálogo responde por HTTPS. Depois salve a origem no Windows e reinicie o Next.js ou o Android Studio que já estiver aberto para que leia a URL atual.

```powershell
$env:API_URL = 'https://HOST_PUBLICO'
$catalogo = Invoke-RestMethod "$env:API_URL/api/v1/imoveis?size=1" -TimeoutSec 15 -ErrorAction Stop
$catalogo.totalElementos
[Environment]::SetEnvironmentVariable('API_URL', $env:API_URL, 'User')
$corpo = @{ pergunta = 'Quais imóveis estão à venda?'; usar_ia = $false } | ConvertTo-Json
Invoke-RestMethod "$env:API_URL/chat" -Method Post -ContentType 'application/json; charset=utf-8' -Body $corpo -TimeoutSec 30
```

A partir da raiz do repositório:

```powershell
Set-Location 'Dev FrontEnd/talisma-imoveis'
npm ci
npm run dev -- --hostname 127.0.0.1 --port 3000
```

Abra [o site](http://127.0.0.1:3000/) e confira as abas de venda e locação. Na base do laboratório já validada há 10 anúncios fictícios publicados por finalidade e duas fotos por imóvel. O backend usa HTTPS mesmo quando o site roda em HTTP no loopback.

O perfil `aws` envia e-mails de teste ao SMTP privado do Mailpit na porta 1025. Para ler os códigos no computador, mantenha um túnel aberto em outro terminal e acesse a caixa pelo navegador. [Mailpit do laboratório](http://127.0.0.1:18025/)

Se já tiver acesso SSH, substitua o caminho da chave e o IP público neste comando PowerShell.

```powershell
ssh -i CAMINHO_DA_CHAVE -N -L 127.0.0.1:18025:127.0.0.1:8025 ubuntu@IP_PUBLICO
```

Com AWS CLI, Session Manager Plugin e credenciais válidas do Learner Lab configurados no computador, use o ID da instância para abrir o túnel pelo SSM. [Encaminhamento de porta pelo Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-sessions-start.html#sessions-start-port-forwarding)

```powershell
aws ssm start-session --region us-east-1 --target ID_DA_INSTANCIA `
  --document-name AWS-StartPortForwardingSession `
  --parameters 'portNumber=8025,localPortNumber=18025'
```

Se puder abrir uma sessão do Session Manager pelo console, também é possível consultar a mensagem diretamente na EC2. Execute o trecho Bash abaixo com o e-mail fictício usado no cadastro. O comando mostra o assunto e o corpo da mensagem mais recente de verificação para esse endereço.

```bash
read -r -p 'E-mail fictício cadastrado: ' EMAIL_TESTE
mailpit=http://127.0.0.1:8025/api/v1
mensagem=$(curl -fsS --get "$mailpit/search" --data-urlencode "query=to:$EMAIL_TESTE" |
  jq -r 'first(.messages[]? | select(.Subject == "Verifique seu e-mail") | .ID) // empty')
if [ -n "$mensagem" ]; then
  curl -fsS "$mailpit/message/$mensagem" | jq -r '.Subject, .Text'
else
  echo 'Mensagem ainda não encontrada. Confira o e-mail e tente novamente.'
fi
```

### 3.7. Migrar dados e fotos (opcional)

Se precisar exportar dados de uma instalação anterior, pare as gravações antes da cópia. O script usa Python 3.11+, `mysql` e `mysqldump`. Use o host e a porta da base de origem:

```powershell
python scripts/migrar-dados.py exportar --pasta .local/backup-migracao --usuario talisma_app --host HOST_ORIGEM --port PORTA_ORIGEM --fotos "Dev BackEnd/imoveisapi/fotos-local"
```

Transfira o backup de forma privada para a EC2 e restaure em schema `talisma` vazio, antes de aplicar Flyway. O script conserva o histórico de migrations e valida hashes:

```bash
cd /opt/talisma/projeto
python3 scripts/migrar-dados.py restaurar --pasta /opt/talisma/backup-migracao \
  --host ENDPOINT_RDS --usuario talisma_admin --ca /opt/talisma/rds-ca.pem
python3 scripts/migrar-dados.py s3 --pasta /opt/talisma/backup-migracao --bucket NOME_BUCKET
```

Confira o resultado antes de iniciar a aplicação. Para um backup que possa conter contas já excluídas, siga a conciliação descrita em [AWS Academy](aws.md#atualizações-backups-e-restauração).

## 4. Rodar o Android

1. Abra `Dev Mobile/talisma-app` no Android Studio. Selecione o JDK 21 para o Gradle e instale o SDK 36.
2. Confirme a variável `API_URL` da seção 3.6. Para uma configuração específica do módulo, use `api.url=https://HOST_PUBLICO` em `local.properties`, que fica fora do Git. A ordem é `-PapiUrl`, `API_URL` e `api.url`. O APK guarda a URL usada na compilação. Se o IP do laboratório mudar, gere e instale outro APK.
3. Inicie um emulador no **Device Manager** ou conecte um aparelho com depuração USB. Execute a configuração `app` ou rode os comandos PowerShell em um segundo terminal, a partir da raiz do repositório.

```powershell
Set-Location 'Dev Mobile/talisma-app'
$env:API_URL = [Environment]::GetEnvironmentVariable('API_URL', 'User')
.\gradlew.bat :app:installDebug
```

4. Abra o catálogo e toque no avatar da Lia acima da barra inferior. Experimente compra, locação e visitas. Abra uma sugestão e confirme que a tela e os filtros correspondem ao link.
5. Se a instalação já estiver em **Modo demonstração**, toque cinco vezes em **Versão 1.0** no Perfil e desative a opção. O catálogo usa a API por padrão em novas instalações.

## 5. Testar os fluxos

Use somente dados fictícios. Para manter cliente, corretor e administrador logados ao mesmo tempo, use perfis de navegador separados. Reutilize as contas de corretores já cadastradas no laboratório se tiver as credenciais guardadas localmente.

| Fluxo | Passos | Resultado esperado |
|---|---|---|
| Catálogo e fotos | Abra **Imóveis**, filtre venda e locação e abra um anúncio. | A lista reflete os filtros e a galeria carrega as fotos da AWS. |
| Cadastro e e-mail | Em **Criar conta**, informe um novo e-mail fictício, confira os termos e envie o cadastro. No Mailpit, procure **Verifique seu e-mail** para esse endereço. Informe os seis dígitos na tela **Verifique seu e-mail** e faça login. | A mensagem chega ao Mailpit e a conta verificada consegue entrar. |
| Corretor | Use uma conta existente ou crie uma em **Painel do administrador → Usuários internos → Cadastrar corretor**. Faça login como corretor e abra **Proprietários**. | O corretor entra no painel e consegue cadastrar um proprietário fictício com telefone e DDD. |
| Imóvel | Como corretor, abra **Cadastrar imóvel**, selecione um proprietário, informe um CEP, confira o endereço, complete o número e preencha pelo menos um preço. Salve, abra o anúncio no painel, envie fotos e clique em **Enviar para revisão**. | Na revisão com ViaCEP, a consulta preenche logradouro, bairro, cidade e UF quando disponíveis. O endereço continua editável. O anúncio fica pendente após o envio para revisão. |
| Publicar e editar | Como administrador, abra **Aprovações** e clique em **Aprovar e publicar**. Confira o anúncio no catálogo. Para testar a edição, abra um anúncio fictício no painel do corretor e salve uma alteração. | O anúncio aprovado aparece na vitrine. Alterações em um anúncio publicado voltam para revisão. Fotos novas ficam em `imoveis/<id>/` no S3. |
| Visita | Como cliente verificado, abra o anúncio publicado pelo corretor usado no teste, selecione **Solicitar visita**, informe data a partir de amanhã, período e telefone, e envie. Entre como o corretor responsável, abra **Visitas** e confirme o pedido. | A visita aparece como pendente para o corretor vinculado ao imóvel na hora do pedido. Após a confirmação, o cliente vê o novo status em **Minha conta → Visitas**. |
| Lia | No site ou no Android, pergunte por imóveis para alugar, como solicitar visita e faça uma pergunta fora do tema. | A Lia mostra links do catálogo, explica o pedido de visita e responde genericamente fora do escopo. A opção Groq requer a chave configurada na EC2. |
| Privacidade | Em **Minha conta → Privacidade**, use uma conta fictícia separada para testar exportação e exclusão. | A conta pode baixar os próprios dados. Após concluir a exclusão, a sessão é encerrada. |

Para testar o encaminhamento de visitas com anúncios já existentes, confirme o responsável antes de enviar o pedido. O sistema grava o corretor no momento da solicitação. Trocar o responsável de um anúncio afeta as próximas visitas. Para testar a saída de um anúncio da vitrine, use **Arquivar** num imóvel fictício que você reservou para esse fim.

### Verificações automatizadas

- Backend, a partir de `imoveisapi`: `.\mvnw.cmd -B -Ptestcontainers verify` com Docker ativo. Os testes de integração também aceitam `TEST_DB_URL`, `TEST_DB_USER` e `TEST_DB_PASSWORD` de um schema descartável `talisma_test`.
- Frontend, a partir de `talisma-imoveis`: `npm run typecheck`, `npm run lint`, `npm run build` e `npx playwright install chromium`. Na revisão com ViaCEP, execute `npm run e2e -- privacidade.spec.ts mascaras-cep.spec.ts`. Para usar a fixture, deixe `API_URL` e `E2E_BASE_URL` ausentes no terminal de testes.
- Android, a partir de `talisma-app`: `.\gradlew.bat :app:assembleDebug :app:lintDebug` com JDK 21 e `API_URL` configurada.
- A partir da raiz, confira as cópias dos textos legais com `python scripts/sincronizar-legais.py --check`.

## 6. Resolver problemas comuns

| Sintoma | O que verificar |
|---|---|
| Porta 3000 ocupada | Pare o Next.js anterior ou escolha outra porta com `--port`. |
| Flyway falha na AWS | Na EC2, confira o endpoint de escrita, o truststore TLS e os privilégios da conta de migração no schema `talisma`. |
| Lia responde com indisponibilidade | Confira `127.0.0.1:8000/health` na EC2, a rota `POST /chat` no Nginx e os logs do contêiner. |
| Site abre, mas catálogo está vazio | Confira o `API_URL`, a base selecionada e se há anúncios em `PUBLICADO`. |
| HTTPS falha após reiniciar o Learner Lab | Confira o novo IPv4 da EC2, o certificado do Nginx e o `API_URL` usado no Next.js e no build Android. |
| Foto falha na AWS | Confirme que o anúncio está publicado e verifique bucket, região e acesso do perfil EC2 à chave da foto. |
| E-mail de teste não aparece | Confira o e-mail informado, o estado do Mailpit na EC2 e se o túnel continua aberto. Na tela de verificação, use **Preciso de outro código** para reenviar. |
| Visita não aparece no painel do corretor | Confira a conta em uso, o responsável no momento do pedido e se a solicitação foi enviada. O administrador também consegue consultar visitas. |
