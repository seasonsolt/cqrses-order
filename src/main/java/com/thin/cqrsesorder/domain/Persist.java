package com.thin.cqrsesorder.domain;

public interface Persist {

    /**
     * Save a memory object to database
     */
    void persist();
}
