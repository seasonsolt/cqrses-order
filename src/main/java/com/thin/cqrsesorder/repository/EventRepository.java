package com.thin.cqrsesorder.repository;

import com.thin.cqrsesorder.domain.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface EventRepository extends JpaRepository<EventLog, String> {
    List<EventLog> findByOrderIdOrderByEventTime(String orderId);
}
