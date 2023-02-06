package com.thin.cqrsesorder.infrastructure.distribution;

import com.thin.cqrsesorder.infrastructure.exception.NoInstanceException;

import java.nio.file.WatchEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Order system is a stateless service which does not need any leader to take control of logging metadata among all instances.
 * But, selecting a specific server to handle a particular request is still a necessary part of Consistent Core.
 * @see <a href="https://martinfowler.com/articles/patterns-of-distributed-systems/consistent-core.html">consistent-core</a>
 */
public interface ConsistentCore {
    void register();

    void unregister();

    void watch(String name, Consumer<WatchEvent> watchCallback);

    List<String> getInstances() throws NoInstanceException;
}
