# AWS Academy: criação e configuração completa do backend Talismã

**Atualizado em 18/11/2026.** Siga as etapas na ordem. Os nomes abaixo são o padrão deste roteiro; anote os IDs e endereços efetivamente gerados na sua conta.

O resultado será: frontend no seu computador → HTTPS da EC2 → API Spring em Docker → RDS MySQL privado e S3 privado. O Android acessará a mesma API HTTPS. E-mails de cadastro, recuperação e atendimento funcionarão no **Mailpit privado**, consultado por túnel SSH; não serão enviados a caixas de e-mail reais.

Este arquivo é o roteiro detalhado de criação. O [guia operacional](aws-academy.md) complementa pipeline, rollback e cuidados com restauração. A infraestrutura descrita ainda precisa ser criada por você; este documento não é comprovante de implantação.

## 1. Antes de começar

1. Abra **AWS Academy → Learner Lab → Start Lab**. Espere o indicador verde e abra a console pelo botão **AWS**.
2. Confira que está na conta do laboratório, com crédito disponível, e selecione **US East (N. Virginia) / us-east-1**.
3. Leia as restrições da sua turma: tipos de EC2/RDS, MySQL 8.4, criação de VPC, S3 e associação de `LabInstanceProfile`. As opções permitidas são definidas pelo laboratório. Se uma opção abaixo retornar `AccessDenied`, não tente contornar a restrição com recurso público; confira com o professor.
4. No computador, tenha PowerShell, OpenSSH (`ssh`, `scp`), `tar.exe`, Python 3.11+ e os clientes MySQL para exportar a base local. O backend da EC2 será construído com Docker, incluindo Java 25; não será necessário instalar MySQL Server na EC2.
5. Faça a atividade com dados fictícios, conforme combinado. Não coloque credenciais, chaves `.pem` ou backups no Git.

**Como ler os comandos:** blocos `powershell` rodam no seu Windows; blocos `bash` rodam no terminal SSH da EC2. `IP_PUBLICO`, `ENDPOINT_RDS` e `NOME_BUCKET` são marcadores a substituir pelos seus valores. Não copie esses marcadores como se fossem endereços reais.

## 2. Nomes e tamanhos a utilizar

| Recurso/campo | Valor deste roteiro |
|---|---|
| Região | `us-east-1` |
| Tag `Projeto` | `TalismaImoveis` |
| Tag `Ambiente` | `Academico` |
| VPC | `talisma-vpc` — `10.20.0.0/16` |
| Subnet pública EC2 | `talisma-publica-a` — `10.20.1.0/24` — `us-east-1a` |
| Subnet privada RDS A | `talisma-privada-a` — `10.20.11.0/24` — `us-east-1a` |
| Subnet privada RDS B | `talisma-privada-b` — `10.20.12.0/24` — `us-east-1b` |
| Internet Gateway | `talisma-igw` |
| Tabela de rotas pública | `talisma-rt-publica` |
| Tabela de rotas privada | `talisma-rt-privada` |
| Security group API | `talisma-api-sg` |
| Security group banco | `talisma-rds-sg` |
| Key pair EC2 | `talisma-lab-key` — RSA — `.pem` |
| Instância EC2 / tag Name | `talisma-api-ec2` |
| Sistema da EC2 | Ubuntu Server **24.04 LTS**, **64 bits x86_64** |
| Tipo EC2 | **`t3.medium`**, 2 vCPUs e 4 GiB RAM, se permitido |
| Volume raiz EC2 | **20 GiB gp3**, criptografado |
| Perfil IAM da EC2 | **`LabInstanceProfile`**, quando disponibilizado pelo laboratório |
| Identificador RDS | `talisma-mysql-lab` |
| Engine RDS | **MySQL**, linha **8.4**, patch suportado disponível na sua console |
| Classe RDS | **`db.t3.micro`**, se oferecida para a versão e permitida pelo laboratório |
| Disponibilidade RDS | **Single DB instance / Single-AZ** |
| Armazenamento RDS | **20 GiB gp3**, criptografado, sem aumento automático para o laboratório |
| DB subnet group | `talisma-rds-subnets` |
| Parameter group | `talisma-mysql84-params`, família `mysql8.4` |
| Banco/schema dentro do MySQL | **`talisma`**, sem hífen ou acento |
| Usuário master RDS | `talisma_admin` |
| Usuário de execução | `talisma_app` |
| Usuário de migrations | `talisma_migracao` |
| Bucket | `talisma-imoveis-lab-CONTA-us-east-1`, substituindo `CONTA` pelo ID da conta de 12 dígitos |
| Tipo do bucket / armazenamento | **General purpose / S3 Standard** |
| Diretório privado na EC2 | `/opt/talisma` |
| Código na EC2 | `/opt/talisma/projeto` |

O bucket precisa ter nome globalmente único e apenas letras minúsculas, números e hífens neste padrão. Se o nome já existir, acrescente um sufixo como `-a1b2`. O identificador do RDS, o endpoint do RDS e o nome do banco `talisma` são três coisas diferentes.

Não há AMI ID fixo no documento: selecione a imagem oficial da **Canonical** para Ubuntu 24.04 x86_64, porque IDs de imagens variam e são atualizados. Se os tipos/engine indicados não estiverem disponíveis no seu Learner Lab, registre a restrição antes de escolher uma alternativa; não troque MySQL por Aurora ou PostgreSQL.

## 3. Criar a rede

### 3.1. VPC

Na busca da console, abra **VPC → Your VPCs / Suas VPCs → Create VPC**:

| Campo | Selecionar |
|---|---|
| Resources to create | **VPC only** |
| Name tag | `talisma-vpc` |
| IPv4 CIDR manual input | `10.20.0.0/16` |
| IPv6 CIDR | Nenhum |
| Tenancy | Default |

Crie a VPC. Selecione-a e vá em **Actions → Edit VPC settings**: deixe **Enable DNS resolution** e **Enable DNS hostnames** habilitados. Anote seu `vpc-...`.

### 3.2. Três subnets

Em **Subnets → Create subnet**, escolha `talisma-vpc` e crie as três subnets exatamente com os nomes, zonas e CIDRs da tabela da seção 2. Não reutilize o mesmo CIDR.

Selecione apenas `talisma-publica-a` → **Actions → Edit subnet settings** → habilite **Enable auto-assign public IPv4 address**. Não habilite essa opção nas duas subnets privadas.

### 3.3. Internet Gateway e rotas

1. Em **Internet gateways → Create internet gateway**, crie `talisma-igw`.
2. Selecione-o → **Actions → Attach to VPC** → escolha `talisma-vpc`.
3. Em **Route tables → Create route table**, crie `talisma-rt-publica` dentro de `talisma-vpc`.
4. Em **Routes → Edit routes**, mantenha a rota `local` e acrescente `0.0.0.0/0` com destino **Internet Gateway → talisma-igw**.
5. Em **Subnet associations → Edit subnet associations**, associe **somente `talisma-publica-a`**.
6. Crie `talisma-rt-privada` na mesma VPC. Mantenha **somente a rota local** e associe `talisma-privada-a` e `talisma-privada-b`.

