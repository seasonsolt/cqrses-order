package com.thin.cqrsesorder.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thin.cqrsesorder.constants.OrderStatus;
import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.proxy.OmsProxy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.Map;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DeliverEvent extends EventLog implements Event {
    private Integer eventStatus = OrderStatus.DELIVERED.getStateId();

    private String deliverNo;

    private Map<String, BigDecimal> itemDeliverCountMap;

    @Transient
    @JsonIgnore
    @Autowired
    private OmsProxy omsProxy;

    public DeliverEvent(Order order, Map<String, BigDecimal> itemDeliverCountMap) {
        this.itemDeliverCountMap = itemDeliverCountMap;
    }

    @Override
    public boolean hasApplied() {
        return StringUtils.isNotBlank(deliverNo);
    }


    public void apply(Order order) {
        super.apply(order);
        if (!hasApplied()) {
            this.deliverNo = omsProxy.delivery(order);
        }

        order.getItems().stream().peek(item -> item.setDeliverCount(itemDeliverCountMap.getOrDefault(item.getId(), BigDecimal.ZERO)));
        order.setStatus(eventStatus);
    }

    @Override
    public void rollback(Order order) {
        order.getItems().stream().peek(item -> item.setDeliverCount(null));
        order.setStatus(eventStatus - 1);
    }

}


