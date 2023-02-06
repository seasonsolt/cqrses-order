package com.thin.cqrsesorder.bean.request;

import com.thin.cqrsesorder.domain.OrderItem;
import com.thin.cqrsesorder.events.CreateEvent;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Data
@ToString
public class CreateRequest extends BaseOrderRequest {
    private List<OrderItem> items;

    private String address;

    private BigDecimal amount;

    public CreateEvent toEvent() {
        return new CreateEvent(items, address, amount);
    }

}
