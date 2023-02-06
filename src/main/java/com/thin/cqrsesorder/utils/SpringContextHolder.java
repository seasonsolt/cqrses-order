package com.thin.cqrsesorder.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * An util that helps acquire IOC container - 'Spring's ApplicationContextAware'
 * A Bean Object could use this util to perform "rich domain model" rather than creating a 'service layer'
 */
@Component
@Lazy(false)
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        setApplicationContextWithStatic(applicationContext);
    }

    private static void  setApplicationContextWithStatic(ApplicationContext applicationContext){
        SpringContextHolder.applicationContext = applicationContext;

    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }


    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException {
        return (T)applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> type) throws BeansException {
        return applicationContext.getBean(type);
    }
}