Não crie NAT Gateway, VPC Endpoint, Load Balancer ou Elastic IP neste roteiro. A EC2 terá saída pelo Internet Gateway; o RDS se comunica com ela pela rede privada. Não é necessário criar regras de Network ACL específicas: use a ACL padrão e restrinja os acessos pelos security groups.

**Conferência:** apenas a subnet da EC2 tem rota para o Internet Gateway; o DB subnet group usará as duas privadas.

## 4. Criar os security groups

Abra **EC2 → Network & Security → Security Groups → Create security group**. Crie os dois grupos em **`talisma-vpc`**.

### `talisma-api-sg`

Descrição: `HTTPS da API Talisma e SSH do aluno`.

| Tipo de entrada | Porta | Origem |
|---|---:|---|
| SSH | 22 | **My IP / Meu IP**, que gera seu IP público com `/32` |
| HTTP | 80 | `0.0.0.0/0` |
| HTTPS | 443 | `0.0.0.0/0` |

Mantenha a regra padrão de saída **All traffic → 0.0.0.0/0**. O HTTP será usado para validar e renovar o certificado e redirecionar o restante a HTTPS. O roteiro não usa IPv6.

### `talisma-rds-sg`

Descrição: `MySQL privado acessivel somente pela EC2 Talisma`.

| Tipo de entrada | Porta | Origem |
|---|---:|---|
| MySQL/Aurora | 3306 | **Custom → security group `talisma-api-sg`** |

A origem deve ser o ID `sg-...` da API, selecionado no campo, e não o IP público da EC2. Mantenha a saída padrão.

**Conferência:** não crie entrada pública para **3306**, **8080**, **8025** ou **1025**. Quando seu IP mudar, atualize apenas a origem da regra SSH; não abra SSH para toda a internet.

## 5. Criar a EC2

1. Vá a **EC2 → Instances → Launch instances**.
2. Preencha a configuração:

| Campo da console | Valor |
|---|---|
| Name | `talisma-api-ec2` |
| Application and OS Images | Ubuntu Server **24.04 LTS**, publicador **Canonical** |
| Architecture | **64-bit (x86)** / x86_64 |
| Instance type | `t3.medium` |
| Number of instances | `1` |
| Key pair | **Create new key pair → talisma-lab-key → RSA → .pem** |
| Network / VPC | `talisma-vpc` |
| Subnet | `talisma-publica-a` |
| Auto-assign public IP | **Enable** |
| Firewall | **Select existing security group → talisma-api-sg**; remova seleções adicionais |
| Storage | 20 GiB, **gp3**, criptografado com chave gerenciada AWS disponível |
| Storage → Delete on termination | Habilitado para o volume da demonstração |
| Advanced details → IAM instance profile | `LabInstanceProfile`, se fornecido pelo laboratório |
| Advanced details → Metadata accessible | **Enabled** |
| Metadata version | **V2 only (token required)** |
| Metadata response hop limit | **2**, necessário para o SDK acessar IMDSv2 a partir do container |
| Detailed monitoring | Desabilitado |
| User data | Deixar vazio; usaremos o terminal SSH |

Baixe a chave `.pem` uma única vez e guarde-a fora do projeto, por exemplo em `C:\Users\SEU_USUARIO\.ssh\talisma-lab-key.pem`. Não use T4g/ARM com este roteiro x86_64.

Clique **Launch instance**. Aguarde **Running** e as verificações de status aprovadas. Anote **Instance ID**, **Public IPv4 address** e **Private IPv4 address**.

Se o perfil não foi associado na criação: selecione a instância → **Actions → Security → Modify IAM role** → escolha o instance profile disponibilizado. Não crie access keys permanentes para substituir o papel.

**Conferência:** aba Security mostra `talisma-api-sg`; aba Networking mostra a VPC e subnet corretas; a instância possui IPv4 público.

## 6. Criar o RDS

### 6.1. DB subnet group

Em **RDS → Subnet groups → Create DB subnet group**:

| Campo | Valor |
|---|---|
| Name | `talisma-rds-subnets` |
| Description | `Subnets privadas do MySQL Talisma` |
| VPC | `talisma-vpc` |
| Availability Zones | `us-east-1a` e `us-east-1b` |
| Subnets | **Somente `talisma-privada-a` e `talisma-privada-b`** |

Crie o grupo. Mesmo sendo Single-AZ, o DB subnet group deve cobrir ao menos duas zonas.

### 6.2. Parameter group

Em **Parameter groups → Create parameter group**:

| Campo | Valor |
|---|---|
| Family | `mysql8.4` |
| Type | DB parameter group, se houver seleção |
| Name | `talisma-mysql84-params` |
| Description | `MySQL 8.4 Talisma com TLS` |

Depois de criar, edite os parâmetros **quando disponibilizados nessa família**:

| Parâmetro | Valor |
|---|---|
| `require_secure_transport` | `ON` ou `1`, conforme a apresentação do campo |
| `character_set_server` | `utf8mb4` |
| `collation_server` | `utf8mb4_0900_ai_ci` |
| `time_zone` | `UTC` |

Salve. Se houver indicação `pending-reboot` depois de associar o grupo ao banco, reinicie a instância RDS pela console e espere **Available** antes de prosseguir. Não desative TLS para resolver erro de certificado.

### 6.3. Banco RDS

Vá a **RDS → Databases → Create database**. Use **Standard create**, pois precisamos controlar todos os campos.

| Seção/campo | Selecionar |
|---|---|
| Engine type | **MySQL**, não Aurora |
| Engine version | **8.4.x**, patch suportado que a console oferecer |
| Template | **Dev/Test**; ajuste manualmente os campos abaixo |
| Availability and durability | **Single DB instance / Single-AZ** |
| DB instance identifier | `talisma-mysql-lab` |
| Master username | `talisma_admin` |
| Credentials management | **Self managed**; não usar Secrets Manager neste roteiro |
| Master password | Crie senha forte e exclusiva, anote em gerenciador privado; não há senha padrão |
| DB instance class | Burstable classes → `db.t3.micro`, se disponível e autorizada |
| Storage type | **General Purpose SSD (gp3)** |
| Allocated storage | **20 GiB** |
| Storage autoscaling | Desabilitado para manter o limite planejado do laboratório |
| Compute resource | **Don't connect to an EC2 compute resource**; faremos a ligação pelos grupos já criados |
| VPC | `talisma-vpc` |
| DB subnet group | `talisma-rds-subnets` |
| Public access | **No / Não** |
| VPC security group | Existing → **somente `talisma-rds-sg`**, sem o grupo default |
| Availability Zone | No preference |
| RDS Proxy | Não criar |
| Certificate authority | CA gerenciada atual disponibilizada pelo RDS; usaremos o bundle oficial regional |
| Database authentication | **Password authentication** |
| Enhanced Monitoring | Desabilitado, evitando criar papel adicional |
| Database Insights | **Standard**, se obrigatório; não ativar Advanced |
| Performance Insights | Desabilitado se a console apresentar a opção antiga |
| Initial database name | **`talisma`** |
| DB parameter group | `talisma-mysql84-params` |
| Option group | Padrão compatível com MySQL 8.4 |
| Automated backups | Habilitados, retenção **7 dias**, se o laboratório permitir |
| Encryption | Habilitada, chave gerenciada **aws/rds** disponível no laboratório |
| Log exports | Não habilitar exportações adicionais neste laboratório |
| Auto minor version upgrade | Habilitado; não escolher upgrade de versão principal |
| Deletion protection | Desabilitada nesta demonstração para permitir limpeza ao final |

