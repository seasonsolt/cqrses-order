package com.thin.cqrsesorder.infrastructure.distribution;

import lombok.*;

@Getter
public class Address implements Comparable<Address> {
    private final String host;
    private final int port;

    public static final String SPLIT = ":";

    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override public int hashCode() {
        return toString().hashCode();
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Address address = (Address)obj;
        return host.equals(address.host) && port == address.port;
    }

    @Override public String toString() {
        return host + SPLIT + port;
    }

    @Override public int compareTo(Address o) {
        return this.toString().compareTo(o.toString());
    }
}
