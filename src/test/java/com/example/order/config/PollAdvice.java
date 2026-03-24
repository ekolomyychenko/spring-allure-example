package com.example.order.config;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import net.bytebuddy.asm.Advice;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PollAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onPoll(@Advice.Return ConsumerRecords<?, ?> records) {
        try {
            if (records == null || records.isEmpty()) {
                return;
            }

            int count = records.count();
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (ConsumerRecord<?, ?> record : records) {
                if (i > 0) {
                    sb.append("\n---\n");
                }
                sb.append("Topic: ").append(record.topic()).append("\n");
                sb.append("Partition: ").append(record.partition()).append("\n");
                sb.append("Offset: ").append(record.offset()).append("\n");
                sb.append("Key: ").append(record.key()).append("\n");
                sb.append("Value: ").append(record.value());
                i++;
            }

            AllureLifecycle lifecycle = Allure.getLifecycle();
            String stepId = UUID.randomUUID().toString();
            lifecycle.startStep(stepId, new StepResult()
                    .setName("Kafka poll → " + count + " record(s)")
                    .setStatus(Status.PASSED));
            Allure.addAttachment("Records", "text/plain", sb.toString());
            lifecycle.stopStep(stepId);
        } catch (Throwable ignored) {
        }
    }
}
