package com.thin.cqrsesorder.bean.view;

import com.thin.cqrsesorder.domain.Order;
import com.thin.cqrsesorder.domain.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.springframework.data.elasticsearch.annotations.FieldType.Nested;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "orderview")
public class OrderView implements Serializable {
    @Id
    private String id;

    @Field(type = Nested, includeInParent = true)
    private List<OrderItem> items;

    private String address;

    private BigDecimal amount;

    private BigDecimal paidAmount;

    /**
     * 0:init
     * 1:paid
     * 2:delivered
     * 3:received
     */
    private Integer status;

    /**
     * 0:unpaid
     * 1:part paid
     * 2:paid
     * 3:over paid
     */
    private Integer payStatus;

    private List<String> payNos;

    private List<String> refundNos;

    private Date tickTime;

    public static OrderView from(Order order) {
        OrderView orderView = new OrderView();
        BeanUtils.copyProperties(order, orderView);
        orderView.tickTime = order.getEventClock().getTickTime();

        return orderView;
    }
}
