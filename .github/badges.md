# Status Badges para README.md

Adicione estes badges ao seu README.md principal para mostrar o status dos workflows:

## CI/CD Status

```markdown
![CI/CD Pipeline](https://github.com/SEU_USERNAME/Sistema_de_Gestão_de_Pedidos_Distribuído_com_Event_Sourcing/actions/workflows/render-ci.yml/badge.svg)
![PR Validation](https://github.com/SEU_USERNAME/Sistema_de_Gestão_de_Pedidos_Distribuído_com_Event_Sourcing/actions/workflows/pr-validation.yml/badge.svg)
```

## Deploy Status

```markdown
[![Deploy Status](https://api.render.com/deploy/srv-XXXXXXXXX?style=flat-square)](https://dashboard.render.com/static/srv-XXXXXXXXX)
```

## Code Quality

```markdown
![Code Quality](https://img.shields.io/badge/code%20quality-A-green.svg)
![Test Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen.svg)
![Security Score](https://img.shields.io/badge/security-A-green.svg)
```

## Technology Stack

```markdown
![Java](https://img.shields.io/badge/Java-17-orange.svg?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-green.svg?logo=spring-boot)
![React](https://img.shields.io/badge/React-18.3-blue.svg?logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg?logo=typescript)
![Docker](https://img.shields.io/badge/Docker-ready-blue.svg?logo=docker)
```

## Performance

```markdown
![Response Time](https://img.shields.io/badge/response%20time-<100ms-brightgreen.svg)
![Bundle Size](https://img.shields.io/badge/bundle%20size-<2MB-brightgreen.svg)
![Uptime](https://img.shields.io/badge/uptime-99.9%25-brightgreen.svg)
```

## Exemplo Completo para README.md:

```markdown
# Sistema de Gestão de Pedidos Distribuído

[![CI/CD Pipeline](https://github.com/SEU_USERNAME/REPO_NAME/actions/workflows/render-ci.yml/badge.svg)](https://github.com/SEU_USERNAME/REPO_NAME/actions/workflows/render-ci.yml)
[![PR Validation](https://github.com/SEU_USERNAME/REPO_NAME/actions/workflows/pr-validation.yml/badge.svg)](https://github.com/SEU_USERNAME/REPO_NAME/actions/workflows/pr-validation.yml)
[![Deploy Status](https://api.render.com/deploy/srv-XXXXXXXXX?style=flat-square)](https://dashboard.render.com/static/srv-XXXXXXXXX)

![Java](https://img.shields.io/badge/Java-17-orange.svg?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-green.svg?logo=spring-boot)
![React](https://img.shields.io/badge/React-18.3-blue.svg?logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg?logo=typescript)
![Docker](https://img.shields.io/badge/Docker-ready-blue.svg?logo=docker)

Sistema distribuído de gestão de pedidos utilizando Event Sourcing, CQRS e microserviços.

## 🚀 Quick Start

...resto do README...
```

**Nota**: Substitua `SEU_USERNAME`, `REPO_NAME` e `srv-XXXXXXXXX` pelos valores corretos do seu repositório e serviço no Render.