package com.thin.cqrsesorder.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class GeneratorIdUtils {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    public String generate(String prefix) {
        return prefix + LocalDateTime.now().format(dateFormatter) + RandomStringUtils.randomNumeric(4);
    }
}