Não escolha uma opção “free tier” presumindo que o saldo do Learner Lab seja ilimitado. Confira a combinação oferecida e os serviços autorizados. Se MySQL 8.4 ou a classe indicada não existir, valide a alternativa com o professor antes de criar: o projeto foi testado localmente com MySQL 8.0.46, mas isso não substitui homologar a versão remota escolhida.

Crie o banco e espere **Available**. Abra **Connectivity & security** e anote o **Endpoint**, semelhante a `talisma-mysql-lab.XXXXXXXXXXXX.us-east-1.rds.amazonaws.com`, e a porta **3306**. Não adicione `https://` ao endpoint JDBC/MySQL.

**Conferência:** Publicly accessible = **No**, VPC = `talisma-vpc`, SG = `talisma-rds-sg`, banco inicial = `talisma`.

## 7. Criar o bucket S3

Em **S3 → Buckets → Create bucket**:

| Campo | Valor |
|---|---|
| AWS Region | **us-east-1** |
| Bucket type | **General purpose** |
| Bucket name | `talisma-imoveis-lab-CONTA-us-east-1`, substituindo CONTA; sufixo extra se necessário |
| Object Ownership | **ACLs disabled / Bucket owner enforced** |
| Block Public Access | **Todas as opções marcadas** |
| Bucket Versioning | **Disable** nesta demonstração |
| Tags | `Projeto=TalismaImoveis`, `Ambiente=Academico` |
| Default encryption | **SSE-S3 / Amazon S3 managed keys** |
| Bucket Key | Não se aplica ao SSE-S3; não trocar para KMS neste roteiro |
| Object Lock | Desabilitado |

Crie o bucket. Não habilite static website hosting, não crie ACL pública, não configure CORS e não gere URLs públicas permanentes. A aplicação entrega as fotos pelas rotas da API e verifica o estado do anúncio antes de buscar o objeto privado.

### 7.1. Permissões do papel da EC2

No Learner Lab, `LabInstanceProfile` costuma associar o papel `LabRole`; confirme o nome exibido na sua conta. O perfil existente pode já permitir o acesso. **Não substitua a política de um papel compartilhado do laboratório.**

Estas são as permissões necessárias sobre **seu bucket**; o JSON abaixo é referência para o professor/administrador caso a turma precise liberá-las:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": "arn:aws:s3:::NOME_BUCKET"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::NOME_BUCKET/*"
    }
  ]
}
```

Mantenha o bucket privado mesmo se o teste retornar `AccessDenied`. Não coloque chave AWS no frontend, no APK ou no `runtime.env`.

### 7.2. Transporte HTTPS e backups

Se o laboratório permitir editar a política **do seu bucket novo**, em **Permissions → Bucket policy** acrescente a negação abaixo, substituindo `NOME_BUCKET` nas duas ocorrências. Ela **não concede acesso público**, apesar de usar Principal `*`; apenas nega HTTP sem TLS. Não substitua políticas de buckets que já pertencem a outra atividade.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "NegarTransporteSemTLS",
    "Effect": "Deny",
    "Principal": "*",
    "Action": "s3:*",
    "Resource": ["arn:aws:s3:::NOME_BUCKET", "arn:aws:s3:::NOME_BUCKET/*"],
    "Condition": {"Bool": {"aws:SecureTransport": "false"}}
  }]
}
```

Se armazenar backups no prefixo `backups/`, crie em **Management → Lifecycle rules** a regra `talisma-backups-7-dias`, filtro **prefixo `backups/`**, expiração de objetos atuais após **7 dias**. Essa regra não cria backups e não pode ser aplicada à raiz do bucket, onde ficarão as fotos ativas.

## 8. Conectar ao Ubuntu por SSH

No PowerShell do Windows, ajuste os dois valores:

```powershell
$ChaveTalisma = "$env:USERPROFILE\.ssh\talisma-lab-key.pem"
$IpTalisma = 'IP_PUBLICO'
ssh -i $ChaveTalisma "ubuntu@$IpTalisma"
```

O usuário da AMI Ubuntu é **`ubuntu`**, não `ec2-user`. Na primeira conexão, compare a impressão digital do host com a informação da instância na console (system log, quando disponibilizada). Não use `StrictHostKeyChecking=no`.

Se o OpenSSH reclamar de permissões da sua chave privada, ajuste somente o arquivo da chave no Windows:

```powershell
$SidTalisma = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
icacls.exe $ChaveTalisma /inheritance:r
icacls.exe $ChaveTalisma /grant:r "*${SidTalisma}:(R)"
```

## 9. Instalar as ferramentas na EC2

Os blocos desta seção rodam no **Ubuntu via SSH**. Instale utilitários, Nginx, cliente MySQL e o keytool. O Java 21 do host será usado apenas para o truststore; **a API usa Java 25 dentro da imagem Docker**.

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg unzip python3 openssl \
  nginx mysql-client openjdk-21-jdk-headless snapd
sudo systemctl enable --now nginx
sudo install -d -m 700 -o ubuntu -g ubuntu /opt/talisma
```

Em uma EC2 nova, instale Docker Engine e Compose pelo repositório oficial:

```bash
sudo install -d -m 0755 /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
. /etc/os-release
ARQUITETURA=$(dpkg --print-architecture)
printf 'deb [arch=%s signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu %s stable\n' \
  "$ARQUITETURA" "$VERSION_CODENAME" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
