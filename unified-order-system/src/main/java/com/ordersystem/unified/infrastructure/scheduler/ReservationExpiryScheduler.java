package com.ordersystem.unified.infrastructure.scheduler;

import com.ordersystem.unified.inventory.model.Reservation;
import com.ordersystem.unified.inventory.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled task to automatically release expired inventory reservations.
 * Runs every minute to check for expired reservations.
 *
 * Production-ready with proper logging and error handling.
 * Implements automatic compensation for expired reservations.
 */
@Component
public class ReservationExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private com.ordersystem.unified.inventory.service.ReservationService reservationService;

    /**
     * Runs every minute to release expired reservations.
     * Cron: every minute (0 * * * * *)
     */
    @Scheduled(cron = "0 * * * * *")
    public void releaseExpiredReservations() {
        logger.debug("Starting expired reservation check");

        try {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            List<Reservation> expiredReservations =
                reservationRepository.findReservationsForAutoRelease(now);

            if (expiredReservations.isEmpty()) {
                logger.debug("No expired reservations found");
                return;
            }

            logger.info("Found {} expired reservations to release", expiredReservations.size());

            int successCount = 0;
            int failureCount = 0;

            for (Reservation reservation : expiredReservations) {
                try {
                    reservationService.releaseReservation(reservation);
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to release reservation: {}",
                               reservation.getId(), e);
                    failureCount++;
                    // Continue with other reservations
                }
            }

            logger.info("Released {} expired reservations (success={}, failed={})",
                       expiredReservations.size(), successCount, failureCount);

            // In production, trigger alerts if failure count is high
            if (failureCount > 0) {
                logger.warn("Failed to release {} reservations - manual intervention may be required",
                           failureCount);
            }

        } catch (Exception e) {
            logger.error("Error during expired reservation check", e);
            // In production, trigger alert for monitoring
        }
    }

    /**
     * Manual trigger for testing purposes.
     * In production, this could be exposed as an admin endpoint.
     */
    public void triggerManualCheck() {
        logger.info("Manual expired reservation check triggered");
        releaseExpiredReservations();
    }
}
