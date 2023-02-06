package com.thin.cqrsesorder.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态
 */

@Getter
@AllArgsConstructor
public enum PayStatus {

    UNPAID(0),

    PART_PAID(1),

    PAID(2),

    OVER_PAID(3);

    private final int id;
}
