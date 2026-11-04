#Requires -Version 7.0
[CmdletBinding()]
param(
    [ValidateNotNullOrEmpty()]
    [string]$UsuarioBanco = 'talisma_app',

    [ValidateNotNullOrEmpty()]
    [string]$UrlBanco = 'jdbc:mysql://127.0.0.1:3306/talisma?connectionTimeZone=UTC'
)

$ErrorActionPreference = 'Stop'

function ConverterParaValorProperties([string]$Valor) {
    return -join ($Valor.ToCharArray() | ForEach-Object { '\u{0:X4}' -f [int]$_ })
}

$projeto = Split-Path -Parent $PSScriptRoot
$perfil = [Environment]::GetFolderPath('UserProfile')
$diretorioPrivado = Join-Path $perfil '.talisma'
$credenciais = Join-Path $diretorioPrivado 'imoveisapi.properties'
$diretorioConfig = Join-Path $projeto 'config'
$importacao = Join-Path $diretorioConfig 'application.properties'
$importacaoEsperada = 'spring.config.import=' + ([Uri]$credenciais).AbsoluteUri

if ((Test-Path -LiteralPath $importacao) -and
    ((Get-Content -LiteralPath $importacao -Raw).Trim() -ne $importacaoEsperada)) {
    throw "Já existe uma configuração diferente em $importacao. Preserve ou mova o arquivo antes de continuar."
}

if (-not (Test-Path -LiteralPath $credenciais)) {
    $entrada = Read-Host "Senha MySQL de $UsuarioBanco" -AsSecureString
    $senha = ConvertFrom-SecureString -SecureString $entrada -AsPlainText
    if ([string]::IsNullOrEmpty($senha)) {
        throw 'Informe a senha da conta local do MySQL.'
    }
    $chave = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))

    New-Item -ItemType Directory -Path $diretorioPrivado -Force | Out-Null
    New-Item -ItemType File -Path $credenciais | Out-Null
    try {
        $usuario = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
        & icacls.exe $credenciais /inheritance:r /grant:r "*${usuario}:(F)" '*S-1-5-18:(F)' '*S-1-5-32-544:(F)' | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw 'Não foi possível proteger o arquivo de credenciais.'
        }
        @(
            'spring.datasource.url=' + (ConverterParaValorProperties $UrlBanco)
            'spring.datasource.username=' + (ConverterParaValorProperties $UsuarioBanco)
            'spring.datasource.password=' + (ConverterParaValorProperties $senha)
            'APP_JWT_KEY=' + $chave
        ) | Set-Content -LiteralPath $credenciais -Encoding ascii
    } catch {
        Remove-Item -LiteralPath $credenciais -ErrorAction SilentlyContinue
        throw
    }
} else {
    Write-Output 'Credenciais e chave JWT já configuradas. Arquivo existente preservado.'
}

if (-not (Test-Path -LiteralPath $importacao)) {
    New-Item -ItemType Directory -Path $diretorioConfig -Force | Out-Null
    Set-Content -LiteralPath $importacao -Value $importacaoEsperada -Encoding ascii
}
Write-Output 'Configuração pronta. Execute a API a partir da pasta imoveisapi.'
