package com.example.kafkaproducer.controller;

import com.example.kafkaproducer.config.KafkaProducerFactory;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Random;

@RestController
@RequestMapping("kafka")
public class KafkaProducerController {

    @GetMapping("/produce")
    private String produce() {

        KafkaTemplate<String, String> producer = KafkaProducerFactory.kafkaTemplate();

        int randomInt = getRandomInt();
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("example-topic", "example-key-" + randomInt, "example-value-" + randomInt);
        producerRecord.headers().add("foo", "bar".getBytes(StandardCharsets.UTF_8));
        producer.send(producerRecord);

        String publishedRecordMessage =
                "Published Kafka Record: " + producerRecord.key() + ", " + producerRecord.value() + " to topic: " + producerRecord.topic() + "\n";
        System.out.println(publishedRecordMessage);
        return publishedRecordMessage;
    }

    private int getRandomInt() {
        return new Random().nextInt(1000);
    }

}
