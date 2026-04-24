package com.example.order.allure.assertion;

import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class AllureSpringAssertNullAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertNull(
            @Advice.Argument(0) String message,
            @Advice.Argument(1) Object actual) {
        try {
            Allure.step("Assert: " + message + " actual " + actual + " is null");
        } catch (Throwable ignored) {
        }
    }
}
