# Requirements Document

## Introduction

Este documento define os requisitos para consolidar a arquitetura de microserviços distribuídos em uma única URL funcional no Render.com. O objetivo é eliminar serviços redundantes e estabelecer uma arquitetura simplificada que funcione de forma confiável em produção, mantendo todas as funcionalidades do sistema de gestão de pedidos.

## Requirements

### Requirement 1

**User Story:** Como desenvolvedor do sistema, eu quero consolidar todos os serviços em uma única aplicação web no Render, para que eu possa ter uma URL única e funcional sem complexidade de múltiplos serviços.

#### Acceptance Criteria

1. WHEN o sistema é deployado no Render THEN deve existir apenas um web service público acessível
2. WHEN um usuário acessa a URL principal THEN deve receber o frontend funcional com todas as funcionalidades
3. WHEN uma requisição API é feita THEN deve ser processada pelo mesmo serviço web unificado
4. IF múltiplos serviços existem THEN devem ser consolidados em um único processo

### Requirement 2

**User Story:** Como usuário final, eu quero acessar o sistema através de uma única URL, para que eu possa usar todas as funcionalidades sem problemas de conectividade entre serviços.

#### Acceptance Criteria

1. WHEN eu acesso a URL do sistema THEN devo ver o dashboard funcional
2. WHEN eu crio um pedido THEN deve ser processado completamente sem erros
3. WHEN eu visualizo a lista de pedidos THEN deve mostrar dados atualizados
4. IF há problemas de conectividade THEN devem ser eliminados pela arquitetura unificada

### Requirement 3

**User Story:** Como administrador do sistema, eu quero que o Redis seja usado apenas como cache/storage interno, para que eu não precise gerenciar múltiplas conexões entre serviços.

#### Acceptance Criteria

1. WHEN o sistema processa eventos THEN deve usar Redis como storage interno do mesmo processo
2. WHEN há comunicação entre componentes THEN deve ser feita via chamadas diretas de método
3. IF Redis não está disponível THEN o sistema deve funcionar com fallback em memória
4. WHEN o sistema reinicia THEN deve recuperar o estado do Redis se disponível

### Requirement 4

**User Story:** Como desenvolvedor, eu quero eliminar a complexidade de Event Sourcing distribuído, para que eu possa ter um sistema mais simples e confiável.

#### Acceptance Criteria

1. WHEN eventos são processados THEN devem ser tratados de forma síncrona no mesmo processo
2. WHEN há atualizações de estado THEN devem ser refletidas imediatamente
3. IF há falhas de processamento THEN devem ser tratadas localmente sem perda de dados
4. WHEN o sistema escala THEN deve manter a consistência de dados

### Requirement 5

**User Story:** Como operador do sistema, eu quero que o deploy seja simples e confiável, para que eu possa ter confiança na estabilidade da aplicação em produção.

#### Acceptance Criteria

1. WHEN faço deploy no Render THEN deve usar apenas um web service
2. WHEN o sistema inicia THEN todos os componentes devem estar funcionais
3. IF há problemas de inicialização THEN devem ser claramente reportados nos logs
4. WHEN o sistema está rodando THEN deve responder corretamente aos health checks

### Requirement 6

**User Story:** Como desenvolvedor, eu quero manter a funcionalidade completa do sistema, para que nenhuma feature seja perdida na consolidação.

#### Acceptance Criteria

1. WHEN consolido os serviços THEN todas as APIs devem continuar funcionando
2. WHEN um pedido é criado THEN deve seguir todo o fluxo de processamento
3. IF há validações de negócio THEN devem ser mantidas
4. WHEN há atualizações de status THEN devem ser refletidas no frontend