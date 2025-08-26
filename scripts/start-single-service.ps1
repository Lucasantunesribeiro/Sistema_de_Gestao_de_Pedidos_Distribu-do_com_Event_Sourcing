param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("order-service", "payment-service", "inventory-service", "order-query-service")]
    [string]$ServiceName,
    
    [int]$Port = 8084
)

Write-Host "=== Iniciando $ServiceName na porta $Port ===" -ForegroundColor Cyan

# Parar processos Java existentes
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Write-Host "Parando processos Java existentes..." -ForegroundColor Yellow
    $javaProcesses | ForEach-Object { $_.Kill() }
    Start-Sleep -Seconds 2
}

# Configurar variáveis de ambiente
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:SERVER_PORT = $Port.ToString()

# Configurações específicas para H2
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
$env:SPRING_DATASOURCE_USERNAME = 'sa'
$env:SPRING_DATASOURCE_PASSWORD = ''
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'org.h2.Driver'
$env:SPRING_JPA_DATABASE_PLATFORM = 'org.hibernate.dialect.H2Dialect'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'create-drop'

# Desabilitar cache do Hibernate
$env:SPRING_JPA_PROPERTIES_HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE = 'false'
$env:SPRING_JPA_PROPERTIES_HIBERNATE_CACHE_USE_QUERY_CACHE = 'false'

# Desabilitar Redis e RabbitMQ para teste local
$env:SPRING_REDIS_HOST = 'localhost'
$env:SPRING_REDIS_PORT = '6379'
$env:SPRING_RABBITMQ_HOST = 'localhost'
$env:SPRING_RABBITMQ_PORT = '5672'

$jarPath = "services/$ServiceName/target/$ServiceName-1.0.0.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "JAR não encontrado: $jarPath" -ForegroundColor Red
    Write-Host "Execute: mvn clean package -DskipTests" -ForegroundColor Yellow
    exit 1
}

Write-Host "Iniciando $ServiceName..." -ForegroundColor Green
Write-Host "JAR: $jarPath" -ForegroundColor Gray
Write-Host "Porta: $Port" -ForegroundColor Gray
Write-Host "Perfil: local" -ForegroundColor Gray
Write-Host ""

# Iniciar o serviço
java -jar $jarPath