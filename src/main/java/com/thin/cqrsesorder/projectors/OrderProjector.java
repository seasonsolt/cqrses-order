package com.thin.cqrsesorder.projectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thin.cqrsesorder.bean.view.OrderView;
import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.repository.EventRepository;
import com.thin.cqrsesorder.repository.OrderReadRepository;
import com.thin.cqrsesorder.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class OrderProjector {


    @Autowired
    OrderService orderService;

    @Autowired
    OrderReadRepository orderReadRepository;

    @Autowired
    EventRepository eventRepository;

    public Order project(String orderId) {
        //create event:return new instance
        if ( Objects.isNull(orderId)) {
            return new Order();
        }

        //other event:do real projection
        Order order = orderService.findOrderById(orderId);
        assert null != order;

        List<? extends EventLog> events = loadEvents(orderId);
        events.forEach(event -> event.apply(order));

        return order;
    }

    public OrderView projectForUpdate(String orderId) {
        Order order = this.project(orderId);

        OrderView orderView = OrderView.from(order);
        orderReadRepository.save(orderView);

        return orderView;
    }

    /**
     * PECS:use '<? extends>' to keep only read
     */
    public List<? extends EventLog> loadEvents(String orderId) {
        List<EventLog> events = eventRepository.findByOrderIdOrderByEventTime(orderId);

        List<? extends EventLog> result = events.stream().map(event -> {
            try {
                Class c = Class.forName(event.getExtendClass());
                EventLog eventExtent = (EventLog)event.getExtendJsonMapper().readValue(event.getExtendJson(), c);
                BeanUtils.copyProperties(event, eventExtent);

                return eventExtent;
            } catch (ClassNotFoundException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return result;
    }
}
