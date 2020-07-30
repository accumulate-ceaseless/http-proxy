package org.acc.http.proxy.utils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ThrowableUtils {
    public static void message(Class<?> aClass, Throwable cause) {
        log.error("发生异常的类：{}, 异常信息：{}", aClass.getSimpleName(), cause.getMessage());
    }
}
