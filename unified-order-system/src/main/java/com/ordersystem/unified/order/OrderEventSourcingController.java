package com.ordersystem.unified.order;

import com.ordersystem.unified.order.application.OrderReplayService;
import com.ordersystem.unified.order.application.OrderReplayService.OrderReplayResponse;
import com.ordersystem.unified.query.projection.OrderProjectionRebuilder;
import com.ordersystem.unified.query.projection.OrderProjectionRebuilder.RebuildResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints administrativos de event sourcing: replay de um pedido e rebuild da projeção.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Event sourcing", description = "Replay do agregado Order e rebuild de projeções")
public class OrderEventSourcingController {

    private final OrderReplayService replayService;
    private final OrderProjectionRebuilder projectionRebuilder;

    public OrderEventSourcingController(OrderReplayService replayService,
                                        OrderProjectionRebuilder projectionRebuilder) {
        this.replayService = replayService;
        this.projectionRebuilder = projectionRebuilder;
    }

    @GetMapping("/orders/{orderId}/replay")
    @Operation(summary = "Reconstrói o pedido a partir do event store e compara com o estado gravado")
    public ResponseEntity<OrderReplayResponse> replay(@PathVariable String orderId) {
        return ResponseEntity.ok(replayService.replay(orderId));
    }

    @PostMapping("/projections/orders/rebuild")
    @Operation(summary = "Reconstrói a projeção order_projection por replay determinístico")
    public ResponseEntity<RebuildResult> rebuildOrderProjection() {
        return ResponseEntity.ok(projectionRebuilder.rebuildAll());
    }
}
