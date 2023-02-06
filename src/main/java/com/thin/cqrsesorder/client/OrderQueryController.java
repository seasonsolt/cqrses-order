package com.thin.cqrsesorder.client;

import com.thin.cqrsesorder.bean.request.CreateRequest;
import com.thin.cqrsesorder.bean.view.OrderView;
import com.thin.cqrsesorder.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/order/query")
public class OrderQueryController {

    @Autowired
    EventService eventService;

    @PostMapping()
    public OrderView query(CreateRequest request) {

        return null;
    }

    @PostMapping(value = "/detail")
    public OrderView detail(CreateRequest request) {

        return null;
    }

}

