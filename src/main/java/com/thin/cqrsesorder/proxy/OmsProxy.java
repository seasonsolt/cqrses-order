package com.thin.cqrsesorder.proxy;

import com.thin.cqrsesorder.domain.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Component
public class OmsProxy {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    public String delivery(Order order) {
        return "DN" + LocalDateTime.now().format(dateFormatter);
    }

}
