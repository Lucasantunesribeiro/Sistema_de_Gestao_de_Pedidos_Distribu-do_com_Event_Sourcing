# Descrição de Portfólio

Análise técnica forense baseada no repositório vigente em 11/03/2026.

Projeto avaliado: **Sistema de Gestão de Pedidos Distribuído com Event Sourcing**

Leitura executiva: este repositório demonstra **boa ambição arquitetural** e **maturidade acima da média de projeto acadêmico**, mas o runtime principal hoje é um **monólito modular em Java/Spring Boot**, não um sistema .NET nem um conjunto de microserviços realmente ativo em produção. Para portfólio, ele é forte como prova de **raciocínio de arquitetura enterprise**, mas precisa ser posicionado com honestidade para vagas `.NET`.

## 1. Visão Geral do Projeto

O sistema resolve o ciclo de vida de pedidos de uma operação de e-commerce ou backoffice comercial: criação de pedido, reserva de estoque, processamento de pagamento, cancelamento com compensação, consulta operacional e painel de acompanhamento.

O domínio coberto é consistente com cenários enterprise:

- pedidos
- inventário
- pagamentos
- dashboard operacional
- saúde da aplicação
- auditoria de eventos

Na prática, o repositório contém duas histórias arquiteturais:

1. Uma arquitetura **vigente** em `unified-order-system`, que consolida o domínio em um **monólito modular**.
2. Uma arquitetura **legada** em `services/`, com microserviços separados para pedidos, pagamentos, inventário e query model.

Para fins de portfólio, o correto é apresentar o projeto como:

**"Sistema de gestão de pedidos com monólito modular orientado a domínio, com artefatos legados de evolução para microserviços e mensageria."**

Não é correto apresentá-lo hoje como:

- plataforma .NET
- frontend React
- microserviços ativos ponta a ponta

## 2. Arquitetura do Sistema

### Arquitetura real do código

O módulo ativo é `unified-order-system`, estruturado por domínios e preocupações transversais:

- `order/`
- `inventory/`
- `payment/`
- `infrastructure/events/`
- `config/`
- `web/`
- `shared/`

Essa organização é boa e mostra intenção clara de separar:

- regras de negócio
- casos de uso
- persistência
- web/API
- eventos
- cross-cutting concerns

### Clean Architecture

O projeto é **inspirado em Clean Architecture**, mas não a implementa de forma rígida.

O que existe de positivo:

- `order/application/` concentra casos de uso como `CreateOrderUseCase` e `CancelOrderUseCase`
- `order/domain/` centraliza regras como `OrderBusinessRules`
- repositórios JPA ficam em pacotes dedicados
- há separação explícita entre DTOs, entidades e controllers

Onde a implementação perde pureza arquitetural:

- entidades de domínio são também entidades JPA
- controllers como `InventoryController` acessam repositórios e fazem bastante orquestração
- `DashboardController` consulta repositórios diretamente
- parte da lógica ainda está em serviços utilitários e controllers, não só em casos de uso

Conclusão arquitetural:

**O projeto é "Clean-ish", não Clean Architecture estrita.**

### DDD

O projeto demonstra **DDD parcial e pragmático**.

Sinais positivos:

- bounded contexts claros: pedido, pagamento, inventário
- regras de negócio encapsuladas em classes de domínio
- status de pedido, pagamento e reserva modelados por enums/eventos
- entidades com comportamento de negócio, não só getters/setters

Limitações:

- o domínio ainda depende fortemente da estrutura JPA
- faltam aggregates mais rigorosos e contratos mais explícitos entre contextos
- há trechos placeholder ou simplificados demais para um domínio enterprise real

### Modular monolith vs microservices

Hoje o sistema é, de fato, um **modular monolith**.

Isso não é um problema. Pelo contrário: para portfólio, é uma decisão defensável porque reduz complexidade operacional e preserva coesão do domínio.

O ponto importante é ser tecnicamente preciso:

- `services/` contém microserviços legados com RabbitMQ, CQRS e retries
- o `pom.xml` raiz e a CI atual **não constroem esses serviços**
- o `docker-compose.yml` atual sobe principalmente o `unified-order-system`

Ou seja:

**a narrativa vigente é monólito modular com herança arquitetural de microserviços.**

