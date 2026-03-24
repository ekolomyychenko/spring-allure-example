package com.example.order.config;

import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class HamcrestAssertAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssertThat(
            @Advice.Argument(0) String reason,
            @Advice.Argument(1) Object actual,
            @Advice.Argument(2) Object matcher) {
        try {
            String path = "";
            if (reason != null) {
                int start = reason.indexOf("\"$.");
                if (start >= 0) {
                    int end = reason.indexOf("\"", start + 1);
                    if (end > start) {
                        path = reason.substring(start + 1, end) + " ";
                    }
                }
            }
            Allure.step("Assert: " + path + actual + " " + matcher);
        } catch (Throwable ignored) {
        }
    }
}
