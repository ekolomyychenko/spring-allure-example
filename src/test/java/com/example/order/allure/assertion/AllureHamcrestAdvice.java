package com.example.order.allure.assertion;

import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class AllureHamcrestAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertThat(
            @Advice.Argument(0) String reason,
            @Advice.Argument(1) Object actual,
            @Advice.Argument(2) Object matcher) {
        try {
            String label = "";
            if (reason != null && !reason.isEmpty()) {
                // попробуем извлечь jsonPath из reason (формат MockMvc jsonPath)
                int start = reason.indexOf("\"$.");
                if (start >= 0) {
                    int end = reason.indexOf("\"", start + 1);
                    if (end > start) {
                        label = reason.substring(start + 1, end) + " ";
                    }
                } else {
                    label = reason + ": ";
                }
            }
            Allure.step("Assert: " + label + actual + " " + matcher);
        } catch (Throwable ignored) {
        }
    }
}
