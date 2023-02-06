package com.thin.cqrsesorder.bean.request;

import com.thin.cqrsesorder.events.PayCallbackEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode
public class PayCallbackRequest extends BaseOrderRequest {

    private String payNo;

    private BigDecimal amount;

    public PayCallbackEvent toEvent() {
        return new PayCallbackEvent(orderId, payNo, amount);
    }

    @Override
    public String getRequestId() {
        return String.valueOf(this.hashCode());
    }

}
