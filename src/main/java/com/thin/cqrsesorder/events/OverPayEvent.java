package com.thin.cqrsesorder.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.proxy.JeePayProxy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Transient;
import java.math.BigDecimal;

/**
 * 修改订单应收金额
 * 改大了允许用户继续补款
 * 改小了自动触发退款
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties
public class OverPayEvent extends EventLog implements Event {

    private BigDecimal alterPayAmount;

    @Transient
    @JsonIgnore
    @Autowired
    private JeePayProxy jeePayProxy;

    public OverPayEvent(Order order, BigDecimal alterPayAmount) {
        this.alterPayAmount = alterPayAmount;
    }

    @Override
    public boolean hasApplied() {
        return false;
    }

    @Override
    public void apply(Order order) {
        super.apply(order);

        BigDecimal fixedAmount = alterPayAmount.subtract(order.getAmount());
        order.setAmount(alterPayAmount);
        if (fixedAmount.compareTo(BigDecimal.ZERO) < 0) {
            //多退
        }

        order.generateVersionId(this.getId());
    }

    @Override
    public void rollback(Order order) {
        jeePayProxy.refund(order);
        order.setStatus(0);
    }
}


