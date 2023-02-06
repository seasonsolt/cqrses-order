package com.thin.cqrsesorder.infrastructure.distribution;

import com.thin.cqrsesorder.infrastructure.exception.NoInstanceException;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.Inet4Address;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.function.Consumer;

@Getter
@Component
@Slf4j
public class Instance implements ConsistentCore, Comparable<Instance> {

    private final int maxRetries = 3;

    private final String path = "/cqrses-order/instance";

    public static final String SPLIT = "/";

    private final Address address;

    final CuratorZookeeperClient zookeeperClient;

    @SneakyThrows
    public Instance(CuratorZookeeperClient zookeeperClient) {
        this.address = new Address(Inet4Address.getLocalHost().getHostAddress(), 8080);
        this.zookeeperClient = zookeeperClient;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        register();
    }

    @PreDestroy
    public void destroy() {
        unregister();
    }

    @Override public String toString() {
        return address.toString();
    }

    @Override
    public int compareTo(Instance o) {
        return this.address.compareTo(o.getAddress());
    }

    @Override
    public void register() {
        zookeeperClient.create(getUrl(), true);
    }

    @Override
    public void unregister() {
        zookeeperClient.delete(path);
    }

    @Override
    public void watch(String name, Consumer<WatchEvent> watchCallback) {
//        CuratorCacheListener listener = CuratorCacheListener.builder()
//                .forCreates(childData -> log.info("Client Start Up: {}", childData))
//                .forChanges((old, now) -> log.info("Client Changes"))
//                .forDeletes(childData -> log.info("Client Delete"))
//                .build();
    }

    private String getUrl() {
       return path + SPLIT + address;
    }

    @Override
    public List<String> getInstances() throws NoInstanceException {
        List<String> instances =  zookeeperClient.getChildren(path);
        if (CollectionUtils.isEmpty(instances)) {
            throw new NoInstanceException();
        }

        return instances;
    }


}
