package com.thin.cqrsesorder.domain;

public interface Snapshot {

    /**
     * Save a memory object to database as snapshot
     */
    void snapshot();
}
