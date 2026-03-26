# 1 Visão Geral do Projeto

Este projeto implementa um sistema de gestão de pedidos com foco em todo o ciclo operacional do pedido: autenticação, criação do pedido, reserva de estoque, processamento de pagamento, confirmação/cancelamento, painel operacional e trilha de eventos. O domínio é claramente enterprise: coordenação de pedidos, estoque e pagamento com preocupação explícita com consistência, concorrência, auditoria e operação em produção.

O problema que o sistema resolve é o típico de plataformas transacionais: evitar venda sem estoque, manter o fluxo de pagamento integrado ao pedido, reduzir acoplamento entre módulos e dar visibilidade operacional do que está acontecendo no sistema.

Observação importante desta auditoria: apesar do nome do repositório e da pasta `legacy/` sugerirem uma arquitetura distribuída com Event Sourcing mais agressivo, o runtime principal atual está concentrado em `unified-order-system/` e `frontend/`. Na prática, hoje o sistema funciona como um modular monolith bem estruturado, com bibliotecas compartilhadas e um histórico de evolução a partir de uma abordagem mais fragmentada em microservices.

# 2 Arquitetura do Sistema

A arquitetura escolhida faz sentido para o problema e para a fase de maturidade do projeto. Em vez de manter múltiplos serviços independentes com alto custo operacional, o núcleo ativo foi consolidado em um monólito modular que preserva separação de responsabilidades por domínio.

Os principais padrões observados no código são:

- Modular Monolith por domínio, com módulos como `order`, `inventory`, `payment`, `auth`, `websocket` e `infrastructure`.
- Influência clara de DDD, com regras de negócio separadas em classes como `OrderBusinessRules`, `InventoryBusinessRules` e `PaymentBusinessRules`.
- Influência de Clean Architecture, com camadas de aplicação e casos de uso em `order/application`, além de repositórios e componentes de infraestrutura desacoplados em pacotes próprios.
- Comunicação orientada a eventos por meio de publicação de eventos de domínio em tabela de outbox e posterior envio ao RabbitMQ.
- Padrão Outbox implementado na prática por `EventPublisher`, `DomainEventEntity` e `DomainEventOutboxPublisher`.

Como isso aparece no código real:

- O fluxo de criação do pedido é orquestrado em `CreateOrderUseCase`, que valida o pedido, consulta/reserva estoque, processa pagamento e confirma ou compensa a operação.
- O cancelamento é tratado em `CancelOrderUseCase`, que aciona liberação de reserva e reembolso.
- As regras transacionais críticas ficam concentradas no backend unificado, e não distribuídas de forma arbitrária entre controllers.
- A publicação de eventos não é “fire and forget” direto para o broker; ela passa primeiro por persistência, o que reduz risco de perda entre banco e mensageria.

O ponto técnico mais importante aqui é honestidade arquitetural: este repositório não é, no runtime atual, um conjunto de microservices independentes plenamente ativos. Ele demonstra maturidade de arquitetura enterprise, mas em formato consolidado. Isso não diminui o projeto; apenas muda a forma correta de apresentá-lo.

# 3 Stack Tecnológica

## Backend

- Java 17: linguagem principal do runtime ativo, adequada para aplicações enterprise.
- Spring Boot 3: acelera configuração, integração com segurança, observabilidade, dados e APIs REST.
- Spring Web / MVC: exposição de APIs HTTP para pedidos, estoque, pagamento, autenticação e dashboards.
- Spring Security: base para autenticação/autorização com filtros JWT, controle de endpoints e headers de segurança.
- Spring Data JPA / Hibernate: persistência relacional com repositórios, mapeamento ORM e suporte a locking/versão.
- Flyway: versionamento de schema e evolução controlada do banco.
- Micrometer + Actuator: métricas, health checks e integração com Prometheus.

## Frontend

- Angular 17: SPA principal do projeto, com rotas, guards, interceptor de autenticação e formulários.
- RxJS: reatividade no consumo de APIs e autenticação.
- WebSocket/STOMP client: existe infraestrutura para atualização em tempo real, embora o uso no runtime atual pareça parcial.

