package com.thin.cqrsesorder.client;

import com.thin.cqrsesorder.bean.request.CreateRequest;
import com.thin.cqrsesorder.bean.request.PayCallbackRequest;
import com.thin.cqrsesorder.bean.request.PayRequest;
import com.thin.cqrsesorder.bean.view.OrderView;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.events.CreateEvent;
import com.thin.cqrsesorder.projectors.OrderProjector;
import com.thin.cqrsesorder.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "/api/order")
public class OrderController {

    @Autowired
    EventService eventService;

    @Autowired
    OrderProjector orderProjector;

    @Autowired
    RestTemplate restTemplate;

    @PostMapping(value = "/create")
    public OrderView create(HttpServletRequest servletRequest, HttpServletResponse servletResponse, @RequestBody CreateRequest request) {
        CreateEvent event = request.toEvent();
        Order order = eventService.apply(event);

        return OrderView.from(order);
    }

    @PostMapping(value = "/pay")
    public OrderView pay(HttpServletRequest servletRequest, @RequestBody PayRequest request) {
        return OrderView.from(eventService.apply(request.toEvent()));
    }

    @PostMapping(value = "/payCallback")
    public OrderView payCallback(HttpServletRequest servletRequest, HttpServletResponse servletResponse, @RequestBody PayCallbackRequest request) {

        return OrderView.from(eventService.apply(request.toEvent()));
    }

    @PostMapping(value = "/cancel")
    public OrderView cancel(CreateRequest request) {

        return null;
    }

    @PostMapping(value = "/deliver")
    public OrderView deliver(CreateRequest request) {

        return null;
    }

    @PostMapping(value = "/receive")
    public OrderView receive(CreateRequest request) {

        return null;
    }

    @PostMapping(value = "/callback")
    public OrderView callback(CreateRequest request) {

        return null;
    }

    @PostMapping(value = "/refund")
    public OrderView refund(CreateRequest request) {

        return null;
    }
}
