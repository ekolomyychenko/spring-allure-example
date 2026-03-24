package com.example.order.allure.mock;

import org.mockito.internal.creation.bytebuddy.InlineByteBuddyMockMaker;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;

import java.util.Optional;

public class AllureMockitoMockMaker implements MockMaker {

    private final InlineByteBuddyMockMaker delegate = new InlineByteBuddyMockMaker();

    @Override
    public <T> T createMock(MockCreationSettings<T> settings, MockHandler handler) {
        return delegate.createMock(settings, new AllureMockitoHandler<>(handler));
    }

    @Override
    public <T> Optional<T> createSpy(MockCreationSettings<T> settings, MockHandler handler, T spiedInstance) {
        return delegate.createSpy(settings, new AllureMockitoHandler<>(handler), spiedInstance);
    }

    @Override
    public MockHandler getHandler(Object mock) {
        return delegate.getHandler(mock);
    }

    @Override
    public void resetMock(Object mock, MockHandler newHandler, MockCreationSettings settings) {
        delegate.resetMock(mock, new AllureMockitoHandler<>(newHandler), settings);
    }

    @Override
    public TypeMockability isTypeMockable(Class<?> type) {
        return delegate.isTypeMockable(type);
    }

    @Override
    public void clearAllCaches() {
        delegate.clearAllCaches();
    }
}
