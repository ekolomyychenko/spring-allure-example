package com.example.order.allure.assertion;

import com.example.order.allure.AllureInstrumentationLogger;
import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class AllureSpringAssertTrueAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertTrue(@Advice.Argument(0) String message) {
        try {
            Allure.step("Assert: " + message);
        } catch (Throwable t) {
            AllureInstrumentationLogger.warn("SpringAssertTrue", t);
        }
    }
}