## Banco de Dados

- PostgreSQL: banco relacional principal do sistema, adequado para consistência transacional.
- Redis: usado como cache distribuído quando habilitado; há fallback para cache em memória quando desabilitado.

## Mensageria

- RabbitMQ: broker para publicação de eventos via outbox.
- Biblioteca interna `common-messaging`: centraliza convenções de exchange, fila, DLQ e configuração AMQP.

## Infraestrutura e DevOps

- Docker / Docker Compose: ambiente local, ambiente produtivo e stack de observabilidade.
- GitHub Actions: pipeline de CI e workflow de deploy para EC2.
- AWS EC2: alvo de deploy observado no pipeline.
- Dependabot: atualização automatizada de dependências.
- WireMock: sandbox para simular gateway de pagamento.

## Observabilidade

- Prometheus: coleta de métricas.
- Grafana: visualização de dashboards.
- Loki: centralização de logs.
- Tempo: tracing distribuído.
- Alertmanager: estrutura para alertas.

Por que essa stack foi usada:

- Ela prioriza robustez operacional e previsibilidade em cenários transacionais.
- O backend usa componentes clássicos de mercado enterprise.
- O frontend entrega uma interface operacional real, e não apenas Swagger.
- A infraestrutura demonstra preocupação com operação, monitoramento e deploy, o que aumenta o peso do projeto no portfólio.

# 4 Fluxo do Sistema

O fluxo principal do sistema, conforme o código ativo, pode ser explicado assim:

1. O usuário autentica na aplicação e recebe um token JWT.
2. O frontend chama a API de pedidos para criar uma nova ordem.
3. O backend valida a requisição, cria o pedido e executa o caso de uso de criação.
4. O módulo de estoque verifica disponibilidade e cria reserva para evitar overselling.
5. O módulo de pagamento processa a tentativa de cobrança via gateway HTTP ou sandbox local.
6. Se tudo der certo, o pedido é confirmado.
7. Se houver falha em estoque ou pagamento, o fluxo executa compensação e atualiza o estado do pedido.
8. Eventos de domínio são persistidos em tabela própria.
9. Um publisher de outbox envia os eventos pendentes ao RabbitMQ quando o recurso está habilitado.
10. Um scheduler também trata expiração automática de reservas, o que evita estoque preso indefinidamente.

Na prática, o sistema combina transação local forte com integração assíncrona posterior. Isso é uma decisão correta para reduzir inconsistência entre regra de negócio e mensageria.

# 5 Conceitos de Engenharia Aplicados

| Conceito | Status no projeto | Evidência técnica |
| --- | --- | --- |
| SOLID | Parcialmente bem aplicado | Há boa separação por responsabilidade em vários serviços e regras de domínio, mas alguns controllers ainda concentram orquestração demais, especialmente em estoque e dashboard. |
| DDD | Sim, com execução parcial | O domínio está organizado por contexto funcional, com regras explícitas e entidades próprias. Não é um DDD “canônico” puro, mas está acima da média de projetos de portfólio. |
| Clean Architecture | Parcial | Existem camadas e casos de uso, porém nem todo acesso passa por portas/adapters estritos; parte do sistema ainda fala diretamente com repositórios concretos. |
| CQRS | Parcial e mais evidente no legado | O repositório contém histórico de query service em `legacy/`, mas o runtime principal atual usa majoritariamente leitura direta no mesmo backend. |
| Event Driven | Sim, mas não de ponta a ponta no runtime ativo | Eventos são gerados e publicados via outbox, porém o runtime principal não demonstra uma cadeia completa de consumers independentes processando o core do negócio. |
| Outbox Pattern | Sim | Implementado com persistência em tabela de eventos e publisher assíncrono para o broker. |
| Idempotência | Parcial | Há alguma proteção contextual, mas não existe um desenho forte de idempotency key para as operações críticas de API. |
| Retry / DLQ | Parcial | A infraestrutura de mensageria prevê retry e DLQ, mas isso não está plenamente exercitado no runtime ativo, porque os consumidores assíncronos não são o centro do fluxo atual. |
| Event Sourcing | Não no sentido estrito | O projeto registra eventos e tem histórico de abordagem orientada a eventos, mas o estado atual do sistema não reconstrói agregados a partir do event store. |

