# Script para baixar e usar Maven temporariamente
Write-Host "Baixando Maven..." -ForegroundColor Green

# Criar diretório temporário
$mavenDir = "C:\temp-maven"
if (!(Test-Path $mavenDir)) {
    New-Item -ItemType Directory -Path $mavenDir -Force
}

# URL do Maven
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
$mavenZip = "$mavenDir\maven.zip"

# Baixar Maven
try {
    Write-Host "Baixando Maven do Apache..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
    Write-Host "Download concluído!" -ForegroundColor Green
} catch {
    Write-Host "Erro ao baixar Maven: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Extrair Maven
try {
    Write-Host "Extraindo Maven..." -ForegroundColor Yellow
    Expand-Archive -Path $mavenZip -DestinationPath $mavenDir -Force
    Write-Host "Extração concluída!" -ForegroundColor Green
} catch {
    Write-Host "Erro ao extrair Maven: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Configurar PATH temporário
$mavenBin = "$mavenDir\apache-maven-3.9.6\bin"
$env:PATH = "$mavenBin;$env:PATH"

Write-Host "Maven configurado temporariamente!" -ForegroundColor Green
Write-Host "Você pode agora usar o comando 'mvn' neste terminal." -ForegroundColor Cyan
Write-Host "Para usar em outros terminais, instale o Maven permanentemente." -ForegroundColor Yellow

# Testar Maven
try {
    $mavenVersion = & "$mavenBin\mvn.cmd" -version 2>&1
    Write-Host "Versão do Maven:" -ForegroundColor Green
    Write-Host $mavenVersion -ForegroundColor White
} catch {
    Write-Host "Erro ao testar Maven: $($_.Exception.Message)" -ForegroundColor Red
} 