```

Saia com `exit` e conecte novamente por SSH para carregar o grupo Docker. Esse grupo dá controle administrativo sobre o host; mantenha o acesso restrito ao usuário de implantação.

Instale AWS CLI v2 pelo pacote oficial x86_64:

```bash
mkdir -p /opt/talisma/ferramentas
cd /opt/talisma/ferramentas
curl -fL https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o awscliv2.zip
unzip -q awscliv2.zip
sudo ./aws/install
docker --version
docker compose version
aws --version
mysql --version
keytool -help
```

Se já existir AWS CLI v2, não reinstale às cegas; confira `aws --version` e siga a atualização oficial se precisar.

## 10. Verificar o papel AWS e o S3

Na EC2, sem executar `aws configure` e sem copiar credenciais temporárias do aluno:

```bash
export AWS_DEFAULT_REGION=us-east-1
export NOME_BUCKET='NOME_BUCKET'
aws sts get-caller-identity
```

O retorno deve mostrar uma identidade `assumed-role/...` vinculada ao papel da instância. `Unable to locate credentials` indica perfil ausente/metadata inacessível; não indica necessidade de criar access key.

Faça um teste com um objeto diagnóstico de nome único, sem tocar nas fotos:

```bash
CHAVE_TESTE="diagnostico/$(date +%s)-$(openssl rand -hex 4).txt"
printf 'Conexao S3 Talisma\n' > /opt/talisma/teste-s3.txt
aws s3api put-object --bucket "$NOME_BUCKET" --key "$CHAVE_TESTE" \
  --body /opt/talisma/teste-s3.txt --server-side-encryption AES256
