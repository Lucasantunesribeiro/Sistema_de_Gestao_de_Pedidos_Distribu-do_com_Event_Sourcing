package com.ordersystem.unified.query.projection;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProjectionRepository extends JpaRepository<OrderProjection, String> {

    List<OrderProjection> findAllByOrderByOrderIdAsc();
}
