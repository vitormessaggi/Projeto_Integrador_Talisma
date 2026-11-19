# Projeto_Integrador Talismã Imóveis
Este repositório é utilizado para o projeto integrador do curso de Análise e Desenvolvimento de sistemas do senai Armando de Arruda Pereira. O projeto, consiste em um sistema completo para a imobiliária Talismã. Contendo: Banco de dados, Site corporativo, APP para Alugueis e venda, Chat Bot e controles de segurança sujeitos a testes e validação contínuos.

## Aplicação Talismã Imóveis

- [Implantação local e roteiro de demonstração](Dev%20BackEnd/imoveisapi/docs/implantacao-local.md)
- [Contrato OpenAPI](Dev%20BackEnd/imoveisapi/docs/openapi.json)
- [Guia de migração da API mobile](Dev%20BackEnd/imoveisapi/docs/migracao-mobile.md)
- [Migrations e scripts MySQL](Dev%20BancoDados/README.md)

## Infraestrutura e privacidade

- [Análise técnica e regras de negócio](docs/analise-tecnica.md)
- [Criação e configuração completa: EC2, RDS e S3](docs/aws-passo-a-passo-completo.md)
- [Guia operacional AWS: pipeline, rollback e restauração](docs/aws-academy.md)
- [Resultados de validação e limitações](docs/validacao.md)
- [Manual Android atualizado](Dev%20Mobile/docs/manual-usuario.md)

O ambiente remoto é uma demonstração acadêmica com dados fictícios. O frontend roda localmente; a API usa o perfil `aws`, RDS privado e bucket S3 privado. O cadastro registra aceite versionado dos documentos, sem exigir CPF/RG. O site possui política de privacidade, escolhas de conteúdo opcional e área de acesso/exclusão dos dados.

Os documentos legais canônicos ficam em `Dev BackEnd/imoveisapi/src/main/resources/legal/`. Execute `python scripts/sincronizar-legais.py` ao alterá-los e `python scripts/sincronizar-legais.py --check` para conferir as cópias web/Android. Preserve cada versão publicada e incremente sua identificação em mudança relevante.

As variáveis necessárias estão documentadas nos guias; arquivos `.env.example` foram removidos. Segredos reais ficam fora do Git. O pipeline de deploy é manual e depende de configuração do laboratório e de runner temporário do aluno.
