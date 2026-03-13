# Descricao Tecnica Para Portfolio

## 1. Visao Geral do Projeto

Este repositorio implementa um sistema de gestao de pedidos com foco em todo o ciclo do pedido: criacao, reserva de estoque, processamento de pagamento, cancelamento, auditoria de eventos e acompanhamento operacional.

O dominio modelado e tipico de cenarios enterprise de e-commerce, ERP comercial ou plataformas B2B: um pedido nasce, depende de disponibilidade de estoque, precisa ser liquidado por pagamento e exige rastreabilidade de ponta a ponta.

O problema que o projeto tenta resolver nao e apenas CRUD de pedido. Ele tenta demonstrar:

- separacao de dominios de `order`, `inventory` e `payment`
- orquestracao do fluxo do pedido com compensacao
- registro de eventos para auditoria
- observabilidade com metricas, logs e tracing
- operacao conteinerizada com deploy em AWS

Ponto importante para portfolio: o projeto **nao e .NET**. O backend principal e **Java 17 + Spring Boot 3**, com frontend **Angular 17** e tambem uma interface server-side em **Thymeleaf**. Para vagas `.NET`, isso reduz a aderencia de stack, embora a arquitetura e os conceitos sejam transferiveis.

## 2. Arquitetura do Sistema

### Leitura arquitetural real do repositorio

O repositorio hoje e um **hibrido entre monolito modular e experimentacao com microservices**.

Camadas macro do repositorio:

- `unified-order-system/`: runtime principal atual, um monolito modular
- `services/`: microservicos legados ou experimentais (`order-service`, `inventory-service`, `payment-service`, `order-query-service`)
- `libs/`: bibliotecas compartilhadas de seguranca, mensageria, observabilidade e eventos
- `shared-events/`: contratos de eventos compartilhados usados pelos servicos legados
- `frontend/`: SPA Angular 17
- `observability/`: Prometheus, Grafana, Loki, Tempo e Alertmanager
- `tests/`: Playwright e k6

### Monolito modular ativo

O modulo que realmente concentra o fluxo principal hoje e `unified-order-system`. Ele organiza o codigo por dominio:

- `order/`
- `inventory/`
- `payment/`
- `infrastructure/events/`
- `config/`
- `web/`
- `websocket/`

Isso e um sinal positivo de **modular monolith**, porque o projeto evita um pacote unico generico e separa responsabilidades por contexto.

### Clean Architecture e DDD: presentes, mas parciais

O repositorio tenta aplicar Clean Architecture e DDD, mas a aplicacao e **parcial**.

Onde isso aparece de forma concreta:

- `order/application/CreateOrderUseCase` e `CancelOrderUseCase` concentram o fluxo de negocio principal
- `order/domain/OrderBusinessRules`, `inventory/domain/InventoryBusinessRules` e `payment/domain/PaymentBusinessRules` representam regras de dominio
- `repository/` separa persistencia em interfaces Spring Data JPA
- `infrastructure/events/EventPublisher` isola a gravacao dos eventos

Onde a arquitetura perde pureza:

- as entidades de dominio do modulo ativo sao tambem entidades JPA, por exemplo `order/model/Order`
- ha duplicidade de fluxo entre `OrderService` e `CreateOrderUseCase` / `CancelOrderUseCase`
- `PaymentBusinessRules` existe, mas nao esta efetivamente integrada ao fluxo principal
- o pacote `query/` do modulo ativo praticamente nao implementa um read model de fato

Em outras palavras: a intencao arquitetural e boa, mas a execucao ainda esta mais proxima de um **Spring modularizado com boas separacoes** do que de uma Clean Architecture rigorosa.

### Modular monolith x microservices

Arquiteturalmente, o projeto conta duas historias ao mesmo tempo:

1. O **runtime principal** e um monolito modular.
2. O repositorio tambem preserva uma **linha legada de microservices** com RabbitMQ, CQRS read model, DLQ e outbox.

Isso pode ser vendido de forma honesta como "evolucao arquitetural", mas hoje gera ambiguidade. Para portfolio, a narrativa correta e:

- o produto executavel principal e o **monolito modular**
- os microservicos em `services/` mostram **exploracao de arquitetura distribuida**
- os microservicos **nao fazem parte do build principal nem da pipeline principal**

