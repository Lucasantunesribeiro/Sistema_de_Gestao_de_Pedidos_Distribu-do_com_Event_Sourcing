package com.ordersystem.unified.config;

import com.ordersystem.unified.auth.model.ApplicationUser;
import com.ordersystem.unified.auth.repository.ApplicationUserRepository;
import com.ordersystem.unified.infrastructure.events.DomainEventEntity;
import com.ordersystem.unified.infrastructure.events.DomainEventRepository;
import com.ordersystem.unified.inventory.model.Product;
import com.ordersystem.unified.inventory.model.Reservation;
import com.ordersystem.unified.inventory.model.ReservationItem;
import com.ordersystem.unified.inventory.model.Stock;
import com.ordersystem.unified.inventory.repository.ProductRepository;
import com.ordersystem.unified.inventory.repository.ReservationItemRepository;
import com.ordersystem.unified.inventory.repository.ReservationRepository;
import com.ordersystem.unified.inventory.repository.StockRepository;
import com.ordersystem.unified.order.model.Order;
import com.ordersystem.unified.order.model.OrderItemEntity;
import com.ordersystem.unified.order.repository.OrderRepository;
import com.ordersystem.unified.payment.model.Payment;
import com.ordersystem.unified.payment.repository.PaymentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Explicit JPA Repository Configuration.
 * 
 * This configuration resolves the "Multiple Spring Data modules found" conflict
 * by explicitly defining which packages contain JPA repositories and entities.
 * This eliminates ambiguity during Spring Boot's component scanning and ensures
 * proper initialization of the application context.
 */
@Configuration
@EnableJpaRepositories(
    basePackageClasses = {
        ApplicationUserRepository.class,
        OrderRepository.class,
        ProductRepository.class,
        StockRepository.class,
        ReservationRepository.class,
        ReservationItemRepository.class,
        PaymentRepository.class,
        DomainEventRepository.class
    }
)
@EntityScan(
    basePackageClasses = {
        ApplicationUser.class,
        Order.class,
        OrderItemEntity.class,
        Product.class,
        Stock.class,
        Reservation.class,
        ReservationItem.class,
        Payment.class,
        DomainEventEntity.class
    }
)
@EnableTransactionManagement
public class JpaRepositoriesConfig {
    
    private static final Logger log = LoggerFactory.getLogger(JpaRepositoriesConfig.class);
    
    /**
     * Explicit JPA configuration to resolve Spring Data module conflicts.
     * 
     * By explicitly defining the repository and entity packages, we eliminate
     * the ambiguity that causes the "strict repository configuration mode"
     * and prevents the application from completing its startup sequence.
     */
    
    @PostConstruct
    public void started() {
        log.info("JpaRepositoriesConfig loaded - JPA-only repositories configured");
    }
}
