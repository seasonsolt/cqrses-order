package com.thin.cqrsesorder.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态
 */

@Getter
@AllArgsConstructor
public enum OrderStatus {
    INIT(0),

    AUDIT(1),

    DELIVERED(2),

    RECEIVED(3),

    DONE(4),

    CANCELED(5);

    private final int stateId;
}