### Event Driven e Event Sourcing

No modulo ativo, o sistema grava eventos de dominio na tabela `domain_events` via `EventPublisher`. Isso gera:

- trilha de auditoria
- correlacao por `correlationId`
- historico de eventos por agregado

Mas isso **nao caracteriza um event-driven distribuido completo** no runtime atual. O que existe de fato:

- `CreateOrderUseCase` orquestra tudo de forma **sincrona e local**
- os eventos sao persistidos no banco
- nao ha RabbitMQ ativo no `unified-order-system`
- nao ha consumidor ou projecao real reconstruindo estado a partir desses eventos

Conclusao tecnica: no runtime principal, o projeto implementa **event log / audit trail com linguagem de Event Sourcing**, mas nao um Event Sourcing pleno com replay e projections como fonte primaria de verdade.

Nos servicos legados, por outro lado, ha evidencias mais fortes de arquitetura distribuida:

- listeners com `@RabbitListener`
- filas e DLQ
- tabelas de `event_store`, `outbox_events` e `processed_messages`
- servico de query separado

Portanto, **o conceito existe no repositorio**, mas esta mais maduro nos modulos legados do que no produto principal.

## 3. Stack Tecnologica

### Backend

- **Java 17**
  Motivo: versao moderna da JVM, boa aderencia a mercado enterprise e suporte estavel no ecossistema Spring.
- **Spring Boot 3**
  Motivo: aceleracao de APIs REST, configuracao, DI, actuator e integracao com o ecossistema corporativo.
- **Spring Data JPA / Hibernate**
  Motivo: persistencia relacional com repositorios e mapeamento ORM.
- **Spring Security + JWT**
  Motivo: base para autenticacao stateless, filtros e hardening de headers.
- **Spring WebSocket + STOMP + SockJS**
  Motivo: canal de notificacao em tempo real para pedidos, estoque e pagamentos.
- **Flyway**
  Motivo: versionamento de schema e controle de migracoes.
- **Micrometer + Actuator + Brave/Zipkin**
  Motivo: metricas, health checks e tracing enviado ao Tempo.

### Frontend

- **Angular 17 + TypeScript**
  Motivo: SPA administrativa para dashboard, pedidos, pagamentos e estoque.
- **SockJS + STOMP**
  Motivo: consumo de atualizacoes em tempo real.
- **Thymeleaf + JS server-side no backend**
  Motivo: interface HTML ainda presente no modulo principal.

Observacao importante: o projeto hoje possui **duas abordagens de frontend coexistindo**:

- SPA Angular em `frontend/`
- paginas server-side em `unified-order-system/src/main/resources/templates`

Isso sugere transicao de UI, nao uma historia de frontend plenamente consolidada.

### Banco de dados

- **PostgreSQL**
  Motivo: banco alvo real para execucao containerizada e deploy.
- **H2**
  Motivo: banco em memoria para testes.

### Infraestrutura e DevOps

- **Docker**
  Motivo: empacotamento e execucao padronizada.
- **Docker Compose**
  Motivo: subida local de backend, frontend e infraestrutura auxiliar.
- **GitHub Actions**
  Motivo: build, testes e deploy automatizado.
- **AWS EC2 + ECR**
  Motivo: deploy de imagem containerizada em infraestrutura cloud real.

### Mensageria

- **RabbitMQ**
  Motivo: presente no ecossistema legado para comunicacao assincrona, filas, listeners e DLQ.

Observacao tecnica importante:

- o RabbitMQ esta no `docker-compose.yml`
- o RabbitMQ esta bem representado nos microservicos legados
- o **runtime principal nao usa RabbitMQ no fluxo real**

### Observabilidade

- **Prometheus**
  Motivo: coleta de metricas.
- **Grafana**
  Motivo: dashboards.
- **Loki**
  Motivo: agregacao de logs.
- **Tempo**
  Motivo: tracing distribuido.
- **Alertmanager**
  Motivo: pipeline de alertas.
- **Correlation IDs**
  Motivo: rastreamento entre requests e eventos.

### Tecnologias previstas, mas nao consolidadas

- **Redis**
  Existe infraestrutura e configuracao, mas o modulo ativo nao usa Redis de verdade. O `spring-boot-starter-data-redis` esta comentado no `pom.xml` do modulo principal e o cache atual e `ConcurrentMapCacheManager`.

