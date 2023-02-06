package com.thin.cqrsesorder.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Just make 'jackson' easy to use.
 */
public class JsonUtil {

    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false);
    }

    public static <T> T readValue(String content, Class<T> valueType) {

        try {
            return mapper.readValue(content, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(String content, JavaType valueType) {
        try {
            return mapper.readValue(content, valueType);
        } catch (Exception e) {
            throw new RuntimeException(content + "json string cannot parse to " + valueType.toString() + " type");
        }
    }

    public static String writeValueAsString(Object value) {
        if (Objects.isNull(value)) {
            return "";
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    public static String writeChildValueAsString(Object value) {
        if (Objects.isNull(value)) {
            return "";
        }
        try {
            return mapper.writerWithView(value.getClass()).writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
