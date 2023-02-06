package com.thin.cqrsesorder.domain;

import com.thin.cqrsesorder.utils.GeneratorIdUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "order_item")
public class OrderItem implements Serializable {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "goods_name")
    private String goodsName;

    @Column(name = "goods_price")
    private BigDecimal goodsPrice;

    @Column(name = "buy_count")
    private BigDecimal buyCount;

    @Transient
    private BigDecimal deliverCount;

    @Transient
    private BigDecimal receiveCount;

    public OrderItem(String goodsName, BigDecimal goodsPrice, BigDecimal buyCount) {
        this.id = GeneratorIdUtils.generate("O");
        this.goodsName = goodsName;
        this.goodsPrice = goodsPrice;
        this.buyCount = buyCount;
    }
}
