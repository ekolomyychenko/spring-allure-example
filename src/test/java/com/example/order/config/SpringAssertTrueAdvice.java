package com.example.order.config;

import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class SpringAssertTrueAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertTrue(@Advice.Argument(0) String message) {
        try {
            Allure.step("Assert: " + message);
        } catch (Throwable ignored) {
        }
    }
}
