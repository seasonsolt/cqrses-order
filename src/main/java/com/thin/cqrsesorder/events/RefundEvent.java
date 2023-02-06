package com.thin.cqrsesorder.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thin.cqrsesorder.domain.EventLog;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.proxy.JeePayProxy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties
public class RefundEvent extends EventLog {

    private Integer eventStatus = 1;

    private String refundNo;

    private BigDecimal refundAmount;

    @Transient
    @JsonIgnore
    @Autowired
    private JeePayProxy jeePayProxy;

    public RefundEvent(Order order, BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    @Override
    public boolean hasApplied() {
        return StringUtils.isNotBlank(refundNo);
    }

    @Override
    public void apply(Order order) {
        super.apply(order);

        if (!hasApplied()) {
            this.refundNo = jeePayProxy.refund(order);
        }

        List<String> refundNos = ObjectUtils.defaultIfNull(order.getRefundNos(), new ArrayList<>(1));
        refundNos.add(this.refundNo);
        order.setPayNos(refundNos);

        order.generateVersionId(this.getId());
    }

    @Override
    public void rollback(Order order) {
        assert eventStatus.equals(order.getStatus());
        jeePayProxy.refund(order);
        order.setStatus(0);
    }
}


