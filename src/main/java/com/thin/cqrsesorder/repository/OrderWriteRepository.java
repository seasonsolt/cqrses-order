package com.thin.cqrsesorder.repository;

import com.thin.cqrsesorder.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderWriteRepository extends JpaRepository<Order, String> {
    @Query("SELECT o, max(e.eventTime) as latestTime  FROM Order o, EventLog e WHERE o.id = e.orderId and o.id = ?1 group by o")
    List<Object[]> findOrderById(String id);

}