## 4. Fluxo do Sistema

Fluxo principal real do runtime ativo:

1. O cliente envia `POST /api/orders`.
2. `OrderController` delega para `CreateOrderUseCase`.
3. O caso de uso valida regras de negocio em `OrderBusinessRules`.
4. O pedido e persistido com status `PENDING`.
5. `InventoryService` tenta reservar estoque.
6. O pedido muda para `INVENTORY_RESERVED`.
7. `PaymentService` processa o pagamento e persiste um registro em `payments`.
8. O pedido muda para `PAYMENT_PROCESSING` e logo em seguida para `CONFIRMED`.
9. Eventos de dominio como `OrderCreatedEvent` e `InventoryReservedEvent` sao gravados em `domain_events`.
10. A API retorna `OrderResponse` com `paymentId`, `reservationId`, `transactionId` e `correlationId`.

Fluxo de cancelamento:

1. O cliente envia cancelamento.
2. `CancelOrderUseCase` carrega o pedido.
3. O caso de uso valida se o cancelamento e permitido.
4. O sistema tenta liberar reserva de estoque.
5. O sistema tenta estornar pagamento.
6. O pedido e marcado como `CANCELLED`.
7. Um `OrderCancelledEvent` e persistido.

Leitura critica:

- ha linguagem de saga e compensacao
- o fluxo, porem, e **sincrono e local**
- nao existe coordenacao distribuida real no runtime principal
- o pagamento e **simulado**, nao integrado a um gateway real

## 5. Conceitos de Engenharia Aplicados

### SOLID

**Parcial.**

Ha esforco de separacao de responsabilidades por modulo, use case, service e repository. Mas ainda existem sinais de acoplamento:

- entidades de dominio acopladas ao JPA
- duplicidade entre `OrderService` e `CreateOrderUseCase`
- classes de regra de negocio nao totalmente integradas ao fluxo

### DDD

**Parcial.**

Pontos positivos:

- separacao por subdominios `order`, `inventory` e `payment`
- regras de negocio encapsuladas em classes de dominio
- linguagem de agregados e eventos

Limites atuais:

- ausencia de bounded contexts realmente isolados no runtime principal
- entidades continuam muito influenciadas pelo modelo relacional
- regras criticas ainda usam placeholders, como validacao de cliente e risco

### Clean Architecture

**Parcial.**

Existe intencao clara de separar `application`, `domain`, `repository` e `infrastructure`, principalmente em `order/`. Mas a fronteira nao esta completamente fechada e ainda ha caracteristicas de aplicacao Spring tradicional.

### CQRS

**Parcial e mais forte no legado.**

- o modulo ativo nao possui um read model separado real
- o `order-query-service` legado representa melhor a ideia de CQRS

### Event Driven

**Parcial.**

- no modulo ativo ha eventos persistidos, mas o fluxo principal nao depende de broker
- nos servicos legados existe event-driven com RabbitMQ

### Event Sourcing

**Parcial.**

- a tabela `domain_events` guarda eventos do dominio
- isso suporta auditoria
- mas o estado principal continua sendo reconstruido do banco relacional normal, nao do stream de eventos

### Outbox Pattern

**Parcial e concentrado no legado.**

- existe estrutura de `outbox_events` nas migracoes dos microservicos legados
- o modulo ativo nao implementa outbox operacional equivalente

### Idempotencia

**Parcial e concentrada no legado.**

- as tabelas `processed_messages` nos servicos legados mostram preocupacao com consumo idempotente
- o runtime principal nao expoe um mecanismo equivalente para mensagens distribuidas porque o broker nao participa do fluxo real

### Retry / DLQ

**Parcial e concentrado no legado.**

- `common-messaging` define retry no listener container
- `inventory-service` legado configura DLQ
- o monolito ativo nao usa essas capacidades no fluxo principal

### Rate Limiting

**Sim, mas local.**

- ha `RateLimiterService`
- o algoritmo e em memoria, logo nao escala bem horizontalmente

## 6. Relevancia Para o Mercado Brasileiro

### O projeto demonstra skills demandadas?

**Sim, em arquitetura e engenharia backend.**

O projeto demonstra:

