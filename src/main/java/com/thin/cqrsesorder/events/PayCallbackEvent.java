package com.thin.cqrsesorder.events;

import com.thin.cqrsesorder.constants.OrderStatus;
import com.thin.cqrsesorder.constants.PayStatus;
import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PayCallbackEvent extends EventLog implements Event {
    private Integer eventStatus = 1;

    private String payNo;

    private BigDecimal paidAmount;

    public PayCallbackEvent(String orderId, String payNo, BigDecimal amount) {
        super.setOrderId(orderId);
        this.payNo = payNo;
        this.paidAmount = amount;
    }

    @Override
    public boolean hasApplied() {
        return super.hasApplied();
    }

    public void apply(Order order) {
        super.apply(order);

        order.setPaidAmount(ObjectUtils.defaultIfNull(order.getPaidAmount(), BigDecimal.ZERO).add(this.paidAmount));
        switch (order.getAmount().compareTo(order.getPaidAmount())) {
            case 1 -> // part paid
                    order.setPayStatus(PayStatus.PART_PAID.getId());
            case -1 -> // over paid
                    order.setPayStatus(PayStatus.OVER_PAID.getId());
            case 0 -> {
                order.setPayStatus(PayStatus.PAID.getId());
                // 已收货的订单，收款后自动完成
                if (OrderStatus.RECEIVED.getStateId() == order.getStatus()) {
                    order.setStatus(OrderStatus.DONE.getStateId());
                }
                // 未审核的订单，收款后自动审核
                if (OrderStatus.INIT.getStateId() == order.getStatus()) {
                    order.setStatus(OrderStatus.AUDIT.getStateId());
                }
            }
        }
    }

    @Override
    public void rollback(Order order) {
    }
}


