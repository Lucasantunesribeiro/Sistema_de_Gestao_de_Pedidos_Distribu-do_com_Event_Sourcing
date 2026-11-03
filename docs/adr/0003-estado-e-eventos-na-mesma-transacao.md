# 0003 — Estado e eventos na mesma transação (outbox transacional)

- Status: aceito
- Data: 2026-11

## Contexto

`EventPublisher.publish` usava `Propagation.REQUIRES_NEW` "para o evento sobreviver a falhas da
transação pai". Na prática, quando `CreateOrderUseCase` falhava depois de publicar
`InventoryReservedEvent` (ou o antigo `OrderCreatedEvent`), o pedido sofria rollback mas o evento
ficava gravado e era enviado ao RabbitMQ, descrevendo um pedido que não existe.
`publishWithinTransaction` fazia o oposto: engolia qualquer erro com um log, permitindo estado
confirmado sem o evento correspondente.

## Decisão

- Toda gravação de evento usa `Propagation.REQUIRED` e participa da transação do caso de uso.
  Falha ao gravar o evento derruba a transação.
- O envio ao broker continua assíncrono pelo `DomainEventOutboxPublisher` (polling de
  `processed = false`), com `messageId` = id da linha e headers `aggregateType`, `aggregateId`,
  `aggregateVersion`, `eventType`, `occurredAt`.

## Consequências

- Pedido e eventos são confirmados ou desfeitos juntos (coberto por
  `OrderUseCaseEventSourcingTest.failedCreationRollsBackOrderAndItsEventsTogether`).
- A entrega ao RabbitMQ é *at-least-once*: se o envio funcionar e a marcação `processed` falhar,
  ou se duas instâncias rodarem o publisher, a mensagem é reenviada. Consumidores precisam ser
  idempotentes (ADR 0004).
- O publisher não usa *publisher confirms*; uma mensagem aceita pelo cliente mas perdida pelo broker
  é marcada como enviada. Registrado como limitação.
