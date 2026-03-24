package com.example.order.config;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public class AllureKafkaInstrumentation {

    public static void install() {
        try {
            Instrumentation instrumentation = ByteBuddyAgent.install();

            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.named("org.apache.kafka.clients.consumer.KafkaConsumer"))
                    .transform((builder, type, cl, module, pd) -> builder
                            .visit(Advice.to(PollAdvice.class)
                                    .on(ElementMatchers.named("poll")
                                            .and(ElementMatchers.takesArgument(0, java.time.Duration.class))))
                    )
                    .installOn(instrumentation);
        } catch (Throwable ignored) {
        }
    }
}
