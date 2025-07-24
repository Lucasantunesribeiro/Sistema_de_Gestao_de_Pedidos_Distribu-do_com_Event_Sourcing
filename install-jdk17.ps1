# Script para baixar e instalar JDK 17
Write-Host "Baixando JDK 17..." -ForegroundColor Green

# Criar diretório temporário
$jdkDir = "C:\temp-jdk"
if (!(Test-Path $jdkDir)) {
    New-Item -ItemType Directory -Path $jdkDir -Force
}

# URL do OpenJDK 17 (Eclipse Temurin)
$jdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.10_7.zip"
$jdkZip = "$jdkDir\jdk17.zip"

# Baixar JDK
try {
    Write-Host "Baixando JDK 17 do Eclipse Temurin..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkZip -UseBasicParsing
    Write-Host "Download concluído!" -ForegroundColor Green
} catch {
    Write-Host "Erro ao baixar JDK: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Extrair JDK
try {
    Write-Host "Extraindo JDK..." -ForegroundColor Yellow
    Expand-Archive -Path $jdkZip -DestinationPath $jdkDir -Force
    Write-Host "Extração concluída!" -ForegroundColor Green
} catch {
    Write-Host "Erro ao extrair JDK: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Encontrar o diretório do JDK
$jdkHome = Get-ChildItem -Path $jdkDir -Directory | Where-Object { $_.Name -like "*jdk*" } | Select-Object -First 1
if (!$jdkHome) {
    Write-Host "Não foi possível encontrar o diretório do JDK" -ForegroundColor Red
    exit 1
}

$jdkPath = $jdkHome.FullName
$jdkBin = "$jdkPath\bin"

# Configurar variáveis de ambiente temporárias
$env:JAVA_HOME = $jdkPath
$env:PATH = "$jdkBin;$env:PATH"

Write-Host "JDK 17 configurado temporariamente!" -ForegroundColor Green
Write-Host "JAVA_HOME: $jdkPath" -ForegroundColor Cyan
Write-Host "Você pode agora usar o comando 'javac' neste terminal." -ForegroundColor Cyan

# Testar JDK
try {
    $javaVersion = & "$jdkBin\java.exe" -version 2>&1
    $javacVersion = & "$jdkBin\javac.exe" -version 2>&1
    
    Write-Host "Versão do Java:" -ForegroundColor Green
    Write-Host $javaVersion -ForegroundColor White
    
    Write-Host "Versão do Javac:" -ForegroundColor Green
    Write-Host $javacVersion -ForegroundColor White
} catch {
    Write-Host "Erro ao testar JDK: $($_.Exception.Message)" -ForegroundColor Red
} 