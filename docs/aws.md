# AWS Academy

O Learner Lab hospeda a API e a Lia em uma EC2 e mantém o banco e as fotos em serviços privados. O frontend e o Android usam a mesma origem HTTPS com certificado público.

## Serviços e estrutura

| Serviço | Função |
|---|---|
| EC2 Ubuntu | Nginx encaminha `/api/v1/` ao Spring Boot e `POST /chat` ao FastAPI. O Mailpit recebe e-mails de teste. |
| Aurora MySQL | Cluster privado compatível com MySQL 8.4. A aplicação usa o endpoint de escrita e TLS verificado. |
| Amazon S3 | Bucket privado com SSE-S3, bloqueio de acesso público e ACLs desativadas. |
| AWS Secrets Manager | Gerencia a senha administrativa do banco. A API usa a conta de execução com privilégios limitados. |
| IAM e Systems Manager | O perfil da EC2 fornece credenciais temporárias ao SDK. Systems Manager permite operar a instância quando disponível. |
| VPC e security groups | A EC2 recebe HTTPS. A porta 3306 do banco aceita conexão somente do grupo da EC2. |

O Compose de referência, o script de deploy e o exemplo de Nginx estão em [infra/aws](../infra/aws). A configuração Spring está em [application-aws.properties](../Dev%20BackEnd/imoveisapi/src/main/resources/application-aws.properties).

## Criação resumida

1. Inicie o Learner Lab em `us-east-1` e confira os recursos existentes antes de criar outros.
2. Prepare uma subnet pública para EC2 e duas privadas em zonas distintas para o grupo de subnets do Aurora. Limite SSH ao IP do operador e mantenha banco, Actuator e Mailpit fora da internet.
3. Prepare a EC2 Ubuntu com EBS criptografado, perfil autorizado pelo laboratório e IMDSv2 obrigatório com limite de saltos 2 para os contêineres.
4. Crie `talisma-aurora` com schema `talisma`, uma gravadora privada, armazenamento criptografado e retenção de sete dias. Neste laboratório, `db.t4g.medium` foi aceita e `db.serverless` foi bloqueada. A classe provisionada continua consumindo crédito enquanto estiver ativa.
5. Crie ou reaproveite o bucket S3 privado. A API usa `imoveis/<id>/<uuid>.<extensão>` nos novos uploads. As fotos migradas conservam as chaves anteriores.
6. Prepare as contas de migrations e execução. Publique a API e a Lia e valide o catálogo, as fotos e o chat por HTTPS antes de abrir o frontend e o Android.

No laboratório já preparado, o bucket é `talisma-lab-fotos-7cf6291d` e a EC2 tem a tag `talisma-api-ec2`. Após reiniciar o laboratório, confirme o DNS, o endereço atual da EC2 e a validade do certificado.

## Atualizações, backups e restauração

- O workflow [validar.yml](../.github/workflows/validar.yml) verifica Java, frontend e Android. A [publicação manual](../.github/workflows/deploy-laboratorio.yml) exige runner Windows com rótulo `talisma-lab`, SSH com chave de host verificada e variáveis `LAB_HOST` e `LAB_USER`. Use o runner somente neste repositório e em revisões confiáveis.
- Aplique migrations separadamente antes de publicar uma versão que altera o esquema. Preserve a imagem anterior e mantenha a mesma chave JWT. Se a imagem anterior suportar o esquema atual, faça o rollback com `APP_IMAGE_TAG=TAG_ANTERIOR docker compose -f infra/aws/compose.yaml up -d --no-build` e confira `/actuator/health` na EC2.
- Guarde snapshots do Aurora e cópias correspondentes das fotos. Se usar Lifecycle para backups no S3, filtre só `backups/`. Antes de restaurar, preserve o registro administrativo em `GET /api/v1/admin/privacidade/exclusoes`. Restaure em uma base nova, reconcilie exclusões com [reaplicar-exclusoes.sql](../scripts/reaplicar-exclusoes.sql) e revogue as sessões restauradas antes de reabrir o tráfego.
- O bucket e o banco podem consumir saldo mesmo quando a EC2 está parada. Confira o saldo e o estado dos recursos quando terminar a atividade.

[Execução detalhada](rodar-aplicacao.md) · [Criação de um cluster Aurora](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.CreateInstance.html)