- APIs REST
- JPA/Hibernate
- PostgreSQL
- Docker
- CI/CD
- observabilidade
- cloud deployment em AWS
- mensageria no legado
- conceitos de DDD, Clean Architecture e event-driven

### Ele parece um projeto enterprise?

**Parece enterprise-like na ambicao, mas nao totalmente na execucao final.**

Elementos fortes de perfil enterprise:

- separacao por dominios
- trilha de auditoria por eventos
- stack de observabilidade completa
- deploy em AWS
- testes automatizados em volume relevante
- preocupacao com rate limit, JWT, CORS e tracing

Elementos que ainda impedem classificacao mais alta:

- runtime principal nao fecha a historia de mensageria
- seguranca efetiva ainda esta frouxa
- Redis e RabbitMQ aparecem mais como preparacao do que como capacidade ativa
- frontend e narrativa arquitetural ainda estao em transicao

### Ele e relevante para vagas Junior?

**Sim, como projeto de arquitetura e backend.**

Para vagas Junior ou Estagio em backend, o projeto chama atencao pelo escopo e pelo repertorio arquitetural.

### Ele e relevante para vagas Fullstack .NET ou Backend .NET?

**Parcialmente.**

Esse e o principal limite do portfolio para o perfil do candidato:

- o projeto nao usa C#
- o projeto nao usa .NET
- o frontend nao e React, e Angular

Logo, ele prova:

- capacidade de desenhar sistemas
- maturidade de backend e arquitetura
- nocao de cloud e operacao

Mas **nao prova produtividade direta em stack .NET**, que e exatamente o foco do candidato.

## 7. Como Explicar o Projeto em Entrevista

### Explicacao simples (30 segundos)

Desenvolvi um sistema de gestao de pedidos com arquitetura modular, cobrindo pedidos, estoque e pagamentos. O projeto tem API, banco relacional, auditoria por eventos, observabilidade, testes automatizados, Docker e deploy em AWS, simulando um contexto enterprise real.

### Explicacao tecnica (2 minutos)

O runtime principal do projeto e um monolito modular em Spring Boot, dividido por dominios de `order`, `inventory` e `payment`. O fluxo principal de pedido passa por um caso de uso que valida regras de negocio, persiste o pedido, tenta reservar estoque, processa o pagamento e grava eventos de dominio em uma tabela `domain_events` com `correlationId` para rastreabilidade. Alem disso, o repositorio preserva uma linha legada de microservicos com RabbitMQ, DLQ, CQRS read model e estruturas de outbox, o que mostra exploracao de arquitetura distribuida. Em operacao, o projeto usa PostgreSQL, Docker Compose, GitHub Actions, deploy em AWS EC2/ECR e uma stack de observabilidade com Prometheus, Grafana, Loki, Tempo e Alertmanager. O ponto que eu explicaria com transparencia e que a arquitetura e forte, mas o runtime principal ainda nao fecha toda a historia event-driven: RabbitMQ e outbox estao mais maduros no legado do que no modulo ativo, e a seguranca ainda precisa de hardening para ficar realmente pronta para producao.

## 8. Pontos Fortes do Projeto

- Escopo acima da media para portfolio junior.
- Monolito modular bem separado por dominios de negocio.
- Casos de uso explicitos para criacao e cancelamento de pedido.
- Registro de eventos de dominio com `correlationId`.
- Observabilidade forte com Prometheus, Grafana, Loki, Tempo e Alertmanager.
- Pipeline de CI e roteiro de deploy em AWS.
- Dockerfile multi-stage e compose para ambiente local.
- Quantidade relevante de testes automatizados.
- Exploracao de mensageria, DLQ, outbox e CQRS nos modulos legados.
- Projeto tem cara de problema enterprise real, nao de CRUD simples.

## 9. Pontos a Melhorar

