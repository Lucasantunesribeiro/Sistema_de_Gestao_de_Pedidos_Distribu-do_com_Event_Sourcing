package com.ordersystem.query.repository;

import com.ordersystem.query.entity.OrderReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderReadModelRepository extends JpaRepository<OrderReadModel, String> {
    
    List<OrderReadModel> findByCustomerId(String customerId);
    
    List<OrderReadModel> findByStatus(String status);
    
    @Query("SELECT o FROM OrderReadModel o WHERE o.customerId = :customerId AND o.status = :status")
    List<OrderReadModel> findByCustomerIdAndStatus(@Param("customerId") String customerId, @Param("status") String status);
    
    @Query("SELECT o FROM OrderReadModel o ORDER BY o.createdAt DESC")
    List<OrderReadModel> findAllOrderByCreatedAtDesc();
}