package com.example.order.allure.assertion;

import com.example.order.allure.AllureInstrumentationLogger;
import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class AllureSpringAssertNotNullAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertNotNull(
            @Advice.Argument(0) String message,
            @Advice.Argument(1) Object actual) {
        try {
            Allure.step("Assert: " + message + " actual " + actual + " is not null");
        } catch (Throwable t) {
            AllureInstrumentationLogger.warn("SpringAssertNotNull", t);
        }
    }
}