Síntese técnica: o projeto demonstra arquitetura madura e boas decisões reais, mas alguns conceitos aparecem de forma híbrida, parcial ou histórica. Isso é aceitável, desde que seja apresentado com precisão.

# 6 Relevância Para o Mercado Brasileiro

Sim, o projeto demonstra várias skills demandadas por vagas reais:

- APIs REST.
- Banco relacional.
- ORM.
- Docker.
- GitHub Actions.
- Mensageria com RabbitMQ.
- Observabilidade.
- Segurança com JWT e políticas de acesso.
- Testes automatizados.
- Arquitetura modular com preocupação enterprise.

Ele também tem aparência de projeto enterprise, principalmente porque foge do padrão “CRUD simples” e mostra problemas reais de mercado: concorrência, reserva de estoque, pagamento, compensação, eventos, monitoramento e deploy.

Para vagas Junior, ele é forte. Para vagas Fullstack ou Backend, ele transmite maturidade acima da média de portfólio acadêmico. O problema é outro: o alvo do candidato é `.NET`, enquanto o runtime ativo do projeto é `Java/Spring + Angular`.

Conclusão de mercado:

- Como prova de capacidade arquitetural, o projeto é muito bom.
- Como prova direta de stack `.NET/C#`, ele é fraco.
- Como projeto de portfólio para demonstrar pensamento enterprise, ele agrega bastante valor.
- Como único projeto principal para vagas `.NET`, ele não é suficiente sozinho.

# 7 Como Explicar o Projeto em Entrevista

## Explicação simples (30 segundos)

Construí um sistema de gestão de pedidos com foco em cenário enterprise, cobrindo autenticação, criação de pedidos, reserva de estoque, integração de pagamento, mensageria e observabilidade. O projeto foi organizado em módulos de domínio, com publicação de eventos e preocupação com segurança, concorrência e operação em produção.

## Explicação técnica (2 minutos)

O projeto implementa um fluxo transacional completo de pedidos usando um backend modular, com separação por domínios como pedido, estoque, pagamento e autenticação. A arquitetura atual é um modular monolith com forte influência de DDD e Clean Architecture, o que permitiu centralizar consistência transacional sem perder organização interna.

No fluxo principal, a criação do pedido passa por um caso de uso que valida os dados, reserva estoque, processa o pagamento e confirma ou compensa a operação. Para desacoplar integração assíncrona, os eventos de domínio são persistidos em uma tabela de outbox e publicados depois no RabbitMQ, reduzindo risco de inconsistência entre banco e broker.

O projeto também inclui JWT, RBAC, headers de segurança, rate limiting, cache com Redis, monitoramento com Prometheus/Grafana/Loki/Tempo, testes automatizados, Docker Compose e pipeline de CI/CD. O ponto que eu destacaria com honestidade é que ele demonstra maturidade arquitetural enterprise, mas hoje está implementado em Java/Spring e Angular, não em .NET, então eu o posicionaria como prova de engenharia e arquitetura, não como prova direta de stack C#.

# 8 Pontos Fortes do Projeto

- Arquitetura modular bem acima do padrão de portfólio Junior.
- Domínio escolhido é forte para mercado enterprise: pedidos, estoque e pagamento.
- Uso real de mensageria com RabbitMQ e outbox.
- Preocupação concreta com consistência e concorrência, incluindo optimistic locking e locking pessimista em estoque.
- Segurança acima da média, com JWT, RBAC, CORS, CSP, HSTS e rate limiting.
- Observabilidade robusta, com stack completa de métricas, logs e tracing.
- Ambiente Docker relativamente completo, incluindo sandbox de pagamento.
- Pipeline de CI e fluxo de deploy para EC2.
- Testes backend relevantes: a suíte atual registra 217 testes, sem falhas na última execução auditada.
- Cobertura backend razoável para portfólio: 63,29% de linhas e 40,84% de branches via JaCoCo.
- Existência de testes E2E e teste de carga, o que eleva o projeto em relação à maioria dos portfólios.

