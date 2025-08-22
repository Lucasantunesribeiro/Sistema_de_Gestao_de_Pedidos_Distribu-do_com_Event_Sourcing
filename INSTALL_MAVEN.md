# Como Instalar Maven no Windows

Se você quiser usar o script completo com Maven, siga estas instruções:

## Opção 1: Instalação Manual

### 1. Download do Maven
- Acesse: https://maven.apache.org/download.cgi
- Baixe o arquivo `apache-maven-3.9.x-bin.zip`

### 2. Extrair o Maven
- Extraia para `C:\Program Files\Apache\maven`
- Ou qualquer diretório de sua preferência

### 3. Configurar Variáveis de Ambiente

#### Via Interface Gráfica:
1. Pressione `Win + R`, digite `sysdm.cpl` e pressione Enter
2. Clique na aba "Avançado"
3. Clique em "Variáveis de Ambiente"
4. Em "Variáveis do Sistema", clique em "Novo"
5. Nome: `MAVEN_HOME`
6. Valor: `C:\Program Files\Apache\maven` (ou onde você extraiu)
7. Clique em "OK"
8. Encontre a variável `Path` e clique em "Editar"
9. Clique em "Novo" e adicione: `%MAVEN_HOME%\bin`
10. Clique em "OK" em todas as janelas

#### Via PowerShell (como Administrador):
```powershell
# Definir MAVEN_HOME
[Environment]::SetEnvironmentVariable("MAVEN_HOME", "C:\Program Files\Apache\maven", "Machine")

# Adicionar ao PATH
$path = [Environment]::GetEnvironmentVariable("PATH", "Machine")
[Environment]::SetEnvironmentVariable("PATH", "$path;%MAVEN_HOME%\bin", "Machine")
```

### 4. Verificar Instalação
Abra um novo PowerShell e execute:
```powershell
mvn -version
```

## Opção 2: Usando Chocolatey

Se você tem o Chocolatey instalado:
```powershell
# Instalar Chocolatey (se não tiver)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Instalar Maven
choco install maven
```

## Opção 3: Usando Scoop

Se você tem o Scoop instalado:
```powershell
# Instalar Scoop (se não tiver)
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# Instalar Maven
scoop install maven
```

## Opção 4: Usar Docker (Recomendado)

Se você não quiser instalar Maven, use nosso script Docker-only:
```powershell
.\scripts\setup-docker-only.ps1
```

Este script usa Docker para compilar tudo, não precisando do Maven instalado localmente.

## Verificação Final

Após a instalação, verifique se tudo está funcionando:
```powershell
# Verificar Java
java -version

# Verificar Maven
mvn -version

# Executar o setup completo
.\scripts\setup.ps1
```

## Solução de Problemas

### Erro: "mvn não é reconhecido"
- Reinicie o PowerShell/CMD após configurar as variáveis
- Verifique se o PATH está correto
- Verifique se MAVEN_HOME está definido corretamente

### Erro: "JAVA_HOME não está definido"
- Defina JAVA_HOME apontando para sua instalação do Java
- Exemplo: `C:\Program Files\Java\jdk-17`

### Permissões
- Execute o PowerShell como Administrador para configurar variáveis do sistema
- Ou use variáveis de usuário se não tiver privilégios de administrador

---

**Dica**: Se você só quer testar o sistema rapidamente, use o script `setup-docker-only.ps1` que não requer Maven instalado!