- O projeto nao e `.NET`, o que reduz aderencia ao objetivo profissional do candidato.
- O runtime principal e ambiguo: coexistem monolito modular, microservicos legados, Angular e Thymeleaf.
- RabbitMQ aparece no compose, mas nao participa do fluxo principal atual.
- Redis esta configurado como infraestrutura, mas nao esta conectado ao modulo principal.
- A seguranca e mais estrutural do que efetiva: nao ha fluxo de login ou emissao real de token e as rotas principais estao abertas por configuracao.
- O perfil `docker` nao e `production`, entao `TestController` fica ativo fora de producao e `/api/test/**` esta em `public-paths`.
- Existe segredo JWT default hardcoded como fallback, o que e inadequado.
- O pagamento do modulo principal e simulado, nao integrado a gateway real.
- O inventario usa fallback de `default-stock` e pode aceitar produtos sem estoque persistido, o que enfraquece a consistencia do dominio.
- Os testes passam, mas usam H2 e `inventory.mock-mode=true`, ficando distantes do comportamento real em PostgreSQL.
- Ha dependencia de Testcontainers no `pom`, mas a suite principal nao a utiliza.
- O JaCoCo gera relatorio de **57%** de cobertura total e a regra de coverage so seria checada em `verify`; a CI roda apenas `test`.
- O frontend Angular nao entra na pipeline principal e seu Docker build esta quebrado porque o `frontend/Dockerfile` usa `npm ci` sem `package-lock.json`.
- Os testes Playwright externos existem, mas nao rodam na CI e apontam para `http://localhost:8080`, ou seja, validam a UI Thymeleaf do backend, nao a SPA Angular em `:4200`.
- Existem workflows duplicados de build e teste em GitHub Actions, o que gera redundancia operacional.
- O repositorio possui tres estrategias de contratos de evento: `unified/shared/events`, `shared-events` e `libs/common-events`, o que revela drift arquitetural.

## 10. Melhorias Prioritarias Para Portfolio

1. **Alinhar o core do projeto ao foco de carreira do candidato**
   Se o objetivo e Fullstack `.NET` ou Backend `.NET`, a melhoria de maior impacto seria portar ou replicar o modulo principal em **ASP.NET Core** mantendo a mesma modelagem de dominio. Isso converte arquitetura boa em evidencia direta de stack pedida pelo mercado.

2. **Escolher uma historia arquitetural unica**
   Definir claramente o que e ativo e o que e legado. Idealmente, manter o monolito modular como produto principal e mover `services/` para uma pasta `legacy/` ou repositorio separado. Isso melhora a leitura do portfolio.

3. **Fechar a seguranca de verdade**
   Remover fallback de segredo, proteger APIs principais, criar fluxo real de autenticacao e autorizacao, tirar `/api/test/**` do runtime de deploy e revisar `public-paths`. Isso melhora muito a percepcao de maturidade tecnica.

4. **Implementar outbox e publicacao real no broker no runtime principal**
   Hoje o projeto fala muito de event-driven, mas entrega isso principalmente no legado. Conectar outbox e RabbitMQ no `unified-order-system` elevaria o valor arquitetural imediatamente.

5. **Trocar H2 e mock mode por PostgreSQL e Testcontainers na suite principal**
   Isso aproxima os testes do ambiente real, aumenta confianca e faz a CI capturar problemas de schema, locking e migracao.

6. **Corrigir a historia do frontend**
   Adicionar `package-lock.json`, build automatizado, testes do Angular e deploy do frontend junto com o backend. Hoje existe Angular, mas a experiencia de entrega ainda nao esta fechada.

7. **Substituir o pagamento simulado por integracao externa ou adaptador mais realista**
   Mesmo que seja um sandbox, isso melhora muito a credibilidade do dominio financeiro e do discurso em entrevista.

8. **Implementar cache real com Redis ou remover a afirmacao**
   No estado atual, Redis aparece mais como promessa do que como capacidade. Conectar cache de fato ou simplificar a narrativa evita overclaim.

## 11. Como Colocar no Curriculo

Sistema de gestao de pedidos com arquitetura modular orientada a dominio, desenvolvido em Java 17 e Spring Boot, com APIs REST, PostgreSQL, trilha de eventos, testes automatizados, observabilidade com Prometheus, Grafana, Loki e Tempo, Docker, CI/CD e deploy em AWS.

## 12. Nivel do Projeto

**Classificacao: Pleno**

Motivo:

- o escopo e claramente superior ao de um CRUD junior
- ha preocupacao real com arquitetura, observabilidade, seguranca, testes e deploy
- existem conceitos enterprise relevantes como eventos, compensacao, mensageria, DLQ e correlacao

Mas eu nao classificaria como `Enterprise-like` porque:

