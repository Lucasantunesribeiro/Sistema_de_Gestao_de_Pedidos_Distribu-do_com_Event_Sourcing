# 0002 — Event store em `domain_events` com versão por stream e concorrência otimista

- Status: aceito
- Data: 2026-11

## Contexto

Os eventos de domínio já eram gravados em `domain_events`, que também é o outbox lido pelo
`DomainEventOutboxPublisher`. Porém:

- não havia número de sequência por agregado: a "ordem" era `created_at`, que empata dentro de uma
  transação e não detecta lacunas;
- não havia checagem de versão ao gravar: dois cancelamentos concorrentes do mesmo pedido gravavam
  dois eventos sem conflito;
- não havia replay: o histórico não era usado para reconstruir nada, e o fluxo principal só emitia
  `OrderCreatedEvent` no fim (já com o pedido `CONFIRMED`), então o histórico não refletia as
  transições.

O estado corrente do pedido continua em `system_orders` (JPA), usado pelas consultas e telas.

## Decisão

- Stream = `(aggregate_type, aggregate_id)`. Cada evento recebe `aggregate_version` 1..n
  (migration `V11`, com backfill por `created_at, id` para dados existentes) e há restrição única
  `(aggregate_type, aggregate_id, aggregate_version)`.
- `EventStore.append(tipo, id, versãoEsperada, eventos)` falha com `ConcurrencyException`
  (HTTP 409) se a versão atual for diferente da esperada; a restrição única cobre a corrida entre a
  checagem e o insert.
- `OrderAggregate` (puro, sem JPA) é reconstruído por `rehydrate` aplicando os eventos em ordem de
  versão. Versões com lacuna/duplicadas, primeiro evento diferente de `OrderCreatedEvent`, evento
  desconhecido ou transição inválida geram `EventStreamCorruptedException`.
- A máquina de estados fica em `OrderStatus.canTransitionTo` e é a mesma para comandos e replay.
- Os casos de uso gravam no stream `Order` o `OrderCreatedEvent` (v1) e um
  `OrderStatusUpdatedEvent` por transição, ou `OrderCancelledEvent`, sempre via
  `OrderEventSourcingRepository.save` (versão esperada = versão carregada).
- `system_orders` continua sendo gravado na mesma transação. É um modelo híbrido: o event store é a
  fonte para auditoria/replay e para detectar conflito; a tabela é o estado consultado pela API.
  `GET /api/orders/{id}/replay` (ADMIN) compara os dois.

## Consequências

- Escritas concorrentes no mesmo pedido falham com 409 em vez de sobrescrever.
- O replay é determinístico: não lê relógio nem dados externos; o mesmo stream gera o mesmo estado.
- Pedidos criados antes desta mudança têm streams incompletos (só `OrderCreatedEvent`, emitido no
  fim do fluxo antigo). O replay deles indica `PENDING` mesmo que a tabela diga `CONFIRMED`;
  `matchesStoredState=false` no endpoint de replay sinaliza isso. Pedidos sem stream algum ganham um
  `OrderCreatedEvent` sintético (a partir da linha atual) no primeiro cancelamento.
- A ordem das colunas da restrição única serve às buscas por stream; não há snapshot, então streams
  muito longos teriam replay linear (pedidos têm poucos eventos).