### Event Driven

No módulo ativo, o sistema persiste eventos de domínio em `domain_events` através de `EventPublisher`.

Isso mostra:

- preocupação com trilha de auditoria
- rastreabilidade por `correlationId`
- base para replay ou integração futura

Mas há um limite importante:

- o monólito ativo **não publica eventos de negócio no RabbitMQ**
- ele **não consome eventos assíncronos** via listeners
- logo, o runtime principal não é event-driven distribuído de ponta a ponta

Na prática, hoje o projeto está mais próximo de:

**"CRUD transacional com event log/audit trail"**

do que de:

**"Event-driven architecture distribuída plenamente operacional"**

## 3. Stack Tecnológica

### Backend

- **Java 17**: linguagem principal do projeto atual.
- **Spring Boot 3.1.5**: base da aplicação, com forte aderência a APIs REST, segurança, actuator e observabilidade.
- **Spring Web**: implementação dos endpoints HTTP.
- **Spring Data JPA / Hibernate**: persistência relacional.
- **Spring Validation**: validação de DTOs.
- **Spring Security**: autenticação/autorização com JWT e filtros customizados.
- **Spring Actuator**: health checks e métricas operacionais.
- **Thymeleaf**: renderização server-side da interface web.

### Frontend

- **Thymeleaf + HTML/CSS/JavaScript vanilla**: interface atual do dashboard e das páginas operacionais.
- **Playwright**: testes e2e de navegação e fluxo visual.

Observação importante para portfólio:

**não há React nem Angular no frontend ativo.**

### Banco de dados

- **PostgreSQL**: banco alvo para execução real.
- **H2**: banco usado em desenvolvimento/testes.
- **Flyway**: versionamento de schema por migrations.

### Infraestrutura

- **Docker**: empacotamento da aplicação.
- **Docker Compose**: ambiente local e ambiente de produção simplificado.
- **AWS EC2**: deploy atual descrito no repositório.
- **AWS ECR**: referência de imagem container no fluxo de deploy.

### DevOps

- **GitHub Actions**: CI, validação de PR e deploy por SSH.
- **Dependabot**: atualização automatizada de dependências.

### Mensageria

- **RabbitMQ**: presente no ambiente e nos artefatos legados de microserviços.
- **DLQ / retries**: implementados em libs e serviços legados.

Nuance essencial:

**no módulo ativo `unified-order-system`, RabbitMQ não é parte do fluxo principal em execução.**

### Observabilidade

- **Prometheus**: coleta de métricas
- **Grafana**: dashboards
- **Loki**: logs
- **Tempo**: tracing stack provisionado
- **Alertmanager**: alertas
- **Micrometer**: métricas de aplicação
- **logstash-logback-encoder**: logs estruturados
- **correlationId / traceId / spanId em MDC**: rastreabilidade de requisições

Por que essas tecnologias foram usadas:

- para aproximar o projeto de um cenário enterprise
- para mostrar preocupações além de CRUD básico
- para evidenciar maturidade em operação, monitoramento e troubleshooting

## 4. Fluxo do Sistema

Fluxo principal de criação de pedido no módulo ativo:

1. O cliente envia `POST /api/orders`.
2. `OrderController` delega para `CreateOrderUseCase`.
3. O caso de uso gera ou propaga `correlationId`.
4. Regras de negócio são validadas por `OrderBusinessRules`.
5. O pedido é persistido inicialmente com status `PENDING`.
6. O sistema tenta reservar estoque.
7. Um evento `InventoryReservedEvent` é gravado em `domain_events`.
8. O pagamento é processado de forma síncrona por `PaymentService`.
9. O pedido é atualizado para `CONFIRMED`.
10. Um `OrderCreatedEvent` é persistido no event store.
11. A API devolve o `OrderResponse`.

Fluxo de cancelamento:

1. O cliente chama cancelamento do pedido.
2. `CancelOrderUseCase` valida se o cancelamento é permitido.
3. O sistema tenta liberar a reserva de estoque.
4. O sistema tenta executar estorno do pagamento.
5. O pedido é marcado como `CANCELLED`.
6. Um `OrderCancelledEvent` é persistido.

Ponto crítico de honestidade técnica:

