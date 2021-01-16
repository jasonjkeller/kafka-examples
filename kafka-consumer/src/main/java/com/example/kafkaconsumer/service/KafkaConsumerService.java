package com.example.kafkaconsumer.service;

import com.example.kafkaconsumer.config.KafkaConsumerFactory;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

public class KafkaConsumerService {

    public static void consume() {
        Consumer<String, String> consumer = KafkaConsumerFactory.consumerFactory().createConsumer();
        consumer.subscribe(Collections.singletonList("example-topic"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                processRecord(record);
            }
        }
    }

    @Trace(dispatcher = true)
    private static void processRecord(ConsumerRecord<String, String> record) {
        NewRelic.setTransactionName("Custom", "KafkaConsumerService/processRecord");

        System.out.println("\nConsuming Kafka Record:");
        System.out.printf("\toffset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
        for (Header header : record.headers()) {
            String headerValueString = new String(header.value(), StandardCharsets.UTF_8);
            System.out.printf("\theader.key = %s, header.value = %s%n", header.key(), headerValueString);

            // TODO use new DT APIs, test W3C headers
            if (header.key().equals("newrelic")) {
                NewRelic.getAgent().getTransaction().acceptDistributedTracePayload(headerValueString);
            }
        }
    }

}
