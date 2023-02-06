package com.thin.cqrsesorder;

import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.domain.OrderItem;
import com.thin.cqrsesorder.events.*;
import com.thin.cqrsesorder.projectors.OrderProjector;
import com.thin.cqrsesorder.repository.EventRepository;
import com.thin.cqrsesorder.repository.OrderReadRepository;
import com.thin.cqrsesorder.repository.OrderWriteRepository;
import com.thin.cqrsesorder.service.EventService;
import com.thin.cqrsesorder.bean.view.OrderView;
import org.junit.After;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class CqrsesTest {

    @Autowired
    OrderWriteRepository orderWriteRepository;

    @Autowired
    OrderReadRepository orderReadRepository;

    @Autowired
    OrderProjector orderProjector;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventService eventService;


    @Test
    public void testMain() {
        Set<Event> events = new TreeSet<>();
        Event create = new CreateEvent(Arrays.asList(new OrderItem("iPhone13", new BigDecimal(4999), new BigDecimal(2))), "江苏省南京市雨花台区新华汇A1", new BigDecimal(9998));
        Order order = new Order();
        create.apply(order);
        orderWriteRepository.save(order);

        events.add(new PayEvent(order.getId(), new BigDecimal("5000")));
        events.add(new PayCallbackEvent(order.getId(), order.getPayNos().get(0), new BigDecimal("5000")));
        events.add(new PayEvent(order.getId(), new BigDecimal("3999")));
        events.add(new PayCallbackEvent(order.getId(), order.getPayNos().get(1), new BigDecimal("3999")));

        events.add(new DeliverEvent(order, order.getItems().stream().collect(Collectors.toMap(t -> t.getId(), t -> t.getBuyCount()))));
        events.add(new ReceiveEvent(order, order.getItems().stream().collect(Collectors.toMap(t -> t.getId(), t -> t.getBuyCount()))));
        events.stream().peek(t -> t.apply(order));

        orderProjector.project(order.getId());

        System.out.println(order);
    }

    @Test
    public void test_create() {
        CreateEvent create = new CreateEvent(Arrays.asList(new OrderItem("iPhone13", new BigDecimal(4999), new BigDecimal(2))), "江苏省南京市雨花台区新华汇A1", new BigDecimal(9998));

        eventService.apply(create);
    }

    @Test
    public void test_pay() {
        Event create = new CreateEvent(Arrays.asList(new OrderItem("iPhone13", new BigDecimal(4999), new BigDecimal(2))), "江苏省南京市雨花台区新华汇A1", new BigDecimal(9998));
        Order order = new Order();
        create.apply(order);

        orderWriteRepository.save(order);
        orderReadRepository.save(OrderView.from(order));

        order = orderProjector.project(order.getId());

        PayEvent payEvent = new PayEvent(order.getId(), order.getAmount());
        payEvent.apply(order);
    }

    @Test
    public void test_load_event() {
        String orderId = "TP2209131927355654";
        List<? extends EventLog> eventLogs = orderProjector.loadEvents(orderId);
        assertEquals(eventLogs.get(0).getOrderId(), orderId);
    }

    @Test
    public void test_pay_callback() {
    }

    @Test
    public void test_deliver() {
    }

    @Test
    public void test_receive() {
    }

    @After
    public void after() {
        orderReadRepository.deleteAll();
    }
}
