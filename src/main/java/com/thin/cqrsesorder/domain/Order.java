package com.thin.cqrsesorder.domain;

import com.thin.cqrsesorder.bean.view.OrderView;
import com.thin.cqrsesorder.constants.OrderStatus;
import com.thin.cqrsesorder.repository.OrderReadRepository;
import com.thin.cqrsesorder.repository.OrderWriteRepository;
import com.thin.cqrsesorder.utils.GeneratorIdUtils;
import com.thin.cqrsesorder.utils.SpringContextHolder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Order.Class is the essential Domain Model of this system
 * Essentially, "order" is a business contract which define the buyer、the seller and the goods they are dealing with.
   In this respect, those properties have already printed/signed on the paper is called "stateless properties",
   because they will never change even the deal is done.
   Therefore, the information that created or changed during deal processing, such as payment、warehouse delivery、
   package tracking is called "stateful properties".
 * We keep stateless properties in an "order_base table", while those stateful properties belonging to Order.Class
   but not existing in order_base table are stored in domain events (event_log). They come back to Order.Class
   again by replaying the Events.
 * Order.Class can be treated as an aggregation that collect stateful properties and stateless properties.
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "order_base")
public class Order implements Persist, Snapshot, Serializable {
    @Id
    @Column(name = "id")
    private String id;

    /**
     * versionId is not just a part of optimistic lock, it represents the identity that all process of this order has applied through.
     * versionId = (versionId + event.id).hashCode();
     */
    @Transient
    private String versionId;

    @JoinColumn(name = "order_id", referencedColumnName = "id")
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items;

    @Column(name = "amount")
    private BigDecimal amount;

    @Transient
    private BigDecimal paidAmount;

    @Column(name = "address")
    private String address;

    @Column(name = "created")
    private Date created;

    /**
     * 0:init
     * 1:paid
     * 2:delivered
     * 3:received
     */
    @Transient
    private Integer status;

    /**
     * 0:unpaid
     * 1:part paid
     * 2:paid
     * 3:over paid
     */
    @Transient
    private Integer payStatus;

    @Transient
    private List<String> payNos;

    @Transient
    private List<String> refundNos;

    /**
     * see class definition
     */
    @Transient
    private EventClock eventClock;

    public Order(List<OrderItem> items, BigDecimal amount, String address) {
        this.id = GeneratorIdUtils.generate("TP");
        this.items = items;
        this.amount = amount;
        this.address = address;
        this.created = new Date();
        this.status = OrderStatus.INIT.getStateId();
        this.eventClock = new EventClock(this.created);
    }

    public void generateVersionId(String eventId) {
        this.versionId = String.valueOf((StringUtils.defaultIfBlank(this.versionId, "") + eventId).hashCode());
    }

    /**
     * Persist order without any stateful property which already annotated with "@Transient"
     * These stateful properties will load by 'Event Replay' when performing a projection.
     */
    @Override
    public void persist() {
        OrderWriteRepository orderWriteRepository = SpringContextHolder.getBean(OrderWriteRepository.class);
        orderWriteRepository.save(this);
    }

    /**
     * snapshot() method will persist the whole properties of order to the read repository, and be retrieved like
        a view in database later.
     * This so called 'read repository' means a database or a search engin with more IO capacity to support massive query
     * from the Internet while the goods is on sale.
     */
    @Override
    public void snapshot() {
        OrderReadRepository orderReadRepository = SpringContextHolder.getBean(OrderReadRepository.class);
        orderReadRepository.save(OrderView.from(this));
    }

    /**
     * EventClock is simple implements of Lamport Clock
     * briefly definition : Lamport Clock use logic time defines event's happens-before rule. To achieve this goal,
       when multi events happened concurrently, we tick the clock (getTime() + 1) manually to void these events have
       same event time.
     * @ref (https://lamport.azurewebsites.net/pubs/time-clocks.pdf)
     */
    @Getter
    @NoArgsConstructor
    public static class EventClock {
        Date latestTime;

        public EventClock(Date created) {
           this.latestTime = created;
        }

        public Date tick(Date requestTime) {
            latestTime = new Date(Long.max(latestTime.getTime(), requestTime.getTime()) + 1);

            return latestTime;
        }

        public Date getTickTime() {
            return new Date(latestTime.getTime() + 1);
        }
    }

}