aws s3api get-object --bucket "$NOME_BUCKET" --key "$CHAVE_TESTE" /opt/talisma/teste-s3-baixado.txt
cmp /opt/talisma/teste-s3.txt /opt/talisma/teste-s3-baixado.txt
aws s3api delete-object --bucket "$NOME_BUCKET" --key "$CHAVE_TESTE"
```

`cmp` sem saída significa que os arquivos são iguais. O último comando remove apenas o objeto diagnóstico recém-criado. O teste real da API no container será feito com upload de foto na seção 19; ele também confirma o hop limit 2.

## 11. Configurar certificados para conectar ao RDS

Ainda na EC2:

```bash
export ENDPOINT_RDS='ENDPOINT_RDS'
cd /opt/talisma
curl -fsSL https://truststore.pki.rds.amazonaws.com/us-east-1/us-east-1-bundle.pem -o rds-ca.pem
mkdir -p ca-rds
awk '/-----BEGIN CERTIFICATE-----/{n++} n {print > ("ca-rds/ca-" n ".pem")}' rds-ca.pem
for ca in ca-rds/*.pem; do
  keytool -importcert -noprompt -storetype PKCS12 -keystore rds-truststore.p12 \
    -storepass changeit -alias "$(basename "$ca" .pem)" -file "$ca"
done
chmod 644 rds-ca.pem rds-truststore.p12
```

Execute a importação uma vez em uma instalação nova. Se repetir e aparecer que o alias já existe, não apague o truststore de um ambiente em uso; confira os certificados com `keytool -list -keystore /opt/talisma/rds-truststore.p12 -storepass changeit`.

`changeit` protege um arquivo de **certificados públicos**, não é senha do MySQL nem chave privada. O arquivo precisa ser legível pelo usuário 10001 do container. Por isso o truststore é 644, enquanto o diretório pai e arquivos de credenciais permanecem restritos.

Teste conexão e TLS com a senha master criada na console:

```bash
MYSQL_HISTFILE=/dev/null mysql --host="$ENDPOINT_RDS" --port=3306 \
  --user=talisma_admin -p --ssl-mode=VERIFY_IDENTITY --ssl-ca=/opt/talisma/rds-ca.pem \
  talisma -e "SELECT DATABASE(), VERSION(); SHOW SESSION STATUS LIKE 'Ssl_cipher';"
```

O resultado precisa mostrar o banco `talisma`, a versão escolhida e um `Ssl_cipher` preenchido. Se `Unknown database 'talisma'` aparecer, crie pelo cliente master `CREATE DATABASE talisma CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;` e confira o campo Initial database name que faltou.

## 12. Enviar o código atual do computador

Este método empacota o **estado atual dos arquivos**, inclusive alterações ainda não commitadas. Não usa `git archive HEAD`, que deixaria de fora arquivos novos não commitados.

No PowerShell, na raiz do projeto:

```powershell
Set-Location 'C:\Users\25170641\Desktop\Projeto_Integrador_Talisma'
New-Item -ItemType Directory -Force .local | Out-Null
tar.exe -czf .local/talisma-release.tar.gz `
  --exclude=.git --exclude=.idea --exclude=target --exclude=node_modules `
  --exclude=config --exclude=fotos-local --exclude=.env --exclude='.env.*' `
  --exclude='*.pem' --exclude='*.p12' --exclude='*.jks' `
  'Dev BackEnd/imoveisapi' 'Dev BancoDados' infra scripts .dockerignore
if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar o pacote' }
tar.exe -tzf .local/talisma-release.tar.gz
scp -i $ChaveTalisma .local/talisma-release.tar.gz "ubuntu@${IpTalisma}:/opt/talisma/"
if ($LASTEXITCODE -ne 0) { throw 'Falha no envio' }
```

Confira a listagem antes de enviar: não deve conter senha local, `config/`, `.env`, `.pem`, diretório de fotos, banco ou backup. Fotos/dados serão enviados separadamente na próxima seção. O pacote contém fontes e migrations; não é preciso enviar o JAR do seu computador.

Na EC2:

```bash
mkdir -p /opt/talisma/projeto
tar -xzf /opt/talisma/talisma-release.tar.gz -C /opt/talisma/projeto
cd /opt/talisma/projeto
test -f 'Dev BackEnd/imoveisapi/Dockerfile'
test -f 'Dev BancoDados/DDL/V6__registro_exclusao_backup.sql'
test -f infra/aws/compose.yaml
```

Não extraia uma segunda revisão por cima desta pasta sem controlar arquivos antigos. Para atualizações, use uma pasta de release nova, conforme o guia operacional/pipeline.

## 13. Migrar seu banco e suas fotos locais

Você escolheu preservar os dados fictícios existentes. **Execute esta seção antes das migrations na EC2**; a restauração exige o schema de destino sem tabelas. Se aplicar Flyway primeiro, não apague tabelas para forçar a importação: use um banco novo.

1. Pare a API local e interrompa as gravações pelo site e Android.
2. No PowerShell da raiz do projeto, exporte para uma pasta nova. Ajuste usuário, porta, caminhos dos clientes e diretório das fotos se forem diferentes:

```powershell
python scripts/migrar-dados.py exportar `
  --pasta .local/backup-migracao `
  --usuario SEU_USUARIO_MYSQL_LOCAL --port 3306 `
  --mysql 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe' `
  --mysqldump 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe' `
  --fotos 'Dev BackEnd/imoveisapi/fotos-local'
if ($LASTEXITCODE -ne 0) { throw 'Backup falhou; não prossiga' }
scp -i $ChaveTalisma -r .local/backup-migracao "ubuntu@${IpTalisma}:/opt/talisma/"
if ($LASTEXITCODE -ne 0) { throw 'Falha no envio do backup' }
```

A senha é solicitada sem aparecer na tela. O programa confere referências de fotos, contagens e SHA-256 e inclui o histórico Flyway. Use uma pasta nova se repetir; não sobrescreva o backup anterior.

Na EC2, com as variáveis `ENDPOINT_RDS` e `NOME_BUCKET` das seções anteriores:

```bash
cd /opt/talisma/projeto
python3 scripts/migrar-dados.py restaurar \
  --pasta /opt/talisma/backup-migracao \
  --host "$ENDPOINT_RDS" --usuario talisma_admin --ca /opt/talisma/rds-ca.pem
python3 scripts/migrar-dados.py s3 \
  --pasta /opt/talisma/backup-migracao --bucket "$NOME_BUCKET"
```

Só prossiga quando ambos terminarem com sucesso e sem divergência. O envio mantém a chave original de cada foto na raiz do bucket, compatível com `foto.arquivo`. Não crie prefixo `fotos/` para esses arquivos: o backend atual não adiciona esse prefixo.

Se o dump apresentar incompatibilidade de versão/DEFINER ou o script detectar histórico Flyway ausente, resolva a origem antes de continuar. Não remova o histórico, não use baseline/repair para ocultar diferenças e não execute os seeds sobre uma base já restaurada.

**Somente se estiver começando sem dados:** pule esta seção e mantenha `talisma` vazio; o Flyway criará as tabelas na seção 15. Essa alternativa não preserva seus registros atuais.

## 14. Gerar credenciais e configurar o backend

Na EC2, rode o bloco abaixo **uma única vez**. Ele gera senhas exclusivas, chave JWT e arquivos privados fora do repositório; não imprime segredos. Digite o endpoint DNS e o nome do bucket, sem marcadores e sem `https://`.

```bash
read -r -p 'Endpoint DNS do RDS: ' ENDPOINT_RDS
read -r -p 'Nome do bucket S3: ' NOME_BUCKET
export ENDPOINT_RDS NOME_BUCKET
python3 - <<'PY'
import os
import re
import secrets
import base64
from pathlib import Path

base = Path('/opt/talisma')
nomes = ('runtime.env', 'migration.env', 'bootstrap-db.sql')
if any((base / nome).exists() for nome in nomes):
    raise SystemExit('Configuração já existe. Preserve-a; não gere outras senhas às cegas.')
host = os.environ['ENDPOINT_RDS'].strip()
bucket = os.environ['NOME_BUCKET'].strip()
if not re.fullmatch(r'[a-zA-Z0-9.-]+', host) or not host.endswith('.rds.amazonaws.com'):
    raise SystemExit('Endpoint RDS inválido para este roteiro em us-east-1.')
if not re.fullmatch(r'[a-z0-9][a-z0-9-]{1,61}[a-z0-9]', bucket):
    raise SystemExit('Nome de bucket inválido para o padrão adotado.')
senha_app = secrets.token_hex(24)
senha_migracao = secrets.token_hex(24)
jwt = base64.b64encode(secrets.token_bytes(32)).decode('ascii')
url = (f'jdbc:mysql://{host}:3306/talisma?connectionTimeZone=UTC'
       '&sslMode=VERIFY_IDENTITY'
       '&trustCertificateKeyStoreUrl=file:/run/secrets/rds-truststore.p12'
       '&trustCertificateKeyStoreType=PKCS12'
       '&trustCertificateKeyStorePassword=changeit&fallbackToSystemTrustStore=false')
runtime = (f'DB_URL={url}\nDB_USER=talisma_app\nDB_PASSWORD={senha_app}\n'
           f'APP_JWT_KEY={jwt}\nS3_BUCKET={bucket}\nAWS_DEFAULT_REGION=us-east-1\n'
           'APP_MAIL_FROM=no-reply@example.test\n')
migracao = f'DB_MIGRATION_USER=talisma_migracao\nDB_MIGRATION_PASSWORD={senha_migracao}\n'
sql = (f"CREATE USER 'talisma_app'@'%' IDENTIFIED BY '{senha_app}' REQUIRE SSL;\n"
       "GRANT SELECT, INSERT, UPDATE, DELETE ON talisma.* TO 'talisma_app'@'%';\n"
       f"CREATE USER 'talisma_migracao'@'%' IDENTIFIED BY '{senha_migracao}' REQUIRE SSL;\n"
       "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES "
       "ON talisma.* TO 'talisma_migracao'@'%';\n")
for nome, texto in zip(nomes, (runtime, migracao, sql)):
    fd = os.open(base / nome, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w', encoding='utf-8') as saida:
        saida.write(texto)
print('Arquivos privados criados. Nenhuma senha foi exibida.')
PY
```

Aplique a criação das contas com o usuário master do RDS:

```bash
MYSQL_HISTFILE=/dev/null mysql --host="$ENDPOINT_RDS" --port=3306 \
  --user=talisma_admin -p --ssl-mode=VERIFY_IDENTITY --ssl-ca=/opt/talisma/rds-ca.pem \
  talisma < /opt/talisma/bootstrap-db.sql
```

Se o comando retornar erro, pare e confira. Em especial, `CREATE USER failed` pode indicar contas existentes; não use `IF NOT EXISTS` para ignorar e deixar a aplicação com uma senha diferente da gravada no MySQL. Não execute novamente o gerador nesse caso.

Após sucesso, remova somente o SQL temporário com as senhas duplicadas:

```bash
rm -- /opt/talisma/bootstrap-db.sql
stat -c '%a %n' /opt/talisma/runtime.env /opt/talisma/migration.env
```

Os dois arquivos devem ter permissão **600**. A conta master não entra no runtime. `talisma_app` tem apenas operações de dados; `talisma_migracao` tem também os privilégios de DDL usados pelas migrations atuais, limitados a `talisma.*`. O wildcard MySQL `%` permite a conexão variável da EC2; a restrição de rede é feita pelo SG privado e a conexão exige TLS.

| Configuração final | Origem |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_JWT_KEY` | Arquivo privado `/opt/talisma/runtime.env` |
| `S3_BUCKET`, `AWS_DEFAULT_REGION` | Mesmo arquivo privado |
| `SPRING_PROFILES_ACTIVE=aws` | Definido pelo Compose |
| `APP_STORAGE=s3`, bind `0.0.0.0`, SMTP `mailpit:1025` | Perfil `application-aws.properties` |
| Credenciais AWS | Papel temporário da EC2 via IMDSv2; não entram em nenhum `.env` |
| Senha DDL | Arquivo privado `migration.env`, carregado somente durante migrations |
| Truststore dentro do container | `/run/secrets/rds-truststore.p12`, montado a partir do host |

Não altere o `application.properties` para colocar endpoint e senha fixos no Git. Não execute `source runtime.env`: a URL contém `&` e o arquivo é destinado ao parser do Docker, não ao shell.

## 15. Construir a API e executar migrations

Na EC2:

```bash
cd /opt/talisma/projeto
export APP_IMAGE_TAG=lab-v1
docker compose -f infra/aws/compose.yaml config --quiet
docker compose -f infra/aws/compose.yaml build api
```

O contexto Docker é a raiz do projeto. O build inclui Maven, Java 25 e as migrations de `Dev BancoDados`; a imagem final executa como usuário 10001. Não rode o build com apenas a pasta Java enviada isoladamente.

Carregue **somente o arquivo gerado de migrations**, execute e remova as variáveis da sessão:

```bash
set -a
. /opt/talisma/migration.env
set +a
docker compose -f infra/aws/compose.yaml run --rm --no-deps \
  -e SPRING_PROFILES_ACTIVE=aws,migrar \
  -e DB_MIGRATION_USER -e DB_MIGRATION_PASSWORD api
RESULTADO_MIGRACAO=$?
unset DB_MIGRATION_USER DB_MIGRATION_PASSWORD
test "$RESULTADO_MIGRACAO" -eq 0
```

O último comando deve terminar com sucesso. Se falhar, **não continue para subir o serviço**. Confira no log “Successfully applied” ou “Schema ... is up to date”. O perfil `migrar` executa sem servidor HTTP e encerra sozinho. No perfil normal `aws`, o Flyway fica desabilitado para não exigir DDL da conta de execução.

Confirme o histórico pelo cliente MySQL master:

```bash
MYSQL_HISTFILE=/dev/null mysql --host="$ENDPOINT_RDS" --user=talisma_admin -p \
  --ssl-mode=VERIFY_IDENTITY --ssl-ca=/opt/talisma/rds-ca.pem talisma \
  -e "SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank;"
```

Na revisão atual, V1 a **V6** precisam estar com `success=1`. Versões futuras podem acrescentar linhas; não edite migrations já aplicadas.

## 16. Subir os serviços e garantir o administrador

Suba primeiro o Mailpit e a API:

```bash
cd /opt/talisma/projeto
export APP_IMAGE_TAG=lab-v1
docker compose -f infra/aws/compose.yaml up -d
docker compose -f infra/aws/compose.yaml ps
curl --fail http://127.0.0.1:8080/actuator/health
```

Espere a API iniciar. A resposta de saúde deve conter **`"status":"UP"`**. Isso confirma aplicação/banco, mas não substitui testar o S3. Consulte erros com `docker compose -f infra/aws/compose.yaml logs --tail=100 api`; não publique segredos de arquivos de configuração nem use `docker inspect` para compartilhar ambiente.

### 16.1. Verificar se o administrador veio da migração

```bash
MYSQL_HISTFILE=/dev/null mysql --host="$ENDPOINT_RDS" --user=talisma_admin -p \
  --ssl-mode=VERIFY_IDENTITY --ssl-ca=/opt/talisma/rds-ca.pem talisma \
  -e "SELECT id,nome,email FROM usuario WHERE perfil='ADMIN';"
```

Se já houver um administrador, **pule o provisionamento** e use essa conta. Não crie administrador inserindo senha em texto na tabela.

### 16.2. Criar o primeiro administrador, somente se não existir

Na sessão SSH:

```bash
read -r -p 'Nome do administrador: ' ADMIN_NOME
read -r -p 'E-mail fictício do administrador: ' ADMIN_EMAIL
read -r -s -p 'Senha do administrador (8 a 72 bytes): ' ADMIN_SENHA
printf '\n'
export ADMIN_NOME ADMIN_EMAIL ADMIN_SENHA
docker compose -f infra/aws/compose.yaml run --rm --no-deps -d \
  --name talisma-admin-bootstrap \
  -e SPRING_PROFILES_ACTIVE=aws,provisionar-admin \
  -e ADMIN_NOME -e ADMIN_EMAIL -e ADMIN_SENHA api
unset ADMIN_NOME ADMIN_EMAIL ADMIN_SENHA
```

Use e-mail de demonstração, por exemplo `admin@example.test`, e guarde sua senha privada. Não há credencial administrativa padrão.

Espere alguns segundos, consulte `docker logs --tail=60 talisma-admin-bootstrap` e repita a consulta SQL da seção 16.1 para confirmar que a linha ADMIN foi criada. Esse perfil **não encerra sozinho**, pois há tarefas agendadas na aplicação. Depois da confirmação, pare apenas o container temporário:

```bash
docker stop talisma-admin-bootstrap
```

O container temporário foi iniciado com `--rm` e será removido; a API principal continua ativa. Não deixe o perfil de provisionamento na configuração permanente. O sistema recusa provisionar outro administrador quando já existe um.

## 17. Configurar HTTPS sem domínio

O Android remoto exige HTTPS válido. Este roteiro usa certificado de **IP público**, sem comprar domínio. A emissão/renovação precisa que a EC2 esteja acessível na porta 80 e conserve o IP usado no certificado.

### 17.1. Preparar Nginx apenas com HTTP para o desafio

Na EC2, substitua o marcador pelo IPv4 atual:

```bash
export IP_PUBLICO='IP_PUBLICO'
sudo mkdir -p /var/www/acme
sudo tee /etc/nginx/sites-available/talisma-api >/dev/null <<'NGINX'
server {
    listen 80;
    server_name IP_PUBLICO;
    location /.well-known/acme-challenge/ { root /var/www/acme; }
    location / { return 404; }
}
NGINX
sudo sed -i "s/IP_PUBLICO/$IP_PUBLICO/g" /etc/nginx/sites-available/talisma-api
sudo ln -s /etc/nginx/sites-available/talisma-api /etc/nginx/sites-enabled/talisma-api
sudo nginx -t
sudo systemctl reload nginx
```

O `ln -s` é para a instalação nova; se o link já existir, confira o destino em vez de substituir arquivos. O site default do Nginx não contém a API; não altere sites de outras atividades. A requisição com o IP correto será direcionada ao `server_name` acima.

Crie uma prova de acesso ao desafio:

```bash
sudo mkdir -p /var/www/acme/.well-known/acme-challenge
printf 'ok\n' | sudo tee /var/www/acme/.well-known/acme-challenge/talisma-teste >/dev/null
```

No Windows, teste `curl.exe http://IP_PUBLICO/.well-known/acme-challenge/talisma-teste`. Deve retornar `ok`. Se não funcionar, resolva SG, IP, rota pública e Nginx antes de emitir certificado.

### 17.2. Emitir e instalar o certificado

Na EC2:

```bash
sudo systemctl enable --now snapd.socket
sudo snap install --classic certbot
sudo /snap/bin/certbot --version
```

Use Certbot **5.4 ou superior** para o modo webroot com IP. Não use o plugin `--nginx` para esta emissão. Execute:

```bash
sudo /snap/bin/certbot certonly --webroot --webroot-path /var/www/acme \
  --preferred-profile shortlived --ip-address "$IP_PUBLICO" --cert-name talisma-api
```

Forneça um e-mail real de contato do operador se solicitado e leia/aceite os termos do emissor por sua iniciativa. O e-mail não é credencial da aplicação. Não use `--staging` no certificado que os clientes utilizarão, pois o certificado de teste não é confiável nos navegadores/Android.

Confirme que foram criados `fullchain.pem` e `privkey.pem` em `/etc/letsencrypt/live/talisma-api/`. Depois instale a configuração definitiva do projeto:

```bash
cd /opt/talisma/projeto
sudo cp infra/aws/nginx.conf /etc/nginx/sites-available/talisma-api
sudo sed -i "s/IP_PUBLICO/$IP_PUBLICO/g" /etc/nginx/sites-available/talisma-api
sudo nginx -t
sudo systemctl reload nginx
curl --fail "https://$IP_PUBLICO/api/v1/legal" >/dev/null
```

O bloco HTTPS do projeto encaminha apenas `/api/v1/` a `127.0.0.1:8080`. `/actuator/health` permanece local; portanto é normal retornar 404 pelo IP público. A porta 8080 da aplicação e a interface Mailpit não ficam expostas.

### 17.3. Renovar automaticamente

```bash
sudo install -d -m 755 /etc/letsencrypt/renewal-hooks/deploy
sudo tee /etc/letsencrypt/renewal-hooks/deploy/talisma-nginx.sh >/dev/null <<'SH'
#!/bin/sh
/usr/sbin/nginx -t && /usr/bin/systemctl reload nginx
SH
sudo chmod 755 /etc/letsencrypt/renewal-hooks/deploy/talisma-nginx.sh
systemctl list-timers --all | grep -i certbot
sudo /snap/bin/certbot renew --dry-run
```

O timer precisa estar ativo e o teste precisa concluir. Esses certificados duram aproximadamente seis dias; um laboratório desligado por muito tempo pode impedir renovação. Antes da apresentação, confira `sudo /snap/bin/certbot certificates`.

## 18. Conectar frontend, Android e Mailpit

### 18.1. Frontend no Windows

```powershell
Set-Location 'C:\Users\25170641\Desktop\Projeto_Integrador_Talisma\Dev FrontEnd\talisma-imoveis'
$env:API_URL='https://IP_PUBLICO'
$env:COOKIE_SECURE='false'
npm ci
npm run dev
```

Abra `http://localhost:3000`. `COOKIE_SECURE=false` é somente para esse frontend em localhost HTTP; em hospedagem HTTPS deve ser `true`. `API_URL` é variável do servidor Next.js; não use prefixo `NEXT_PUBLIC_` e não coloque senha do RDS no frontend.

Depois de alterar `API_URL`, reinicie o processo Next.js. CORS amplo não é necessário porque o Next.js faz a chamada à API pelo servidor. Não acesse o RDS diretamente do navegador.

### 18.2. Mailpit e códigos de verificação

Abra outro PowerShell e mantenha o túnel em execução:

```powershell
$ChaveTalisma = "$env:USERPROFILE\.ssh\talisma-lab-key.pem"
$IpTalisma = 'IP_PUBLICO'
ssh -i $ChaveTalisma -N -L 127.0.0.1:8025:127.0.0.1:8025 "ubuntu@$IpTalisma"
```

Abra `http://127.0.0.1:8025`. Ao cadastrar uma conta fictícia no site, o código de **seis dígitos** aparecerá nessa interface. O mesmo vale para recuperação de senha. O SMTP é `mailpit:1025` na rede Docker e a interface está vinculada a loopback na EC2.

**Funcionalidade completa no laboratório:** esses e-mails são capturados, não entregues a destinatários reais. Se futuramente quiser entrega real, será necessário configurar um provedor SMTP, autenticação e TLS próprios; SES/SMTP real não fazem parte desta implantação acadêmica.

### 18.3. Android

Use **JDK 21 para executar o Gradle** e Android SDK 36. No PowerShell, a partir da pasta do app:

```powershell
Set-Location 'C:\Users\25170641\Desktop\Projeto_Integrador_Talisma\Dev Mobile\talisma-app'
.\gradlew.bat :app:assembleDebug -PapiUrl=https://IP_PUBLICO
```

Se seu terminal estiver com Java 25, configure `JAVA_HOME` para um JDK 21 antes de executar Gradle. O JDK 21 de teste desta máquina, se ainda estiver disponível, está em `.local/jdk21/jdk-21.0.13+9` na raiz do projeto; em outro computador instale um JDK 21 próprio.

O APK fica em `app/build/outputs/apk/debug/app-debug.apk`. Instale no aparelho, desligue o modo demonstração no Perfil e utilize uma conta da API real. O IP da API fica no build: se mudar, é necessário recompilar/atualizar a configuração. Não libere cleartext HTTP remoto nem ignore erros de certificado.

## 19. Testar o backend integrado

Faça os testes nesta ordem e use contas/imóveis fictícios:

| Teste | Procedimento | Resultado esperado |
|---|---|---|
| Saúde local | Na EC2, `curl --fail http://127.0.0.1:8080/actuator/health` | `status: UP` |
| HTTPS público | Abra `https://IP_PUBLICO/api/v1/legal` | JSON com os textos/versões, sem alerta de certificado |
| Banco privado | Console RDS → Connectivity | Public access = No, SG só aceita o grupo da EC2 |
| Administrador | Entre pelo frontend com a conta migrada/provisionada | Acesso ao painel interno |
| Corretor | ADMIN cadastra um corretor no painel; entre como esse corretor | Área de imóveis/proprietários, sem acesso de administrador |
| Cadastro de imóvel | Corretor cadastra proprietário fictício, imóvel e oferta | Imóvel começa sem publicação pública |
| S3 pela API | Corretor envia JPEG/PNG no painel ou Android | Linha em `foto` e objeto UUID na raiz do bucket; sem chave AWS no cliente |
| Foto interna | Abra a foto na gestão antes da publicação | Visível apenas para perfil autorizado |
| Publicação | Envie anúncio à revisão e publique como ADMIN | Catálogo e fotos públicas ficam disponíveis pela API |
| Bucket privado | Abra o Object URL do objeto em janela sem sessão AWS | Acesso direto não autorizado; foto pela API continua funcionando |
| Cadastro de cliente | Aceite termos, registre nome/e-mail fictícios e senha | Usuário criado com registro em `aceite_legal`; sem CPF/RG inicial |
| Verificação e recuperação | Consulte códigos no Mailpit pelo túnel | Código de seis dígitos aceito e login funcionando |
| Favoritos/visita | Cliente favorita e pede visita; corretor confirma | Persistência no RDS e transições de estado corretas |
| Mudança de foto | Altere foto de anúncio publicado | Anúncio volta à revisão; foto não deve continuar pública indevidamente |
| Cookies/mapa | Recuse opcionais, abra imóvel, depois autorize e revogue | Sem iframe antes da escolha; mapa removido após revogação |
| Privacidade | Em conta fictícia descartável, baixe dados e confirme exclusão | JSON próprio, conta removida do ambiente ativo e sessão encerrada |
| Persistência | Reinicie os containers, sem apagar RDS/S3 | Banco e fotos permanecem; códigos Mailpit podem ser perdidos se o container for recriado |

Para conferir um upload pelo MySQL: `SELECT id,imovel_id,arquivo FROM foto ORDER BY id DESC LIMIT 5;`. Compare `arquivo` com a chave mostrada na console S3. A rota pública é `/api/v1/fotos/ID_DA_FOTO`, não uma URL direta do S3. A remoção de arquivo é assíncrona: o job de limpeza executa a cada minuto e repete falhas; uploads órfãos têm janela de proteção de uma hora.

Se todos esses testes passarem, o backend foi efetivamente homologado no laboratório. O status de saúde sozinho não verifica as permissões de upload S3 nem os fluxos web/Android.

## 20. Reiniciar, atualizar e resolver problemas

### Após reabrir o Learner Lab

1. Inicie o laboratório, confirme o saldo e verifique EC2 **Running** e RDS **Available**. Não presuma que todos os recursos tenham o mesmo comportamento ao encerrar a sessão do laboratório.
2. Confira o IP público atual da EC2. Se mudou, atualize `IP_PUBLICO` no Nginx, reemita o certificado para o novo IP com a seção 17 e atualize `API_URL`, o túnel e o build Android.
3. Caso o certificado antigo não permita mais acessar o novo IP, renove/reemita corretamente; não ignore avisos HTTPS. Para trocar o IP mantendo o nome do certificado, use o comando de emissão com `--cert-name talisma-api`, o novo `--ip-address` e `--force-renewal`; leia os prompts do Certbot e confira o certificado emitido antes de recarregar Nginx.
4. Verifique SG SSH se o IP público do seu computador também mudou.
5. Na EC2: `cd /opt/talisma/projeto`, `export APP_IMAGE_TAG=lab-v1`, `docker compose -f infra/aws/compose.yaml up -d`. Confira saúde, HTTPS e S3 novamente.

### Atualização do código

Use uma pasta de release nova e um tag de imagem novo, por exemplo `lab-v2`. Preserve `/opt/talisma/runtime.env`, a chave JWT e o truststore. Aplique apenas migrations novas com a conta de DDL antes de publicar a nova revisão. O [guia operacional](aws-academy.md) descreve o pipeline manual e rollback; não faça downgrade automático do banco.

### Diagnóstico rápido

| Erro | Conferir |
|---|---|
| SSH timeout | IP atual, SG 22 com seu IP `/32`, IP público da EC2, subnet pública e rota 0.0.0.0/0 para IGW |
| Permission denied (publickey) | Usuário `ubuntu`, key pair usado na criação e arquivo `.pem` correto |
| RDS timeout | Mesmo VPC, RDS Available, origem do SG 3306 = SG da EC2, endpoint DNS correto |
| `Unknown database` | Schema `talisma` criado, não usar identificador da instância como nome do schema |
| `Access denied for user` | Conta/senha corretas e `REQUIRE SSL`; gerador não deve ser repetido para criar senha diferente de usuário existente |
| Erro PKIX/SSL JDBC | Truststore contém todas as CAs do bundle, é legível no container, URL usa hostname RDS e caminho `/run/secrets/...` |
| Migrations sem permissão | Carregar `migration.env` e usar perfil `aws,migrar`; não ampliar privilégios de `talisma_app` |
| `Unable to locate credentials` ou S3 403 | Instance profile correto, permissões do bucket, IMDSv2 habilitado e hop limit 2; não tornar bucket público |
| API funciona no host, mas foto falha | Teste S3 dentro do fluxo de upload; conferir nome do bucket/região e privilégios do papel, não apenas `sts` no host |
| 502 no Nginx | Container da API iniciou, health local, bind remoto via perfil aws, mapeamento 127.0.0.1:8080 |
| Certbot falha no desafio | IP atual, porta 80 pública, webroot e arquivo de prova acessíveis pelo Windows |
| HTTPS abre legal mas health público dá 404 | Esperado: health é somente local; teste `/api/v1/legal` externamente |
| Imagem não aparece no catálogo | Imóvel precisa estar PUBLICADO; alterações de foto voltam para revisão |
| API indisponível no Next.js | Reiniciar frontend após mudar API_URL; URL HTTPS acessível e sem erro de certificado |
| Código não chega em e-mail real | Comportamento esperado no laboratório: consulte Mailpit pelo túnel SSH |
| Docker sem espaço | `df -h` e `docker system df`; examine imagens antigas, não apague backups ou recursos em uso indiscriminadamente |

## 21. Checklist final e informações para guardar

- [ ] Região `us-east-1`, saldo do Learner Lab conferido.
- [ ] EC2 `talisma-api-ec2`, Ubuntu 24.04 x86_64, instance profile correto e hop limit 2.
- [ ] RDS `talisma-mysql-lab`, MySQL compatível, privado e acessível apenas pela EC2.
- [ ] Bucket com bloqueio público, SSE-S3, sem CORS público e objetos com chaves esperadas.
- [ ] Dados e fotos locais migrados com contagens e hashes conferidos.
- [ ] Contas MySQL separadas, credenciais em `/opt/talisma` com permissão 600.
- [ ] Flyway V1–V6 validado; `runtime.env` não contém credenciais AWS nem senha master.
- [ ] Administrador existente ou primeiro administrador provisionado; container temporário encerrado.
- [ ] Health local UP, API HTTPS válida e renovação automática verificada.
- [ ] Site e APK usam o IP HTTPS atual; Mailpit acessível somente pelo túnel.
- [ ] Upload/leitura/exclusão de foto, revisão/publicação, visita e privacidade testados.
- [ ] Backup/retomada documentados; exclusões conciliadas antes de qualquer restauração.

Guarde em local privado: ID da conta do laboratório, IDs VPC/SG/instância, IP da EC2, endpoint RDS, nome do bucket, localização da chave SSH, credenciais master e de administrador da aplicação, revisão/tag implantados e data de validade do certificado. Não publique essa ficha no repositório com senhas.

Ao encerrar a atividade, siga a orientação do laboratório para parar/remover os recursos e conferir o saldo. A retenção de sete dias precisa ser aplicada de fato aos backups existentes; parar a EC2 não apaga snapshots, objetos ou dados do RDS. Veja no guia operacional como preservar/reaplicar exclusões de titulares durante uma restauração.

## Fontes oficiais

Consultadas em 18/11/2026. Campos da console e permissões do Learner Lab podem variar; use os valores funcionais acima e confira o que a sua conta permite.

- [AWS: criação e conexão EC2/RDS MySQL privado](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_GettingStarted.CreatingConnecting.MySQL.html).
- [AWS: RDS for MySQL e versões suportadas](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_MySQL.html).
- [AWS: criação de buckets S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/GetStartedWithS3.html).
- [AWS: credenciais do instance profile no SDK Java](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/ec2-iam-roles.html).
- [AWS: IMDSv2 e configuração de metadados para containers](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-IMDS-new-instances.html).
- [AWS: certificados e TLS do RDS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.SSL.html).
- [AWS: instalação oficial da CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html).
- [Docker: instalação no Ubuntu](https://docs.docker.com/engine/install/ubuntu/).
- [Let's Encrypt: Certbot e certificado para IP público](https://letsencrypt.org/2026/03/11/shorter-certs-certbot).
