package com.thin.cqrsesorder.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.thin.cqrsesorder.events.Event;
import com.thin.cqrsesorder.repository.EventRepository;
import com.thin.cqrsesorder.utils.GeneratorIdUtils;
import com.thin.cqrsesorder.utils.SpringContextHolder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@ToString
@Entity
@Data
@Table(name = "order_event_log")
@Slf4j
public class EventLog implements Event, Persist, Serializable, Comparable<EventLog> {
    @Serial
    private static final long serialVersionUID = -5765106447436150711L;

    @Transient
    @JsonIgnore
    private ObjectMapper extendJsonMapper = new ObjectMapper().setAnnotationIntrospector(new IgnoreSuperClassFieldsIntrospector());

    @Id
    private String id;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "created")
    private Date created;

    @Column(name = "event_time")
    private Date eventTime;

    @Column(name = "extend_class")
    private String extendClass;

    @Column(name = "extend_json")
    private String extendJson;

    @Override
    public boolean hasApplied() {
        return StringUtils.isNotBlank(id);
    }

    /**
     * Save data to event repository
     * SubClasses of EventLog such as PayEvent、DeliverEvent、ReceiveEvent... owns its individual properties.
       Due to RDB's limitation, we use a JsonString struct to persist individual properties which are also
       stateful properties.
         i.e. PayCallbackEvent's extendJson:{"eventStatus":1,"payNo":"P2209161342301276","paidAmount":9998}
     */
    public void persist() {
        EventLog eventLog = new EventLog();
        BeanUtils.copyProperties(this, eventLog);
        eventLog.setExtendClass(this.getClass().getName());
        try {
            eventLog.setExtendJson(getExtendJsonMapper().writeValueAsString(this));
        } catch (JsonProcessingException e) {
            log.error("Event.toEventLog got a JsonProcessingException", e);
            throw new RuntimeException(e);
        }

        EventRepository eventRepository = SpringContextHolder.getBean(EventRepository.class);
        eventRepository.save(eventLog);
    }


    /**
     * Apply Event
     * set EventLog's base properties with state and then auto persist
     */
    @Override
    public void apply(Order order) {
        //the real apply proc
        if (!hasApplied()) {
            Date eventTime = new Date();
            this.id = GeneratorIdUtils.generate("E");
            this.created = eventTime;
            this.eventTime = order.getEventClock().tick(eventTime);
            this.orderId = order.getId();
            persist();
        }

        // replay proc
        order.generateVersionId(this.getId());
    }

    @Override
    public void rollback(Order order) {
        throw new RuntimeException("none implements methods");
    }

    /**
     * SubClasses of EventLog does not need to JsonStringify parent class's base properties
     * use this 'Introspector' to ignore them
     */
    static class IgnoreSuperClassFieldsIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public boolean hasIgnoreMarker(final AnnotatedMember member) {
            return member.getDeclaringClass() == EventLog.class || super.hasIgnoreMarker(member);
        }
    }

    @Override
    public int compareTo(EventLog that) {
        return that.eventTime.compareTo(this.eventTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Event)) return false;

        EventLog that = (EventLog) obj;
        return that.eventTime.compareTo(eventTime) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

}