- a arquitetura principal ainda nao esta completamente fechada
- a seguranca efetiva ainda tem brechas
- o story de event-driven e outbox esta fragmentado
- frontend, CI e runtime ainda mostram drift

## 13. Checklist de Mercado

| REQUISITO MERCADO | PRESENTE NO PROJETO | OBSERVACAO |
|---|---|---|
| C# | Nao | Repositorio nao possui codigo C# |
| .NET | Nao | Nao ha `*.csproj`, `*.sln` ou ASP.NET Core |
| Java / Spring Boot | Sim | E a stack principal do backend |
| APIs REST | Sim | Forte no modulo ativo e nos servicos legados |
| PostgreSQL | Sim | Banco alvo real em Docker e deploy |
| SQL / ORM | Sim | JPA/Hibernate com repositories e migracoes Flyway |
| EF Core | Nao | ORM usado e Hibernate |
| React | Nao | O frontend moderno e Angular 17 |
| Angular | Sim | SPA em `frontend/` |
| Thymeleaf | Sim | UI server-side ainda presente no backend |
| Docker | Sim | Dockerfile multi-stage e Compose |
| CI/CD | Parcial | GitHub Actions existe, mas cobre basicamente o backend principal e com workflows duplicados |
| AWS | Parcial | Deploy em EC2/ECR e script de setup, sem IaC |
| RabbitMQ | Parcial | Forte nos servicos legados, ausente no fluxo principal |
| Redis | Parcial | Infra existe, uso real nao esta consolidado |
| Clean Architecture | Parcial | Boa intencao, execucao ainda acoplada ao JPA e Spring |
| DDD | Parcial | Dominios separados e business rules, mas com varias simplificacoes |
| CQRS | Parcial | Mais visivel no `order-query-service` legado |
| Event Driven | Parcial | Forte no legado; no ativo ha mais event log do que event-driven distribuido |
| Event Sourcing | Parcial | `domain_events` registra eventos, mas nao dirige o estado principal |
| Outbox Pattern | Parcial | Estruturas no legado, nao no runtime principal |
| Idempotencia | Parcial | Evidencias em `processed_messages` no legado |
| Retry / DLQ | Parcial | Configurado em mensageria legada |
| Testes automatizados | Sim | `mvn test` executou 238 testes locais com sucesso |
| Playwright | Parcial | Suite existe, mas nao esta integrada ao pipeline principal |
| Testcontainers | Parcial | Dependencia existe, uso real nao e o caminho principal dos testes |
| Observabilidade | Sim | Prometheus, Grafana, Loki, Tempo e Alertmanager |
| Kubernetes | Nao | Nao ha manifests ou Helm |
| Terraform | Nao | Nao ha IaC |

## 14. Score Final do Projeto

**Nota final: 6,8 / 10**

Leitura da nota:

- **Arquitetura: 8,0/10**
  O projeto demonstra repertorio forte para eventos, modularizacao, observabilidade e pensamento enterprise.

- **Engenharia de execucao: 6,5/10**
  Ha testes, Docker, CI e deploy, mas tambem existem drift arquitetural, seguranca frouxa, frontend inconsistente e dependencias de H2 e mock mode.

- **Relevancia para vagas do alvo do candidato: 4,5/10**
  O maior desconto da nota vem do desalinhamento com `.NET` e `React`, que sao justamente os eixos centrais do mercado-alvo descrito.

Conclusao final:

Este e um **projeto forte como demonstracao de maturidade arquitetural e ambicao tecnica**, especialmente para backend e sistemas distribuidos. Para portfolio geral de engenharia, ele tem peso. Para portfolio focado em **Fullstack .NET / Backend .NET no Brasil**, ele **nao deve ser o unico projeto principal**, porque prova muito mais arquitetura e Java/Spring do que stack .NET. O melhor uso deste projeto e como prova de pensamento enterprise, acompanhado por ao menos um projeto mais enxuto e muito bem executado em **ASP.NET Core + React** ou **ASP.NET Core puro**.

## 15. Atualizacao de Execucao - 2026-03-13

Esta secao complementa a analise acima sem substituir o historico original. O repositorio foi endurecido tecnicamente em varias frentes e parte relevante dos pontos de melhoria foi de fato corrigida no codigo, na pipeline e na narrativa arquitetural.

### Status consolidado dos pontos de melhoria

