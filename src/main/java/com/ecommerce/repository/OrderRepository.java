package com.ecommerce.repository;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByOrderStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status AND o.createdAt < :since")
    List<Order> findStaleOrders(@Param("status") OrderStatus status,
                                @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.orderStatus = :status")
    long countByUserAndStatus(@Param("userId") Long userId,
                              @Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE " +
           "o.orderStatus = :status AND o.createdAt BETWEEN :start AND :end")
    Double getTotalRevenueBetween(@Param("status") OrderStatus status,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);
}
