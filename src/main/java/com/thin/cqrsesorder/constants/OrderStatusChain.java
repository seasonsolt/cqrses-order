package com.thin.cqrsesorder.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 订单状态链
 * 目前仅支持单一状态链(StateChain)，不引入状态机(StateMachine)
 *  在线支付: INIT->PAID->DELIVERED->RECEIVED->DONE
 * 有多个状态链时，实质上已拓展为一个UAG(有向不循环图)，对于状态机语言即："有限状态机"
 */

@Getter
@AllArgsConstructor
public enum OrderStatusChain {
    OLP(Arrays.asList(OrderStatus.INIT, OrderStatus.AUDIT, OrderStatus.DELIVERED, OrderStatus.RECEIVED, OrderStatus.DONE));

    private final List<OrderStatus> chain;
}
