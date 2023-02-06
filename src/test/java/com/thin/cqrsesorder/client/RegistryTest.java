package com.thin.cqrsesorder.client;

import com.thin.cqrsesorder.infrastructure.distribution.Instance;
import com.thin.cqrsesorder.infrastructure.exception.NoInstanceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class RegistryTest {
    @Autowired
    Instance service;

    @Test
    public void test_getInstances() throws NoInstanceException {
        List<String> instances = service.getInstances();
        assertTrue(instances.size()==1);
    }
}
