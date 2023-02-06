package com.thin.cqrsesorder.service;

import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.projectors.OrderProjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventService {

    @Autowired
    private OrderProjector orderProjector;

    public Order apply(EventLog event) {
        // read repository is not reliable，use event replay to get the latest state of order，may depends on a switch later
        Order order;
        synchronized (event.getOrderId().intern()) {
            // load
            order = orderProjector.project(event.getOrderId());

            // check
            check(order, event);

            // apply the event
            event.apply(order);

            // snapshot order with states to the read repository
            order.snapshot();

            return order;
        }
    }

    private void check(Order order, EventLog event) {
        if (order.getEventClock().getLatestTime().compareTo(event.getEventTime()) > 0) {
            throw new RuntimeException("bad request timestamp error");
        }
    }

}