- o estoque do fluxo principal ainda usa implementação simplificada em `InventoryService`
- o reembolso em `CancelOrderUseCase` ainda é parcial/placeholder
- o evento é persistido no banco, não propagado assíncronamente no runtime principal

## 5. Conceitos de Engenharia Aplicados

### SOLID

**Parcialmente demonstrado.**

Há boa intenção de separação por responsabilidade, principalmente em:

- use cases
- business rules
- repositórios
- filtros/configuração

Mas ainda existem violações práticas:

- controllers muito carregados
- mistura de orchestration e acesso a banco em camadas web
- serviços com comportamento mockado convivendo com repositórios reais

### DDD

**Parcial.**

Existe modelagem por domínio e linguagem ubíqua razoável, mas ainda sem o rigor de um projeto DDD mais maduro.

### Clean Architecture

**Parcial.**

A separação por pacotes ajuda, porém a regra de dependência não é rígida o suficiente para caracterizar Clean Architecture clássica.

### CQRS

**Parcial e mais presente na arquitetura legada do que no runtime atual.**

O `order-query-service` legado indica leitura segregada, mas o monólito ativo lê e escreve no mesmo modelo relacional.

### Event Driven

**Parcial.**

No monólito ativo há persistência de eventos; nos serviços legados há RabbitMQ, listeners, exchanges e DLQ. No fluxo atual em produção local, isso não está totalmente conectado.

### Outbox Pattern

**Não implementado no módulo ativo.**

Há indícios e migrations nos serviços legados, além de `TODO` explícito no código do pedido, mas o runtime vigente não usa outbox.

### Idempotência

**Parcial e informal.**

Exemplo:

- `PaymentService` evita duplicidade simples ao verificar pagamento já concluído por `orderId`

Limites:

- não há chave de idempotência HTTP
- não há deduplicação robusta de mensagens/eventos

### Retry / DLQ

**Presente em bibliotecas e serviços legados, ausente no fluxo principal ativo.**

Ou seja, é um conceito demonstrado no repositório, mas não na entrega principal em execução.

## 6. Relevância Para o Mercado Brasileiro

### O projeto demonstra skills demandadas?

Sim, em boa parte:

- APIs REST
- PostgreSQL
- Docker
- CI/CD
- AWS
- segurança
- observabilidade
- testes automatizados
- arquitetura modular
- conceitos de mensageria e eventos

### Ele parece um projeto enterprise?

**Sim em intenção e escopo. Parcialmente em execução.**

Ele tem características que lembram sistemas enterprise:

- domínio de negócio crível
- múltiplos contextos
- event store
- observabilidade
- pipeline de deploy
- preocupações com segurança e rastreabilidade

Mas ainda não é um projeto enterprise maduro porque:

- o runtime principal ainda tem partes mockadas/simplificadas
- há drift entre documentação, scripts e arquitetura vigente
- a mensageria real está mais no legado do que no caminho principal

### Ele é relevante para vagas Junior?

Sim, principalmente para mostrar:

- pensamento arquitetural acima da média
- familiaridade com back-end enterprise
- noção de distribuição, observabilidade e segurança

Mas há uma limitação importante para o foco do Lucas:

**para vagas Fullstack .NET + React ou Backend .NET, a aderência de stack é baixa, porque o projeto atual é Java/Spring + Thymeleaf.**

## 7. Como Explicar o Projeto em Entrevista

### Explicação simples (30 segundos)

Desenvolvi um sistema de gestão de pedidos com pedidos, pagamentos e estoque, estruturado como monólito modular e com conceitos de arquitetura enterprise, como eventos de domínio, observabilidade, testes automatizados, Docker e deploy em AWS. O projeto foi pensado para simular cenários reais de mercado, com foco em organização de domínio e qualidade de engenharia.

### Explicação técnica (2 minutos)

O projeto atual roda como um monólito modular em Spring Boot, separado por contextos como `order`, `payment` e `inventory`. No fluxo principal, a criação do pedido passa por um caso de uso dedicado, valida regras de negócio, persiste o pedido, tenta reservar estoque, processa pagamento e grava eventos de domínio em uma tabela `domain_events`, com `correlationId` para rastreabilidade. Também há segurança com JWT, rate limiting e headers, métricas com Micrometer, logs estruturados e stack de observabilidade com Prometheus, Grafana, Loki e Tempo provisionados no repositório.

