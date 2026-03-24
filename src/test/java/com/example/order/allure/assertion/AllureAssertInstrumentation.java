package com.example.order.allure.assertion;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.assertj.core.api.AbstractAssert;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AllureAssertInstrumentation {

    public static void install() {
        try {
            Instrumentation instrumentation = ByteBuddyAgent.install();

            // AssertJ assertions
            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(isSubTypeOf(AbstractAssert.class))
                    .transform((builder, type, cl, module, pd) -> builder
                            .visit(Advice.to(AllureAssertJAdvice.class)
                                    .on(named("isEqualTo")
                                            .or(named("isNotEqualTo"))
                                            .or(named("hasSize"))
                                            .or(named("contains"))
                                            .or(named("isGreaterThan"))
                                            .or(named("isGreaterThanOrEqualTo"))
                                            .or(named("isLessThan"))
                                            .or(named("isLessThanOrEqualTo"))
                                            .or(named("isNull"))
                                            .or(named("isNotNull"))
                                            .or(named("isTrue"))
                                            .or(named("isFalse"))
                                            .or(named("isEmpty"))
                                            .or(named("isNotEmpty"))
                                    ))
                    )
                    .installOn(instrumentation);

            // Spring MockMvc assertions (AssertionErrors.assertEquals / assertTrue)
            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(named("org.springframework.test.util.AssertionErrors"))
                    .transform((builder, type, cl, module, pd) -> builder
                            .visit(Advice.to(AllureSpringAssertAdvice.class)
                                    .on(named("assertEquals")
                                            .and(takesArguments(3))))
                            .visit(Advice.to(AllureSpringAssertTrueAdvice.class)
                                    .on(named("assertTrue")
                                            .and(takesArguments(2))))
                    )
                    .installOn(instrumentation);
            // Hamcrest MatcherAssert.assertThat(String reason, Object actual, Matcher matcher)
            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(named("org.hamcrest.MatcherAssert"))
                    .transform((builder, type, cl, module, pd) -> builder
                            .visit(Advice.to(AllureHamcrestAdvice.class)
                                    .on(named("assertThat")
                                            .and(takesArguments(3))))
                    )
                    .installOn(instrumentation);
        } catch (Throwable ignored) {
        }
    }
}
