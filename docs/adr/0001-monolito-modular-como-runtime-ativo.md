# 0001 — Monólito modular (`unified-order-system`) como runtime ativo

- Status: aceito (registrado retroativamente)
- Data: 2026-11

## Contexto

O repositório começou com microsserviços separados (`order-service`, `order-query-service`,
`inventory-service`, `payment-service`) e um módulo `shared-events`. Esses serviços estão em
`legacy/` e não fazem parte do build raiz (`pom.xml` só lista `libs/common-*` e
`unified-order-system`), do CI (`.github/workflows/ci.yml`) nem do `docker-compose.yml`.

## Decisão

O produto ativo é o monólito modular `unified-order-system`, com módulos por contexto (`order/`,
`inventory/`, `payment/`, `query/`) no mesmo processo e no mesmo banco PostgreSQL. As bibliotecas
`libs/common-*` são compartilhadas por ele. `legacy/` é apenas referência histórica.

A orquestração do pedido (reserva de estoque -> pagamento -> confirmação, com compensação no
cancelamento) é feita em processo pelos casos de uso `CreateOrderUseCase` e `CancelOrderUseCase`,
dentro de uma transação de banco. O RabbitMQ é usado para distribuir eventos **depois** do commit
(outbox), não para coordenar a saga.

## Consequências

- Consistência forte entre pedido, reserva e pagamento local (uma transação), sem saga distribuída.
- O "distribuído" do sistema está na publicação de eventos (outbox -> RabbitMQ) e nos consumidores,
  não na divisão do domínio em processos.
- Alterações em `legacy/` não são testadas pelo CI; qualquer reaproveitamento deve ser tratado como
  migração.
