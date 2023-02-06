package com.thin.cqrsesorder.events;

import com.thin.cqrsesorder.constants.OrderStatus;
import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.domain.OrderItem;
import com.thin.cqrsesorder.utils.GeneratorIdUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CreateEvent extends EventLog {
    private List<OrderItem> items;

    private String address;

    private BigDecimal amount;

    public boolean hasApplied() {
        return StringUtils.isNotBlank(getOrderId()) && super.hasApplied();
    }

    public void apply(Order order) {
        order.setItems(items);
        order.setAmount(amount);
        order.setAddress(address);
        order.setCreated(new Date());
        order.setStatus(OrderStatus.INIT.getStateId());
        order.setEventClock(new Order.EventClock(order.getCreated()));

        if (!hasApplied()) {
            order.setId(GeneratorIdUtils.generate("TP"));

            // persist event
            super.apply(order);

            // persist order
            order.persist();
        }
    }

    public void rollback(Order order) {
    }
}
