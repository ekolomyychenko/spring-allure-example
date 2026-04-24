package com.example.order.allure.assertion;

import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class AllureHamcrestSimpleAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertThat(
            @Advice.Argument(0) Object actual,
            @Advice.Argument(1) Object matcher) {
        try {
            Allure.step("Assert: " + actual + " " + matcher);
        } catch (Throwable ignored) {
        }
    }
}
