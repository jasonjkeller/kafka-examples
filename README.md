# New Relic Java agent distributing tracing API usage with Kafka  

This project demonstrates usage of the New Relic Java agent distributed tracing APIs. In this example, we use Java agent APIs to propagate 
[W3C Trace Context](https://www.w3.org/TR/trace-context/) headers (transported over Kafka records) between two SpringBoot services resulting in them being 
linked together in a distributed trace.

## Build

Requires Java 8+  

To build all artifacts run the following from the project root:  
`./gradlew clean build`

Artifacts produced:
* `kafka-examples/kafka-producer/build/libs/kafka-producer-0.0.1-SNAPSHOT.jar`
* `kafka-examples/kafka-consumer/build/libs/kafka-consumer-0.0.1-SNAPSHOT.jar`

## Usage

A number of scripts have been provided in the `kafka_2.13-2.6.0/` directory to streamline running Kafka. 

1. Start zookeeper in its own terminal window:  
   `./start-zookeeper.sh`
2. Start the Kafka broker in its own terminal window:  
    `./start-kafka.sh`
3. Create a topic on the Kafka broker in new terminal window (only needs to be done once):  
    `./create-topic.sh`
4. [Run the Kafka producer and consumer services](#run-the-kafka-producer-and-consumer-services)
    * `KafkaProducerApplication`
    * `KafkaConsumerApplication`
5. Publish records to topic on the Kafka broker via one of the following options:  
    * Directly execute the route (publishes a single record each time): http://localhost:8080/kafka/produce
    * Run `./produce-records.sh` in its own terminal window to batch `curl` the route

## Run the Kafka producer and consumer services

### Configure the New Relic Java agent

Each service has its own `newrelic` directory containing the agent jar, agent api jar, and agent config yaml file:
* `/path/to/kafka-examples/kafka-producer/newrelic/`
* `/path/to/kafka-examples/kafka-consumer/newrelic/`

In order to report data to your New Relic account you are required to configure your `license_key` in the `newrelic.yml` or via 
the environment variable `NEW_RELIC_LICENSE_KEY` for each service.

The following config to enable 
[distributed tracing for kafka](https://docs.newrelic.com/docs/agents/java-agent/instrumentation/java-agent-instrument-kafka-message-queues),
 has already been added to the `newrelic.yml` file for each service:
 
```
common: &default_settings

  license_key: 'key'

...
  distributed_tracing:
    enabled: true

  class_transformer:
    kafka-clients-spans:
      enabled: true
...
```

### Run services with the New Relic Java agent

Once each service has a configured license key you can start both of the services as follows:

`kafka-producer` (runs on port `8080`):
* `java -javaagent:/path/to/kafka-examples/kafka-producer/newrelic/newrelic.jar -jar /path/to/kafka-examples/kafka-producer/build/libs/kafka-producer-0.0.1-SNAPSHOT.jar`

`kafka-consumer` (runs on port `8081`):
* `java -javaagent:/path/to/kafka-examples/kafka-consumer/newrelic/newrelic.jar -jar /path/to/kafka-examples/kafka-consumer/build/libs/kafka-consumer-0.0.1-SNAPSHOT.jar`

This will result in two services reporting to New Relic One: `kafka-producer` and `kafka-consumer`

## How it works

### Kafka producer

The Java agent's [Spring instrumentation](https://github.com/newrelic/newrelic-java-agent/tree/main/instrumentation/spring-4.3.0) automatically starts a 
transaction when the `kafka/produce (GET)` controller route in the `kafka-producer` service is executed. This controller creates a Kafka record, injects W3C 
Trace Context headers into the record using the `insertDistributedTraceHeaders(Headers headers)` API, and then publishes the record to a Kafka broker.

Additionally, the agent's [Kafka client instrumentation](https://github.com/newrelic/newrelic-java-agent/tree/main/instrumentation/kafka-clients-spans-0.11.0.0) 
automatically applies to the Kafka producer client and generates a span named `MessageBroker/Kafka/Topic/Produce/Named/example-topic` that is included in the 
distributed trace.

The `kafka-producer` service logs a line similar to the following each time it publishes a Kafka record to the broker:

```
Published Kafka Record: example-key-693, example-value-693 to topic: example-topic
``` 

### Kafka consumer

The `kafka-consumer` service continuously polls the Kafka broker and individually processes each record that is retrieved. When the method that processes each 
record is executed a transaction is started using the Java agent's custom instrumentation APIs. During the processing W3C Trace Context headers are accessed 
from each record and passed to the `acceptDistributedTraceHeaders(TransportType transportType, Headers headers)` API which links the transaction to the 
distributed trace that originated in the `kafka-producer` service.

The `kafka-consumer` service logs a line similar to the following each time it processes a Kafka record:

```
// TODO example with W3C Trace Context headers

Consuming Kafka Record:
	offset = 3132, key = example-key-860, value = example-value-860
	header.key = foo, header.value = bar
	header.key = newrelic, header.value = {"d":{"ac":"2212864","pr":1.2920771,"tx":"dab6d2c0c4375dfd","ti":1610748204711,"ty":"App","tk":"1939595",
"id":"571f1d3479ae6199","tr":"6d157a9b619f8b4ecf39b0a85b747ae6","sa":true,"ap":"1279685854"},"v":[0,1]}
``` 
