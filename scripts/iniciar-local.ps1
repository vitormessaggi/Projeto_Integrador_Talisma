#Requires -Version 7.0
[CmdletBinding()]
param([switch]$SomenteConfigurar)
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
$arquivoEnv = Join-Path $raizProjeto '.env'
function NovaSenha { [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24)) }
if (-not (Test-Path -LiteralPath $arquivoEnv)) {
    @('MYSQL_PORT=3307', ('MYSQL_ROOT_PASSWORD=' + (NovaSenha)), 'DB_USER=talisma_app',
      ('DB_PASSWORD=' + (NovaSenha)), 'TEST_DB_USER=talisma_test', ('TEST_DB_PASSWORD=' + (NovaSenha))) |
      Set-Content -LiteralPath $arquivoEnv -Encoding ascii
}
$configBanco = @{}
Get-Content -LiteralPath $arquivoEnv | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') { $configBanco[$Matches[1]] = $Matches[2] }
}
foreach ($campoObrigatorio in @('DB_USER','DB_PASSWORD','TEST_DB_USER','TEST_DB_PASSWORD','MYSQL_PORT')) {
    if (-not $configBanco[$campoObrigatorio]) { throw "Campo ausente em .env: $campoObrigatorio" }
}
$privado = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.talisma'
$credenciais = Join-Path $privado 'imoveisapi.properties'
New-Item -ItemType Directory -Path $privado -Force | Out-Null
$chaveJwt = $null
if (Test-Path -LiteralPath $credenciais) {
    $chaveJwt = Get-Content -LiteralPath $credenciais | Where-Object { $_ -like 'APP_JWT_KEY=*' } | Select-Object -First 1
}
if (-not $chaveJwt) { $chaveJwt = 'APP_JWT_KEY=' + [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)) }
function ValorProperties([string]$valor) { -join ($valor.ToCharArray() | ForEach-Object { '\u{0:X4}' -f [int]$_ }) }
@(('spring.datasource.url=' + (ValorProperties "jdbc:mysql://127.0.0.1:$($configBanco.MYSQL_PORT)/talisma?connectionTimeZone=UTC")),
  ('spring.datasource.username=' + (ValorProperties $configBanco.DB_USER)),
  ('spring.datasource.password=' + (ValorProperties $configBanco.DB_PASSWORD)), $chaveJwt) |
  Set-Content -LiteralPath $credenciais -Encoding ascii
$sidLocal = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
foreach ($arquivoProtegido in @($credenciais, $arquivoEnv)) {
    & icacls.exe $arquivoProtegido /inheritance:r /grant:r "*${sidLocal}:(F)" '*S-1-5-18:(F)' '*S-1-5-32-544:(F)' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao proteger credenciais locais' }
}
$pastaConfig = Join-Path $raizProjeto 'Dev BackEnd/imoveisapi/config'
New-Item -ItemType Directory -Path $pastaConfig -Force | Out-Null
('spring.config.import=' + ([Uri]$credenciais).AbsoluteUri) | Set-Content -LiteralPath (Join-Path $pastaConfig 'application.properties') -Encoding ascii
$env:TEST_DB_URL = "jdbc:mysql://127.0.0.1:$($configBanco.MYSQL_PORT)/talisma_test?connectionTimeZone=UTC"
$env:TEST_DB_USER = $configBanco.TEST_DB_USER
$env:TEST_DB_PASSWORD = $configBanco.TEST_DB_PASSWORD
if (-not $SomenteConfigurar) {
    & docker compose --project-directory $raizProjeto up -d --build --wait
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao iniciar infraestrutura Docker' }
}
Write-Output 'Configuração Docker pronta. A chave JWT existente foi preservada. Execute a API na pasta imoveisapi.'
