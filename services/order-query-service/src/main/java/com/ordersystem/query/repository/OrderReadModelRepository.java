package com.ordersystem.query.repository;

import com.ordersystem.query.entity.OrderReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderReadModelRepository extends JpaRepository<OrderReadModel, String> {
    
    Page<OrderReadModel> findByCustomerId(String customerId, Pageable pageable);

    Page<OrderReadModel> findByStatus(String status, Pageable pageable);

    Page<OrderReadModel> findByCustomerIdAndStatus(String customerId, String status, Pageable pageable);
}
