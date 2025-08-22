package com.ordersystem.payment.controller;

import com.ordersystem.payment.model.Payment;
import com.ordersystem.payment.service.PaymentService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/payments")
@Validated
@Tag(name = "Payments", description = "API para gerenciamento de pagamentos")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Operation(summary = "Buscar pagamento por ID", description = "Retorna os detalhes de um pagamento específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pagamento encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Serviço temporariamente indisponível",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(
            @Parameter(description = "ID do pagamento", required = true)
            @PathVariable @NotBlank String paymentId) {
        
        String correlationId = MDC.get("correlationId");
        logger.debug("GET /payments/{} - correlationId: {}", paymentId, correlationId);

        Payment payment = paymentService.getPayment(paymentId);
        
        return ResponseEntity.ok(payment);
    }

    @Operation(summary = "Buscar pagamento por ID do pedido", description = "Retorna o pagamento associado a um pedido")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pagamento encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(
            @Parameter(description = "ID do pedido", required = true)
            @PathVariable @NotBlank String orderId) {
        
        String correlationId = MDC.get("correlationId");
        logger.debug("GET /payments/order/{} - correlationId: {}", orderId, correlationId);

        Payment payment = paymentService.getPaymentByOrderId(orderId);
        
        return ResponseEntity.ok(payment);
    }

    @Operation(summary = "Listar todos os pagamentos", description = "Retorna lista de todos os pagamentos")
    @ApiResponse(responseCode = "200", description = "Lista de pagamentos")
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        String correlationId = MDC.get("correlationId");
        logger.debug("GET /payments - correlationId: {}", correlationId);

        List<Payment> payments = paymentService.getAllPayments();
        
        return ResponseEntity.ok(payments);
    }

    @Operation(summary = "Tentar novamente um pagamento", description = "Reprocessa um pagamento que falhou")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Reprocessamento iniciado"),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Pagamento não pode ser reprocessado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<CompletableFuture<Payment>> retryPayment(
            @Parameter(description = "ID do pagamento", required = true)
            @PathVariable @NotBlank String paymentId) {
        
        String correlationId = MDC.get("correlationId");
        logger.info("POST /payments/{}/retry - correlationId: {}", paymentId, correlationId);

        CompletableFuture<Payment> futurePayment = paymentService.retryPayment(paymentId);
        
        return ResponseEntity.accepted().body(futurePayment);
    }

    @Operation(summary = "Cancelar pagamento", description = "Cancela um pagamento pendente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Pagamento cancelado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Pagamento não pode ser cancelado",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> cancelPayment(
            @Parameter(description = "ID do pagamento", required = true)
            @PathVariable @NotBlank String paymentId) {
        
        String correlationId = MDC.get("correlationId");
        logger.info("DELETE /payments/{} - correlationId: {}", paymentId, correlationId);

        paymentService.cancelPayment(paymentId);
        
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Health check", description = "Verifica a saúde do serviço")
    @ApiResponse(responseCode = "200", description = "Serviço funcionando corretamente")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is healthy");
    }
}