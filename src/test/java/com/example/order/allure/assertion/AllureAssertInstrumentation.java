package com.example.order.allure.assertion;

import com.example.order.allure.AllureInstrumentationLogger;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import org.assertj.core.api.AbstractAssert;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AllureAssertInstrumentation {

    public static void install() {
        try {
            Instrumentation instrumentation = ByteBuddyAgent.install();

            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)

                    // AssertJ — blacklist approach: intercept all assertion methods
                    // except configuration/fluent-builder methods
                    .type(isSubTypeOf(AbstractAssert.class))
                    .transform((builder, type, cl, module, pd) -> builder
                            .visit(Advice.to(AllureAssertJAdvice.class)
                                    .on(isPublic()
                                            .and(not(isStatic()))
                                            .and(not(named("as")))
                                            .and(not(named("describedAs")))
                                            .and(not(named("withFailMessage")))
                                            .and(not(named("withRepresentation")))
                                            .and(not(named("overridingErrorMessage")))
                                            .and(not(named("usingComparator")))
                                            .and(not(named("usingElementComparator")))
                                            .and(not(named("usingRecursiveComparison")))
                                            .and(not(named("usingDefaultComparator")))
                                            .and(not(named("withThreadDumpOnError")))
                                            .and(not(named("withAssertionInfo")))
                                            .and(not(named("inHexadecimal")))
                                            .and(not(named("inBinary")))
                                            .and(not(named("extracting")))
                                            .and(not(named("filteredOn")))
                                            .and(not(named("asInstanceOf")))
                                            .and(not(named("asString")))
                                            .and(not(named("asList")))
                                            .and(not(named("newAbstractIterableAssert")))
                                            .and(not(named("getActual")))
                                            .and(not(named("actual")))
                                            .and(not(named("info")))
                                            .and(not(named("myself")))
                                            .and(not(named("objects")))
                                            .and(not(named("throwUnsupportedExceptionOnEquals")))
                                            .and(not(named("hashCode")))
                                            .and(not(named("equals")))
                                            .and(not(named("toString")))
                                            .and(not(named("failWithMessage")))
                                            .and(not(named("failWithActualExpectedAndMessage")))
                                            .and(not(named("isNotNull")))
                                    ))
                    )

                    // Spring AssertionErrors
                    .type(named("org.springframework.test.util.AssertionErrors"))
                    .transform((builder, type, cl, module, pd) -> builder
                            .visit(Advice.to(AllureSpringAssertAdvice.class)
                                    .on(named("assertEquals").and(takesArguments(3))))
                            .visit(Advice.to(AllureSpringAssertNotEqualsAdvice.class)
                                    .on(named("assertNotEquals").and(takesArguments(3))))
                            .visit(Advice.to(AllureSpringAssertTrueAdvice.class)
                                    .on(named("assertTrue").and(takesArguments(2))))
                            .visit(Advice.to(AllureSpringAssertFalseAdvice.class)
                                    .on(named("assertFalse").and(takesArguments(2))))
                            .visit(Advice.to(AllureSpringAssertNullAdvice.class)
                                    .on(named("assertNull").and(takesArguments(2))))
                            .visit(Advice.to(AllureSpringAssertNotNullAdvice.class)
                                    .on(named("assertNotNull").and(takesArguments(2))))
                            .visit(Advice.to(AllureSpringFailAdvice.class)
                                    .on(named("fail").and(takesArguments(1))))
                    )

                    // Hamcrest MatcherAssert — only patch the 3-arg overload.
                    // The 2-arg overload delegates to it internally (assertThat("", actual, matcher)),
                    // so patching both would log every 2-arg call twice.
                    .type(named("org.hamcrest.MatcherAssert"))
                    .transform((builder, type, cl, module, pd) -> builder
                            .visit(Advice.to(AllureHamcrestAdvice.class)
                                    .on(named("assertThat").and(takesArguments(3))))
                    )

                    .installOn(instrumentation);
        } catch (Throwable t) {
            AllureInstrumentationLogger.warn("AssertInstrumentation", t);
        }
    }
}
