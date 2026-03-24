package com.example.order.allure.assertion;

import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class AllureSpringAssertAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertEquals(
            @Advice.Argument(0) String message,
            @Advice.Argument(1) Object expected,
            @Advice.Argument(2) Object actual) {
        try {
            Allure.step("Assert: " + message + " = " + actual);
        } catch (Throwable ignored) {
        }
    }
}
