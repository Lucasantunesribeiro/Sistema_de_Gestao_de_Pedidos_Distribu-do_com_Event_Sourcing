package com.ordersystem.unified.inventory.service;

import com.ordersystem.unified.infrastructure.events.EventPublisher;
import com.ordersystem.unified.inventory.model.Reservation;
import com.ordersystem.unified.inventory.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository reservationRepository;

    private final EventPublisher eventPublisher;

    public ReservationService(ReservationRepository reservationRepository, EventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseReservation(Reservation reservation) {
        logger.info("Auto-releasing expired reservation: id={}, orderId={}, expiryTime={}",
                   reservation.getId(), reservation.getOrderId(), reservation.getExpiryTime());
        
        Reservation freshReservation = reservationRepository.findById(reservation.getId())
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservation.getId()));
                
        freshReservation.markAsExpired();
        reservationRepository.save(freshReservation);
        
        if (freshReservation.getItems() != null && !freshReservation.getItems().isEmpty()) {
            java.util.List<com.ordersystem.unified.shared.events.OrderItem> items = freshReservation.getItems().stream()
                .map(item -> new com.ordersystem.unified.shared.events.OrderItem(
                    item.getProductId(),
                    "", // product name not available
                    item.getQuantity(),
                    null 
                ))
                .collect(java.util.stream.Collectors.toList());

            com.ordersystem.unified.shared.events.InventoryReleasedEvent event = new com.ordersystem.unified.shared.events.InventoryReleasedEvent(
                freshReservation.getId(),
                freshReservation.getOrderId(),
                items,
                "Automatic release due to expiration",
                freshReservation.getCorrelationId()
            );
            event.setAutoReleased(true);

            eventPublisher.publish(event);
            logger.debug("Inventory released event published for reservation: {}", freshReservation.getId());
        }
    }
}
