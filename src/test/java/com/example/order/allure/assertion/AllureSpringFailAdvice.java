package com.example.order.allure.assertion;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import net.bytebuddy.asm.Advice;

public class AllureSpringFailAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onFail(@Advice.Argument(0) String message) {
        try {
            Allure.step("Assert fail: " + message, Status.FAILED);
        } catch (Throwable ignored) {
        }
    }
}
