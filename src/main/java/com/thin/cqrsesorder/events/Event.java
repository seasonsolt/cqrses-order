package com.thin.cqrsesorder.events;

import com.thin.cqrsesorder.domain.Order;

/**
 * This project is based on event-driven architecture pattern.
 * Unlike usual event-driven system, we do not need a eventBus that contains a sequent data structure (i.e.
 * BlockingQueue, Disruptor).
 * The sequence of events can be provided by "event time" which is an implementation class of Lamport Clock. (see
 * {@link Order#getEventClock})
 */

public interface Event {
    /**
     * return if the event has benn applied, especially targeting on whether external services is invoked.
     */
    boolean hasApplied();

    /**
     * Thanks to microservice architecture, domain models are separated into different services. To keep the consistence
     * of every domain's lifecycle, each service must provide idempotent APIs to avoid duplicated operation by other
     * service's event replay.
     *
     * Because we can not guarantee all external services proving idempotent APIs, we replace the relay() function with
     * a apply() function that can avoid invoke external service when hasApplied() method return true.
     * if (hasApplied() == false), this function equals a fully functional 'replay()' which should invoke external
     * service that does not exist for the time being.
     *
     * Event Replay is a core concept from Event Sourcing Architecture.
     * Replay() function attaches stateful properties which come from event_logs persisted in database to order object,
     * i.e 'payNo', 'paidAmount', 'deliverCount', 'receiveCount'...
     * then the order domain can be converted into orderView, and then we can take a snapshot on it.
     *
     * Thanks to cqrs architecture, the snapshot can be stored in elastic-search which provide huge throughput.
     */
    void apply(Order order);

    void rollback(Order order);
}

