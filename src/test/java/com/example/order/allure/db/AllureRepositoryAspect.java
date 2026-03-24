package com.example.order.allure.db;

import io.qameta.allure.Allure;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Aspect
public class AllureRepositoryAspect {

    private final Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();

    @Around("execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object logRepositoryCall(ProceedingJoinPoint pjp) throws Throwable {
        String repoName = pjp.getTarget().getClass().getInterfaces()[0].getSimpleName();
        String methodName = pjp.getSignature().getName();
        Object[] args = pjp.getArgs();

        Object result = pjp.proceed();

        if (isCalledFromTest()) {
            String stepName = "DB: " + repoName + "." + methodName;

            Allure.step(stepName, step -> {
                Allure.addAttachment("Request", "text/plain", formatRequest(repoName, methodName, args));
                Allure.addAttachment("Response", "text/plain", formatResponse(result));
            });
        }

        return result;
    }

    private String formatRequest(String repoName, String methodName, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(repoName).append(".").append(methodName);
        if (args.length > 0) {
            sb.append("\n\nArguments:\n");
            for (int i = 0; i < args.length; i++) {
                sb.append("  [").append(i).append("]: ").append(describe(args[i])).append("\n");
            }
        }
        return sb.toString();
    }

    private String formatResponse(Object result) {
        if (result == null) {
            return "void";
        }
        if (result instanceof Optional<?> opt) {
            return opt.map(this::describe).orElse("Optional.empty");
        }
        if (result instanceof Collection<?> col) {
            return "Collection size: " + col.size() + "\n\n"
                    + col.stream().map(this::describe).collect(Collectors.joining("\n"));
        }
        return describe(result);
    }

    private boolean isCalledFromTest() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String className = frame.getClassName();
            if (className.startsWith("com.example.order.service.")
                    || className.startsWith("com.example.order.controller.")) {
                return false;
            }
            if (className.startsWith("com.example.order.") && className.endsWith("Test")) {
                return true;
            }
        }
        return false;
    }

    private String describe(Object obj) {
        if (obj == null) {
            return "null";
        }
        Class<?> clazz = obj.getClass();
        if (clazz.isPrimitive() || obj instanceof Number || obj instanceof String
                || obj instanceof Boolean || obj instanceof Enum) {
            return obj.toString();
        }
        if (clazz.isAnnotationPresent(jakarta.persistence.Entity.class)) {
            return describeEntity(obj, clazz);
        }
        return obj.toString();
    }

    private String describeEntity(Object obj, Class<?> clazz) {
        Field[] fields = fieldCache.computeIfAbsent(clazz, c -> {
            Field[] declared = c.getDeclaredFields();
            for (Field f : declared) {
                f.setAccessible(true);
            }
            return declared;
        });

        StringJoiner sj = new StringJoiner(", ", clazz.getSimpleName() + "{", "}");
        for (Field field : fields) {
            try {
                sj.add(field.getName() + "=" + field.get(obj));
            } catch (IllegalAccessException e) {
                sj.add(field.getName() + "=?");
            }
        }
        return sj.toString();
    }
}
