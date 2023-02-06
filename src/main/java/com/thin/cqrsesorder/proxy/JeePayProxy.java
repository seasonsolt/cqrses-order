package com.thin.cqrsesorder.proxy;

import com.thin.cqrsesorder.domain.Order;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class JeePayProxy {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    public String pay(Order order) {
        //invoke https://pay.jeepay.vip/api/pay/unifiedOrder

        return "P" + LocalDateTime.now().format(dateFormatter) + RandomStringUtils.randomNumeric(4);
    }

    public String refund(Order order) {
        //invoke https://pay.jeepay.vip/api/pay/unifiedOrder

        return "RPP" + LocalDateTime.now().format(dateFormatter) + RandomStringUtils.randomNumeric(4);
    }

}
