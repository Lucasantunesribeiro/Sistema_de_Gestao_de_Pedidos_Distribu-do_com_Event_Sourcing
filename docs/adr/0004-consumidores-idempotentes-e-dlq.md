# 0004 — Consumidores idempotentes com `processed_events`, ack após commit e DLQ

- Status: aceito
- Data: 2026-11

## Contexto

O outbox entrega *at-least-once* (ADR 0003). Até aqui não havia nenhum `@RabbitListener` no código
ativo: as filas `*.audit.queue` eram declaradas sem consumidor e sem dead-letter. A factory
compartilhada (`MessagingAutoConfiguration`) já fazia retry stateless (3 tentativas) seguido de
reject sem requeue, mas, sem `x-dead-letter-exchange` na fila, a mensagem rejeitada seria descartada.

## Decisão

- Tabela `processed_events (consumer, event_id)` com chave primária composta (migration `V12`).
- `IdempotentMessageProcessor.processOnce(consumer, eventId, efeito)` numa única transação:
  se o par existe, ignora; senão insere o marcador com flush imediato e executa o efeito. Falha no
  efeito desfaz o marcador. Duas entregas simultâneas: a segunda esbarra na PK, faz rollback e, no
  retry, é descartada como duplicata.
- Primeiro consumidor: `OrderProjectionListener` (fila `order.projection.queue`, ligada a
  `order.exchange` com `order.#`), que mantém o read model `order_projection`.
  - `eventId` = `messageId` (id da linha em `domain_events`).
  - O projetor aplica por versão: versão já aplicada é ignorada; lacuna lança
    `OutOfOrderEventException`.
  - Mensagem malformada lança `AmqpRejectAndDontRequeueException`; a política de retry da factory
    (`MessagingAutoConfiguration.listenerRetryPolicy`) não tenta de novo essa exceção (nem como
    causa), então ela vai direto para a DLQ.
- O outbox preserva a ordem por stream: se o envio da versão N falha, as versões seguintes do mesmo
  stream ficam para o próximo ciclo. Sem isso, N+1 chegaria antes de N, esgotaria o retry por
  lacuna e iria para a DLQ, deixando a projeção parada.
- Ack em modo `AUTO` (explícito na factory): só é enviado quando o listener retorna, ou seja, após o
  commit. Exceção -> 3 tentativas com backoff -> reject sem requeue -> `order.dlx` ->
  `order.projection.dlq`.
- Fila nova com DLX em vez de alterar `order.audit.queue`: redeclarar uma fila existente com outros
  argumentos causa `PRECONDITION_FAILED` no RabbitMQ.
- `OrderProjectionRebuilder` (POST `/api/projections/orders/rebuild`, ADMIN) apaga a projeção e os
  marcadores do consumidor e reaplica o event store pelo mesmo caminho do consumidor. O projetor não
  lê relógio, então duas execuções geram as mesmas linhas.
- O consumidor é ligado por `ORDER_PROJECTION_CONSUMER_ENABLED` (padrão `true` no perfil `docker`,
  `false` fora dele).

## Consequências

- Entrega duplicada não duplica efeito (testes em `IdempotentOrderProjectionConsumerTest`).
- Mensagens fora de ordem que esgotam o retry vão para a DLQ; o rebuild por replay corrige a
  projeção. Não há reprocessamento automático da DLQ.
- O retry stateless roda na thread do consumidor e bloqueia a fila durante o backoff (até ~3 s por
  mensagem com falha).
- `processed_events` cresce sem limpeza automática.
- As filas `*.audit.queue` continuam sem consumidor e acumulam mensagens no broker.
