package com.example.order.allure.assertion;

import com.example.order.allure.AllureInstrumentationLogger;
import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class AllureSpringAssertNotEqualsAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertNotEquals(
            @Advice.Argument(0) String message,
            @Advice.Argument(1) Object unexpected,
            @Advice.Argument(2) Object actual) {
        try {
            Allure.step("Assert: " + message + " unexpected " + unexpected + " != " + actual);
        } catch (Throwable t) {
            AllureInstrumentationLogger.warn("SpringAssertNotEquals", t);
        }
    }
}
