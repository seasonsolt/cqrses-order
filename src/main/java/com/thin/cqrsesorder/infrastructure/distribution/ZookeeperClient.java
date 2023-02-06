package com.thin.cqrsesorder.infrastructure.distribution;

import java.util.List;

public interface ZookeeperClient {

	void create(String path, boolean ephemeral);

	void delete(String path);

	List<String> getChildren(String path);

	boolean isConnected();

	void close();

}
