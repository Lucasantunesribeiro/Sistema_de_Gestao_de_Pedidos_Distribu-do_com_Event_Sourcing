package com.ordersystem.order.controller;

import com.ordersystem.order.service.OrderService;
import com.ordersystem.shared.dto.CreateOrderRequest;
import com.ordersystem.shared.dto.OrderResponse;
import com.ordersystem.shared.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/orders")
@Validated
@Tag(name = "Orders", description = "API para gerenciamento de pedidos")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Operation(summary = "Criar novo pedido", description = "Cria um novo pedido no sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Erro de regra de negócio",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Serviço temporariamente indisponível",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "Dados do pedido a ser criado", required = true)
            @Valid @RequestBody CreateOrderRequest request) {
        
        String correlationId = MDC.get("correlationId");
        logger.info("POST /orders - Creating order for customer {}, correlationId: {}", 
                   request.getCustomerId(), correlationId);

        OrderResponse response = orderService.createOrder(request);
        
        logger.info("Order {} created successfully, correlationId: {}", 
                   response.getOrderId(), correlationId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Buscar pedido por ID", description = "Retorna os detalhes de um pedido específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Serviço temporariamente indisponível",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "ID do pedido", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable @NotBlank String orderId) {
        
        String correlationId = MDC.get("correlationId");
        logger.debug("GET /orders/{} - correlationId: {}", orderId, correlationId);

        OrderResponse response = orderService.getOrder(orderId);
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancelar pedido", description = "Cancela um pedido existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pedido cancelado com sucesso",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Não é possível cancelar o pedido no estado atual",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Serviço temporariamente indisponível",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "ID do pedido", required = true)
            @PathVariable @NotBlank String orderId,
            @Parameter(description = "Motivo do cancelamento")
            @RequestParam(required = false, defaultValue = "Cancelado pelo usuário") String reason) {
        
        String correlationId = MDC.get("correlationId");
        logger.info("DELETE /orders/{} - Cancelling order, reason: {}, correlationId: {}", 
                   orderId, reason, correlationId);

        OrderResponse response = orderService.cancelOrder(orderId, reason);
        
        logger.info("Order {} cancelled successfully, correlationId: {}", orderId, correlationId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Processar reserva de estoque", description = "Inicia o processo de reserva de estoque para um pedido")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Comando de reserva enviado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Serviço temporariamente indisponível",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{orderId}/reserve-inventory")
    public ResponseEntity<Void> reserveInventory(
            @Parameter(description = "ID do pedido", required = true)
            @PathVariable @NotBlank String orderId) {
        
        String correlationId = MDC.get("correlationId");
        logger.info("POST /orders/{}/reserve-inventory - correlationId: {}", orderId, correlationId);

        orderService.processInventoryReservation(orderId);
        
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Health check", description = "Verifica a saúde do serviço")
    @ApiResponse(responseCode = "200", description = "Serviço funcionando corretamente")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is healthy");
    }
}