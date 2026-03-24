package com.example.order.allure.mock;

import io.qameta.allure.Allure;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.InvocationContainer;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;

import java.lang.reflect.Method;

public class AllureMockitoHandler<T> implements MockHandler<T> {

    private final MockHandler<T> delegate;

    public AllureMockitoHandler(MockHandler<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object handle(Invocation invocation) throws Throwable {
        Object result = delegate.handle(invocation);

        if (!isObjectMethod(invocation.getMethod())) {
            Allure.step("Mock: " + invocation + " → " + result);
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
}
