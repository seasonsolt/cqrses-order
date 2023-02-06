package com.thin.cqrsesorder.service;

import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.repository.OrderWriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class OrderService {
    @Autowired
    private OrderWriteRepository orderWriteRepository;

    public Order findOrderById(String id) {
        List<Object[]> results = orderWriteRepository.findOrderById(id);
        assert results.size() > 0;

        Order order = (Order)results.get(0)[0];
        order.setEventClock(new Order.EventClock((Date)results.get(0)[1]));

        return order;
    }


}
