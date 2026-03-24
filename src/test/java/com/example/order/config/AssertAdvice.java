package com.example.order.config;

import io.qameta.allure.Allure;
import net.bytebuddy.asm.Advice;

public class AssertAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onAssert(
            @Advice.FieldValue("actual") Object actual,
            @Advice.Origin("#m") String methodName,
            @Advice.AllArguments Object[] args) {
        try {
            StringBuilder sb = new StringBuilder("Assert: ");
            sb.append(actual);
            sb.append(" ").append(methodName);
            if (args != null && args.length > 0) {
                sb.append(" ").append(args[0]);
            }
            Allure.step(sb.toString());
        } catch (Throwable ignored) {
        }
    }
}