Além disso, o repositório ainda mantém uma estrutura legada de microserviços com RabbitMQ, CQRS read model, DLQ e retry, o que mostra a evolução arquitetural do sistema. O ponto que eu apresentaria com transparência é que o módulo principal ainda precisa de hardening para ficar realmente enterprise-like: remover mocks no inventário, ativar outbox/mensageria no runtime principal, fechar a superfície pública da API e alinhar melhor a stack ao meu foco de carreira em `.NET`.

## 8. Pontos Fortes do Projeto

- Estrutura de domínio bem acima de um CRUD simples.
- Monólito modular com separação razoável por contexto.
- Presença de casos de uso explícitos para o fluxo central de pedidos.
- Event store com `domain_events` e correlação entre requisições.
- Segurança com JWT, rate limiting, CSP e headers.
- Stack de observabilidade relativamente completa no repositório.
- Dockerfile multi-stage e `docker-compose` para ambiente local/prod.
- CI/CD com GitHub Actions e deploy em AWS/EC2.
- Testes automatizados amplos: **238 testes executados localmente com sucesso**.
- Cobertura automática com JaCoCo e regra mínima de qualidade.
- Testes de controller, repository, integração, concorrência, resiliência e e2e.

## 9. Pontos a Melhorar

- O projeto não é `.NET`; isso reduz valor direto para o posicionamento do candidato em vagas C#.
- O frontend não é React; é Thymeleaf + JS vanilla.
- O runtime principal não usa RabbitMQ de verdade, apesar de a infra existir.
- O uso de eventos no monólito é mais audit trail do que event-driven distribuído completo.
- O inventário do fluxo principal ainda tem comportamento simplificado/mockado em `InventoryService`.
- O reembolso de pagamento no cancelamento ainda é incompleto.
- O outbox pattern não está implementado no módulo ativo.
- Há drift entre arquitetura atual, README, scripts e workflows.
- `docker-compose` principal roda com `ddl-auto=update` e Flyway desligado, o que é fraco para governança de banco.
- CORS está permissivo em `WebConfig` e muitos endpoints ficam públicos por configuração.
- `TestController` está em código principal e exposto em `/api/test`.
- Existe stack de tracing com Tempo, mas a aplicação ativa não está realmente instrumentada com tracing distribuído.
- Há cliente WebSocket no frontend, mas não há configuração clara do broker/endpoints no backend ativo.
- Dependabot e scripts ainda assumem um `frontend/` React que não existe no repositório atual.
- README cita k6, mas o diretório `tests/` não traz scripts k6 reais.

## 10. Melhorias Prioritárias Para Portfólio

1. **Portar o módulo principal para .NET 8 + ASP.NET Core + EF Core**
   Isso aumenta drasticamente a aderência ao mercado alvo do Lucas sem perder o domínio e a arquitetura já pensados.

2. **Adicionar um frontend React consumindo a API**
   Isso transforma o projeto em ativo real para vagas Fullstack `.NET + React`, hoje o maior gap de stack do portfólio.

3. **Substituir os mocks do inventário por fluxo real com banco + mensageria**
   Isso elimina o principal ponto que enfraquece a credibilidade técnica do sistema atual.

4. **Implementar Outbox + publicação real no RabbitMQ no runtime ativo**
   Isso fecha a história de Event Driven Architecture de forma defensável em entrevista.

5. **Endurecer segurança**
   Fechar endpoints públicos desnecessários, remover `TestController`, limitar CORS e obrigar autenticação em produção de forma consistente ajuda muito em percepção de senioridade.

6. **Fazer a CI validar PostgreSQL/Flyway/Testcontainers em vez de depender só de H2**
   Isso aproxima o pipeline do ambiente real e reduz a sensação de “teste que passa, mas não prova produção”.

7. **Instrumentar tracing real até Tempo**
   Hoje há observabilidade boa em logs e métricas, mas tracing distribuído ainda está mais no provisioning do que na aplicação.

8. **Limpar o legado morto ou reativá-lo de forma objetiva**
   Scripts, workflows e referências a `frontend/` e microserviços não ativos geram ruído e reduzem confiança no repositório.

