package com.thin.cqrsesorder.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.proxy.JeePayProxy;
import com.thin.cqrsesorder.utils.SpringContextHolder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties
public class PayEvent extends EventLog {

    private Integer eventStatus = 1;

    private String payNo;

    private BigDecimal payAmount;

    @Transient
    @JsonIgnore
    private JeePayProxy jeePayProxy;

    public PayEvent(String orderId, BigDecimal payAmount) {
        super.setOrderId(orderId);
        jeePayProxy = SpringContextHolder.getBean(JeePayProxy.class);
        this.payAmount = payAmount;
    }

    @Override
    public boolean hasApplied() {
        return StringUtils.isNotBlank(payNo) && super.hasApplied();
    }

    @Override
    public void apply(Order order) {
        if (!hasApplied()) {
            this.payNo = jeePayProxy.pay(order);
        }
        super.apply(order);

        List<String> payNos = ObjectUtils.defaultIfNull(order.getPayNos(), new ArrayList<>(1));
        payNos.add(this.payNo);
        order.setPayNos(payNos);
    }

    @Override
    public void rollback(Order order) {
        assert eventStatus.equals(order.getStatus());
        jeePayProxy.refund(order);
        order.setStatus(0);
    }
}


