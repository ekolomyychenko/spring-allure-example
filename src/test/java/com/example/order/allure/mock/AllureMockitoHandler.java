package com.example.order.allure.mock;

import io.qameta.allure.Allure;
import org.mockito.internal.progress.ThreadSafeMockingProgress;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.InvocationContainer;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AllureMockitoHandler<T> implements MockHandler<T> {

    private final MockHandler<T> delegate;

    public AllureMockitoHandler(MockHandler<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object handle(Invocation invocation) throws Throwable {
        // detect phase BEFORE delegate consumes Mockito internal state
        String phase = isObjectMethod(invocation.getMethod())
                ? null
                : detectPhase(invocation.getMock().getClass());

        Object result = delegate.handle(invocation);

        if (phase != null) {
            Allure.step(phase + invocation + " → " + result);
        }

        return result;
    }

    @Override
    public MockCreationSettings<T> getMockSettings() {
        return delegate.getMockSettings();
    }

    @Override
    public InvocationContainer getInvocationContainer() {
        return delegate.getInvocationContainer();
    }

    private boolean isObjectMethod(Method method) {
        return method.getDeclaringClass() == Object.class;
    }

    private String detectPhase(Class<?> mockClass) {
        if (isVerificationMode()) {
            return "Mock verify: ";
        }

        if (isCalledFromProductionCode(mockClass)) {
            return "Mock call: ";
        }

        return "Mock stub: ";
    }

    /**
     * Peek at Mockito's verification mode without consuming it.
     * verify(mock) sets verificationMode on MockingProgress,
     * then .method() triggers handler where we can detect it.
     */
    private boolean isVerificationMode() {
        try {
            Object progress = ThreadSafeMockingProgress.mockingProgress();
            Field field = progress.getClass().getDeclaredField("verificationMode");
            field.setAccessible(true);
            return field.get(progress) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * If stack contains frames from non-test, non-framework code between
     * the test and the mock, the mock was called through production code.
     * <p>
     * With inline ByteBuddy mocking (Mockito 5+), mock proxies use the original
     * class (no $$ subclass), so we must skip the mock class itself explicitly.
     */
    private boolean isCalledFromProductionCode(Class<?> mockClass) {
        String mockClassName = mockClass.getName();
        boolean passedHandler = false;

        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String className = frame.getClassName();

            if (className.equals(AllureMockitoHandler.class.getName())) {
                passedHandler = true;
                continue;
            }
            if (!passedHandler) {
                continue;
            }

            // skip the mocked class itself (inline mocking — no $$ subclass)
            if (className.equals(mockClassName)) {
                continue;
            }

            // skip Mockito/Spring/JDK internals and generated proxies
            if (className.startsWith("org.mockito.")
                    || className.startsWith("org.springframework.")
                    || className.startsWith("java.")
                    || className.startsWith("javax.")
                    || className.startsWith("jakarta.")
                    || className.startsWith("jdk.")
                    || className.startsWith("sun.")
                    || className.startsWith("com.sun.")
                    || className.contains("$$")
                    || className.contains("CGLIB")
                    || className.contains("ByteBuddy")) {
                continue;
            }

            // if we hit the test class, it's a direct test call (stub setup)
            if (className.endsWith("Test") || className.endsWith("Tests")) {
                return false;
            }

            // any other application code → production call
            return true;
        }
        return false;
    }
}
