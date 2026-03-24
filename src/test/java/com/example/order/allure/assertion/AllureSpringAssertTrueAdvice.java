package com.example.order.allure.assertion;

import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class AllureSpringAssertTrueAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertTrue(@Advice.Argument(0) String message) {
        try {
            Allure.step("Assert: " + message);
        } catch (Throwable ignored) {
        }
    }
}
