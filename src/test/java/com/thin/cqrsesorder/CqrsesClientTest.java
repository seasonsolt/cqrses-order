package com.thin.cqrsesorder;

import com.thin.cqrsesorder.bean.request.CreateRequest;
import com.thin.cqrsesorder.bean.request.PayCallbackRequest;
import com.thin.cqrsesorder.bean.request.PayRequest;
import com.thin.cqrsesorder.bean.view.OrderView;
import com.thin.cqrsesorder.client.OrderController;
import com.thin.cqrsesorder.constants.OrderStatus;
import com.thin.cqrsesorder.constants.PayStatus;
import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.domain.OrderItem;
import com.thin.cqrsesorder.projectors.OrderProjector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CqrsesClientTest {

    @Autowired
    OrderController orderController;

    @Autowired
    OrderProjector orderProjector;

    @Test
    public void test_create() {
        OrderView orderView = create();

        assertNotNull(orderView);
        assertNotNull(orderView.getId());
    }

    @Test
    public void test_pay() {
        OrderView orderView = create();
        pay(orderView.getId(), orderView.getAmount());

        Order order = orderProjector.project(orderView.getId());
        assertTrue(order.getPayNos().size() > 0);
    }


    @Test
    public void test_pay_callback() {
        OrderView orderView = create();
        pay(orderView.getId(), orderView.getAmount());
        Order order = orderProjector.project(orderView.getId());

        payCallback(order.getId(), order.getPayNos().get(0), order.getAmount());

        order = orderProjector.project(orderView.getId());
        assertEquals(order.getStatus(), OrderStatus.AUDIT.getStateId());
        assertEquals(order.getPayStatus(), PayStatus.PAID.getId());
    }

    @Test
    public void test_qeury() {
    }

    @Test
    public void test_deliver() {
    }

    @Test
    public void test_receive() {
    }


    private OrderView create() {
        CreateRequest request = new CreateRequest();
        request.setItems(Arrays.asList(new OrderItem("iPhone13", new BigDecimal(4999), new BigDecimal(2))));
        request.setAddress("江苏省南京市雨花台区新华汇A1");
        request.setAmount(new BigDecimal(9998));

        return orderController.create(null, null, request);
    }

    private OrderView pay(String orderId, BigDecimal amount) {
        PayRequest request = new PayRequest();
        request.setOrderId(orderId);
        request.setPayAmount(amount);

        return orderController.pay(null, request);
    }

    private OrderView payCallback(String orderId, String payNo, BigDecimal amount) {
        PayCallbackRequest request = new PayCallbackRequest();
        request.setOrderId(orderId);
        request.setPayNo(payNo);
        request.setAmount(amount);

        return orderController.payCallback(null, null, request);
    }

}