## 11. Como Colocar no Currículo

Descrição curta recomendada:

**Sistema de gestão de pedidos com arquitetura modular orientada a domínio, desenvolvido em Java/Spring Boot, com APIs REST, PostgreSQL, event store, testes automatizados, Docker, CI/CD e deploy em AWS. Projeto focado em conceitos enterprise como separação de contextos, observabilidade, segurança e evolução para mensageria/event-driven.**

Observação estratégica:

Se usar este projeto no currículo para vagas `.NET`, o ideal é enquadrá-lo como **prova de arquitetura e engenharia de software**, não como prova de stack principal.

## 12. Nível do Projeto

**Classificação: Junior+**

Motivo:

- Está acima de projeto júnior comum em escopo e preocupação arquitetural.
- Demonstra domínio de temas que aparecem em times enterprise.
- Tem pipeline, observabilidade, segurança, testes e deploy.

Por que ainda não classifico como Pleno ou Enterprise-like:

- há inconsistências entre narrativa e execução real
- partes do fluxo principal ainda são simplificadas
- stack não está alinhada ao foco profissional do candidato
- mensageria, outbox e tracing não estão fechados no runtime principal

## 13. Checklist de Mercado

| Requisito Mercado | Presente no Projeto | Observação |
|---|---|---|
| C# | Não | Stack principal atual é Java |
| .NET | Não | Gap direto para vagas alvo |
| Java/Spring Boot | Sim | Muito presente no runtime ativo |
| React | Não | Há scripts obsoletos referenciando frontend React, mas ele não existe |
| Angular | Não | Não identificado |
| APIs REST | Sim | Controllers para pedidos, pagamentos, inventário, dashboard e health |
| PostgreSQL | Sim | Banco alvo real |
| H2 | Sim | Usado em dev/testes |
| Flyway | Parcial | Existe, mas o compose principal roda com Flyway desligado |
| Docker | Sim | Dockerfile multi-stage e compose |
| CI/CD | Sim | GitHub Actions para build/test/deploy |
| AWS | Sim | EC2 + ECR no fluxo descrito |
| RabbitMQ | Parcial | Forte no legado, não no fluxo principal ativo |
| Redis | Parcial | Configuração existe, uso real é fraco e starter está comentado |
| DDD | Parcial | Boa intenção, execução pragmática |
| Clean Architecture | Parcial | Estrutura boa, dependências ainda misturadas |
| CQRS | Parcial | Mais claro na estrutura legada |
| Event Driven | Parcial | Event log ativo, broker ativo só no legado |
| Outbox Pattern | Não | Indícios e TODOs, mas não implementado no módulo principal |
| Idempotência | Parcial | Há proteção simples em pagamento, não política completa |
| Retry / DLQ | Parcial | Presente em libs e serviços legados |
| Observabilidade | Sim | Prometheus, Grafana, Loki, Alertmanager e métricas |
| Tracing distribuído | Parcial | Stack existe, app ativo não está totalmente instrumentado |
| Testes automatizados | Sim | 238 testes passaram localmente |
| Playwright | Sim | Existe suíte em `tests/e2e` |
| Testcontainers | Parcial | Dependência existe, mas a execução principal validada aqui usou H2 |
| Kubernetes | Não | Não identificado |
| Terraform | Não | Não identificado |
| NoSQL | Não | Não identificado |

## 14. Score Final do Projeto

**Nota final: 6,7 / 10**

Como cheguei nessa nota:

- **Arquitetura:** boa ambição, bom domínio e organização por contextos.
- **Engenharia:** sólida em testes, segurança e observabilidade, mas com drift relevante e partes mockadas.
- **Relevância para vagas:** boa para discutir arquitetura enterprise; moderada para vagas `.NET` por causa do desalinhamento de stack.

Leitura justa da nota:

- Como projeto geral de engenharia: a base é boa.
- Como projeto de portfólio para backend enterprise: é competitivo.
- Como projeto principal para vagas **Fullstack .NET + React**: ainda não é suficiente sozinho.

Se o núcleo deste projeto for migrado ou replicado em **.NET 8 + ASP.NET Core + EF Core + React**, a percepção de mercado sobe de forma significativa e ele pode se tornar um ativo realmente forte para processos seletivos no Brasil.
