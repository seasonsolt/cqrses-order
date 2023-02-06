package com.thin.cqrsesorder.bean;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PayCallbackDto implements Serializable {

    private String orderId;

    /**
     * 1-pay ,2-refund
     */
    private Byte orderType;

    private String mchOrderNo;

    private BigDecimal amount;
}