- **Historia arquitetural unica:** praticamente resolvida. O runtime ativo ficou explicitamente concentrado em `frontend/` + `unified-order-system/` e os microservicos antigos foram isolados em `legacy/`, fora do build e da pipeline principal.
- **Ambiguidade dos contratos de evento:** resolvida com delimitacao mais clara. Os eventos internos do monolito deixaram de morar em `unified/shared/events` e passaram para `unified/domain/events`, enquanto `libs/common-events` ficou como contrato ativo de integracao e `legacy/shared-events` permaneceu apenas como compatibilidade historica.
- **Seguranca efetiva:** resolvida em grande parte. Agora existe fluxo real de autenticacao com `POST /api/auth/login`, emissao de JWT, protecao das rotas principais, guard/interceptor no Angular, endurecimento dos `public-paths`, remocao do fallback inseguro de segredo e exclusao do `TestController` do runtime de producao.
- **RabbitMQ e outbox no runtime principal:** resolvido. O modulo ativo passou a publicar eventos de dominio por outbox para o broker, com topologia RabbitMQ ligada ao runtime real.
- **Redis como capacidade real:** resolvido. O cache saiu da narrativa de promessa e passou a participar do runtime principal.
- **Pagamento do modulo principal:** parcialmente resolvido. Continua sem PSP de mercado real, mas deixou de ser apenas simulacao interna e passou a usar adaptador externo/sandbox, o que melhora bastante a credibilidade tecnica.
- **Consistencia do inventario:** resolvida para o runtime real. O fallback de `default-stock` ficou restrito ao modo mock; no fluxo real o sistema exige estoque persistido e falha quando ele nao existe.
- **Suite principal em PostgreSQL/Testcontainers:** resolvida. Os testes principais passaram a usar PostgreSQL com Testcontainers como caminho padrao, com fallback para PostgreSQL embarcado quando Docker nao esta disponivel.
- **Cobertura e gate de qualidade:** resolvido. O JaCoCo agora participa do `verify`, a pipeline principal executa `verify` e a cobertura total medida localmente apos as correcoes ficou em **63,27%**.
- **Frontend Angular fora da entrega principal:** resolvido. O Angular agora tem `package-lock.json` rastreado, testes unitarios executaveis, build automatizado, smoke E2E com Playwright apontando para `:4200` e deploy conjunto com o backend.
- **Workflows duplicados:** resolvido. A validacao foi consolidada em um unico workflow principal de CI, reduzindo redundancia operacional.

### O que ainda permanece como ressalva honesta

- O projeto continua sendo **Java + Spring Boot + Angular**, nao `.NET`. Para o alvo profissional do candidato, isso ainda reduz aderencia direta de stack.
- O pagamento esta mais crivel, mas ainda e **sandbox/adaptador**, nao integracao produtiva com um gateway comercial real.
- O backend ainda preserva views Thymeleaf legadas. Isso ja nao define a historia principal do produto, mas ainda mostra coexistencia de camadas de UI em transicao.

### Novo score revisado

**Nota final revisada: 8,1 / 10**

Leitura da revisao:

- **Arquitetura: 8,7/10**
  A arquitetura ficou mais coerente porque o runtime ativo foi isolado, o outbox entrou no fluxo principal e o drift dos contratos de evento foi reduzido.

- **Engenharia de execucao: 8,2/10**
  Houve ganho real em autenticacao, pipeline, cobertura, testes com PostgreSQL, deploy conjunto de frontend/backend e integracao operacional de Redis e RabbitMQ.

- **Aderencia ao alvo Fullstack/Backend .NET: 5,0/10**
  O principal desconto da nota continua sendo o desalinhamento de stack. O projeto esta mais maduro, mas ainda prova Java/Spring/Angular muito mais do que C#/.NET.

### Nova leitura final para portfolio

Depois das correcoes, este projeto deixa de ser apenas "ambicioso com gaps" e passa a ser um **case bem mais defensavel de backend enterprise-like em producao simulada**, com autenticacao real, pipeline principal fechada, stack operacional coerente e narrativa arquitetural muito mais limpa. Para portfolio geral de engenharia ele sobe bastante de nivel. Para portfolio focado em `.NET`, continua sendo um excelente projeto complementar, mas ainda nao substitui a necessidade de um case forte em **ASP.NET Core**.