# 9 Pontos a Melhorar

- O principal desalinhamento é de stack: o projeto não é `.NET`, o que reduz aderência direta para vagas C#.
- O frontend é Angular, enquanto o alvo preferencial do candidato é `.NET + React`.
- O runtime ativo hoje é menos distribuído do que o nome do repositório sugere; isso precisa ser explicado com transparência.
- Event Driven existe, mas de forma parcial no runtime principal: a publicação assíncrona está presente, porém os consumers não são o coração do fluxo atual.
- CQRS está mais como herança arquitetural do que como prática viva no runtime ativo.
- Idempotência ainda não aparece de forma robusta nas operações críticas de API.
- Retry e DLQ existem mais como infraestrutura preparada do que como fluxo operacional totalmente demonstrado.
- Há controllers com responsabilidade excessiva, o que enfraquece a pureza arquitetural.
- O rate limiting é local em memória por instância; em ambiente com múltiplas réplicas ele não se comporta como rate limit distribuído.
- O frontend possui pouca cobertura unitária no estado atual: apenas 2 specs em `frontend/src`.
- O E2E ativo visível é enxuto: 1 spec Playwright no diretório de testes auditado.
- Existem traços de documentação e scripts desatualizados, inclusive referências antigas a React, query service e Render.
- Algumas regras de negócio ainda estão em nível de placeholder ou simplificação, especialmente no domínio de pedidos/pagamentos.
- A infraestrutura de WebSocket existe, mas o acoplamento real desse canal com os eventos do domínio não está totalmente evidente no runtime ativo.

# 10 Melhorias Prioritárias Para Portfólio

1. Criar uma versão `.NET 8` do núcleo de pedidos.
Por que ajuda no mercado: isso transforma imediatamente o projeto em prova concreta de ASP.NET Core, C#, EF Core, DI, autenticação e arquitetura enterprise aplicada à stack alvo.

2. Adicionar um frontend em React ou migrar a interface principal para React.
Por que ajuda no mercado: vagas Fullstack `.NET` no Brasil pedem React com frequência maior do que Angular. Essa mudança melhora aderência sem desperdiçar a arquitetura já construída.

3. Tornar o fluxo assíncrono mais explícito no runtime ativo, com consumers reais, retry operacional e DLQ demonstrável.
Por que ajuda no mercado: isso fortalece o discurso de arquitetura orientada a eventos e aproxima o projeto do que empresas realmente usam com RabbitMQ, SQS ou Kafka.

4. Fortalecer testes de integração e contrato nas bordas críticas.
Por que ajuda no mercado: aumenta credibilidade técnica e mostra maturidade de engenharia, principalmente para pagamento, estoque e autenticação.

5. Adicionar IaC para AWS com Terraform ou CDK.
Por que ajuda no mercado: reforça o discurso de cloud e produção, muito valorizado em vagas backend e enterprise.

6. Evoluir idempotência, deduplicação e garantias operacionais.
Por que ajuda no mercado: esse tipo de cuidado diferencia um projeto “bonito” de um projeto realmente pronto para ambientes transacionais.

7. Melhorar a camada de documentação arquitetural.
Por que ajuda no mercado: facilita apresentação em entrevista, onboarding de avaliadores e leitura rápida por recrutadores técnicos.

# 11 Como Colocar no Currículo

Sistema de gestão de pedidos com arquitetura modular enterprise, desenvolvido com Java 17, Spring Boot e Angular, integrando PostgreSQL, Redis e RabbitMQ, com uso de JWT, outbox pattern, observabilidade completa e testes automatizados para cenários de pedidos, estoque e pagamento.

# 12 Nível do Projeto

Classificação: **Enterprise-like**

Motivo:

- O projeto está muito acima do nível Junior tradicional em termos de escopo e preocupação arquitetural.
- Ele demonstra componentes que normalmente aparecem em ambientes reais: mensageria, segurança, concorrência, monitoramento, testes, deploy e domínio transacional.
- Ao mesmo tempo, ainda existem lacunas para chamá-lo de “enterprise completo” sem ressalvas, como event driven parcial no runtime atual, idempotência incompleta, alguns acoplamentos internos e desalinhamento com a stack-alvo do candidato.

