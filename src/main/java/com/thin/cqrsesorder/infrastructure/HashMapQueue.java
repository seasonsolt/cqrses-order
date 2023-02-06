package com.thin.cqrsesorder.infrastructure;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 *  A simple container aggregates HashMap and Queue with the goal to providing both random access and FIFO manner
 */
public class HashMapQueue<K, V> implements Serializable {
    private static final long serialVersionUID = 8784037134485131898L;

    private HashMap<K, V> map;

    private Queue<K> queue;

    private int size;

    public HashMapQueue(int size) {
        map = new HashMap<>();
        queue = new LinkedList<>();
        this.size = size;
    }

    public void put(K key, V value) {
        while(map.size() >= size){
            K removeKey = queue.poll();
            map.remove(removeKey);
        }
        if(null == map.get(key)){
            queue.add(key);
        }
        map.put(key, value);
    }

    public V get(K key){
       return map.get(key);
    }


}


