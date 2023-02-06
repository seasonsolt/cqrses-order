package com.thin.cqrsesorder.bean.request;

import com.thin.cqrsesorder.events.PayEvent;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class PayRequest extends BaseOrderRequest {
    BigDecimal payAmount;

    public PayEvent toEvent() {
        return new PayEvent(orderId, payAmount);
    }
}