Em outras palavras: é um projeto com cara de produção, mais maduro do que a maioria dos portfólios, mas ainda em evolução.

# 13 Checklist de Mercado

| Requisito de mercado | Presente no projeto | Observação |
| --- | --- | --- |
| C# | Não | Ausente no runtime atual. |
| .NET / ASP.NET Core | Não | Principal gap para o objetivo profissional do candidato. |
| Java / Spring Boot | Sim | É a stack principal do backend ativo. |
| APIs REST | Sim | Forte presença no runtime principal. |
| PostgreSQL | Sim | Banco principal com Flyway e JPA. |
| EF Core | Não | Não aplicável à stack atual. |
| JPA / Hibernate | Sim | ORM principal do backend. |
| Angular | Sim | Frontend principal ativo. |
| React | Não | Recomendado para aderência ao mercado-alvo. |
| Redis | Sim | Usado como cache distribuído quando habilitado. |
| RabbitMQ | Sim, parcialmente central | Publicação via outbox está ativa; uso intensivo de consumers no runtime principal é limitado. |
| Kafka / SQS / Service Bus | Não | Não observado. |
| Docker | Sim | Compose local, produtivo e de observabilidade. |
| CI/CD | Sim | GitHub Actions para CI e deploy em EC2. |
| AWS | Sim, parcialmente demonstrado | Há deploy para EC2; faltam evidências de IaC e serviços gerenciados mais amplos. |
| Observabilidade | Sim | Prometheus, Grafana, Loki, Tempo e Alertmanager. |
| Testes unitários | Sim | Backend possui suíte robusta; frontend ainda é limitado. |
| Testes de integração | Sim | Há cobertura relevante no backend. |
| Testes E2E | Parcial | Existe Playwright, mas o volume visível de cenários ainda é pequeno. |
| Teste de carga | Sim | Existe script k6. |
| DDD | Sim, parcial | Aplicado de forma prática, não acadêmica. |
| Clean Architecture | Parcial | Boa estrutura, mas não totalmente rigorosa. |
| CQRS | Parcial | Mais evidente no legado do que no runtime ativo. |
| Event Driven | Parcial | Eventos e outbox existem, mas a arquitetura atual é mais síncrona no core. |
| Outbox Pattern | Sim | Um dos melhores pontos do projeto. |
| Idempotência | Parcial | Ainda precisa amadurecer. |
| Retry / DLQ | Parcial | Infra preparada, demonstração operacional incompleta. |
| Segurança com JWT | Sim | Bem implementada para nível de portfólio. |
| Rate limiting | Sim, local | Em memória por instância, sem coordenação distribuída. |
| Kubernetes | Não | Não observado. |
| Terraform / CDK | Não | Recomendado como evolução. |
| NoSQL | Não | Não observado no runtime auditado. |
| gRPC | Não | Não observado. |

# 14 Score Final do Projeto

**Nota final: 8,0 / 10**

Justificativa da nota:

- Em engenharia e arquitetura, o projeto é forte. Ele demonstra organização de domínio, segurança, mensageria, cache, deploy, observabilidade e testes em um nível acima do padrão de portfólio Junior.
- Em relevância para vagas enterprise, ele também é forte, porque o domínio e as preocupações técnicas são realistas.
- A nota não sobe mais porque há três limitações objetivas: a stack principal não é `.NET`, parte dos conceitos avançados aparece de forma parcial no runtime atual, e ainda existem lacunas operacionais importantes em idempotência, testes frontend, consumers assíncronos e documentação arquitetural consolidada.

Conclusão final: este é um projeto de portfólio forte, com aparência profissional e qualidade acima da média, mas ele gera mais valor como prova de capacidade arquitetural enterprise do que como prova direta de experiência em `.NET`. Para o objetivo de carreira do candidato, a melhor estratégia é usar este projeto como base e evoluí-lo para uma versão explicitamente alinhada com `.NET + AWS`, ou complementá-lo com um segundo projeto nessa stack.
