package org.example.producer;

import org.example.producer.model.NetworkLog;

import com.fasterxml.jackson.databind.ObjectMapper; // JSON Serialization
import com.fasterxml.jackson.databind.SerializationFeature; // for pretty print
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.Properties;
import java.util.Random;

public class Producer {

    private static final String TOPIC_NAME = "log-topic";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092"; // Kafka broker address

    private static final int NUM_SIMULATED_USERS = 5;

    private final Random random = new Random();
    private final ObjectMapper objectMapper; // ObjectMapper is thread-safe after configuration
    private int logSequence = 0;

    private final String[] PROTOCOLS = {"TCP", "UDP", "ICMP", "HTTP", "HTTPS"};
    private final String[] MESSAGES = {"Connection attempt", "Authentication failed",
            "Connection successful", "Authentication successful", "Download Started"};

    public Producer() {
        // Configure ObjectMapper for Instant and pretty printing
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Enables serialization/deserialization of Instant
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // Write dates as ISO 8601 strings
    }

    private NetworkLog generateRandomNetworkLog() {
        String srcIP = generateRandomIpAddress();
        String destIP = generateRandomIpAddress();
        int port = random.nextInt(65535) + 1;
        String protocol = PROTOCOLS[random.nextInt(PROTOCOLS.length)];
        long bytes = random.nextInt(100000) + 1;
        Instant timestamp = Instant.now();

        return new NetworkLog(srcIP, destIP, port, protocol, bytes, timestamp);
    }

    private String generateRandomIpAddress() {
        return random.nextInt(256) + "." +
                random.nextInt(256) + "." +
                random.nextInt(256) + "." +
                random.nextInt(256);
    }

    public void startProducing() {
        // Define Producer Properties
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()); // We'll serialize to String (JSON)

        // Create the KafkaProducer instance
        KafkaProducer<String, String> producer = new KafkaProducer<>(props); // Key and Value are both String

        System.out.println("Starting Kafka Producer...");

        try {
            // Loop indefinitely to keep sending logs
            while (true) {
                NetworkLog log = generateRandomNetworkLog();
                String logKey = "plain-log-seq-" + (logSequence++);

                // Manually serialize NetworkLog object to JSON string
                String jsonLog = objectMapper.writeValueAsString(log);

                // Create a ProducerRecord (topic, key, value)
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, logKey, jsonLog);

                // Send the record asynchronously with a callback
                producer.send(record, (metadata, exception) -> {
                    if (exception == null) {
                        // System.out.println("Message sent: " + metadata.topic() + "-" + metadata.partition() + ":" + metadata.offset());
                    } else {
                        System.err.println("Error sending message: " + exception.getMessage());
                        exception.printStackTrace();
                    }
                });

                System.out.println("Produced: [Key=" + logKey + ", JSON=" + jsonLog + "]");

                Thread.sleep(1000); // Wait for 1 second before sending the next log
            }
        } catch (InterruptedException e) {
            System.out.println("Producer interrupted. Shutting down.");
            Thread.currentThread().interrupt(); // Restore interrupt status
        } catch (Exception e) {
            System.err.println("An error occurred during production: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 3. Flush and Close the Producer
            producer.flush(); // Ensure all buffered records are sent
            producer.close(); // Release resources. CRUCIAL!
            System.out.println("Producer closed.");
        }
    }

    public static void main(String[] args) {
        new Producer().startProducing();
    }
}