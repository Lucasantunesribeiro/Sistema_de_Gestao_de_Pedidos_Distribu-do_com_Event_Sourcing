# Requirements Document

## Introduction

O sistema atual de gestão de pedidos está perdendo dados quando a aplicação reinicia no Render, pois utiliza armazenamento em memória. É necessário implementar persistência real no banco de dados PostgreSQL para garantir que os pedidos criados sejam mantidos permanentemente e apareçam na lista mesmo após reinicializações da aplicação.

## Requirements

### Requirement 1

**User Story:** Como um usuário do sistema, eu quero que os pedidos que eu criar sejam salvos permanentemente no banco de dados, para que eles não sejam perdidos quando a aplicação reiniciar.

#### Acceptance Criteria

1. WHEN um pedido é criado via POST /api/orders THEN o sistema SHALL salvar o pedido no banco de dados PostgreSQL
2. WHEN a aplicação reinicia THEN os pedidos previamente criados SHALL permanecer disponíveis no banco
3. WHEN um pedido é salvo no banco THEN o sistema SHALL retornar um ID único e persistente
4. IF o banco de dados não estiver disponível THEN o sistema SHALL retornar erro apropriado ao invés de simular sucesso

### Requirement 2

**User Story:** Como um usuário do sistema, eu quero visualizar todos os pedidos que foram criados anteriormente, para que eu possa acompanhar o histórico completo de pedidos.

#### Acceptance Criteria

1. WHEN eu acesso GET /api/orders THEN o sistema SHALL retornar todos os pedidos salvos no banco de dados
2. WHEN não há pedidos no banco THEN o sistema SHALL retornar uma lista vazia ao invés de dados mock
3. WHEN há pedidos no banco THEN o sistema SHALL retornar os dados reais com todas as informações (ID, cliente, items, total, data)
4. WHEN múltiplos usuários criam pedidos THEN todos os pedidos SHALL aparecer na lista para todos os usuários

### Requirement 3

**User Story:** Como desenvolvedor do sistema, eu quero que as entidades JPA sejam corretamente mapeadas para o banco PostgreSQL, para que a persistência funcione de forma confiável.

#### Acceptance Criteria

1. WHEN a aplicação inicia THEN o sistema SHALL criar automaticamente as tabelas necessárias no PostgreSQL
2. WHEN um pedido é criado THEN o sistema SHALL mapear corretamente todos os campos para as colunas da tabela
3. WHEN há relacionamentos entre entidades THEN o sistema SHALL manter a integridade referencial
4. IF há erro de mapeamento THEN o sistema SHALL logar o erro específico para debugging

### Requirement 4

**User Story:** Como administrador do sistema, eu quero que a transição do armazenamento em memória para banco seja transparente, para que não haja quebra na funcionalidade existente.

#### Acceptance Criteria

1. WHEN a nova implementação é deployada THEN a API SHALL manter a mesma interface (endpoints, formato de request/response)
2. WHEN pedidos são criados THEN o sistema SHALL continuar retornando as mesmas respostas de sucesso
3. WHEN há erro no banco THEN o sistema SHALL retornar códigos de erro apropriados (500 para erro interno, não 201 fake)
4. WHEN a migração é feita THEN o sistema SHALL funcionar imediatamente sem necessidade de configuração adicional

### Requirement 5

**User Story:** Como usuário final, eu quero que o sistema seja confiável e consistente, para que eu possa confiar que meus dados estão sendo salvos corretamente.

#### Acceptance Criteria

1. WHEN eu crio um pedido THEN o sistema SHALL confirmar que foi salvo no banco antes de retornar sucesso
2. WHEN eu recarrego a página THEN os pedidos criados SHALL aparecer imediatamente na lista
3. WHEN há múltiplas operações simultâneas THEN o sistema SHALL manter consistência dos dados
4. IF há falha na operação THEN o sistema SHALL retornar erro claro ao invés de simular sucesso