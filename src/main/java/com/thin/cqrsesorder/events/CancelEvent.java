package com.thin.cqrsesorder.events;

import com.thin.cqrsesorder.constants.OrderStatus;
import com.thin.cqrsesorder.constants.PayStatus;
import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class CancelEvent extends EventLog {
    private Integer eventStatus = OrderStatus.CANCELED.getStateId();

    private Map<String, BigDecimal> itemReceiveCountMap;

    public CancelEvent(Order order, Map<String, BigDecimal> receiveCountMap) {
        this.itemReceiveCountMap = receiveCountMap;
    }

    @Override
    public boolean hasApplied() {
        return false;
    }

    @Override
    public void apply(Order order) {
        order.getItems().stream().peek(item -> item.setReceiveCount(itemReceiveCountMap.getOrDefault(item.getId(), BigDecimal.ZERO)));

        //已支付的订单签收后自动完成,未支付的(货到付款) 等待收款后自动完成
        if (PayStatus.PAID.getId() == order.getPayStatus()) {
            order.setStatus(OrderStatus.DONE.getStateId());
        } else {
            order.setStatus(eventStatus);
        }
    }

    @Override
    public void rollback(Order order) {
        assert eventStatus.equals(order.getStatus());

        order.getItems().stream().peek(item -> item.setDeliverCount(null));
        order.setStatus(eventStatus - 1);
    }
}


