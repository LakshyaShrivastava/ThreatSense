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

    // For testing Frequent Connections:
    private static final String TEST_FREQ_SRC_IP = "192.168.1.10"; // Specific source IP for high frequency
    private static final String TEST_FREQ_DEST_IP = "10.0.0.5";   // Specific destination IP for high frequency
    private static final int FREQ_TEST_PROBABILITY = 3; // Send test freq log approx. 1 out of 3 times (33%)


    private final Random random = new Random();
    private final ObjectMapper objectMapper; // ObjectMapper is thread-safe after configuration
    private int logSequence = 0;

    // Array of suspicious ports to inject for testing
    private static final int[] SUSPICIOUS_PORTS_ARRAY = {21, 23, 80, 443, 3389, 22, 445};
    private static final String[] BLACKLISTED_IPS_ARRAY = {
            "1.2.3.4", "192.0.2.1", "10.0.0.1", "203.0.113.5", "198.51.100.10"
    };

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
        String srcIP;
        String destIP;
        String message;
        int port;

        // TEMPORARY: forcing some blacklisted IPs for testing
        if (random.nextInt(10) == 0) { // Approx 10% chance for srcIP to be blacklisted
            srcIP = BLACKLISTED_IPS_ARRAY[random.nextInt(BLACKLISTED_IPS_ARRAY.length)];
        } else {
            srcIP = generateRandomIpAddress();
        }

        if (random.nextInt(10) == 0) { // Approx 10% chance for destIP to be blacklisted
            destIP = BLACKLISTED_IPS_ARRAY[random.nextInt(BLACKLISTED_IPS_ARRAY.length)];
        } else {
            destIP = generateRandomIpAddress();
        }
        /*
        if (random.nextInt(FREQ_TEST_PROBABILITY) == 0) { // e.g., 1 out of 3 times
            srcIP = TEST_FREQ_SRC_IP;
            destIP = TEST_FREQ_DEST_IP;
            message = MESSAGES[random.nextInt(3)]; // Use specific messages for test traffic
        }
        else {
            srcIP = generateRandomIpAddress();
            destIP = generateRandomIpAddress();
            message = MESSAGES[random.nextInt(MESSAGES.length)];
        }

         */
        // TEMPORARY: Forcing some suspicious ports for testing (approx. 20% of logs)
        if (random.nextInt(5) == 0) {
            port = SUSPICIOUS_PORTS_ARRAY[random.nextInt(SUSPICIOUS_PORTS_ARRAY.length)];
        } else {
            port = random.nextInt(65535) + 1;
        }

        String protocol = PROTOCOLS[random.nextInt(PROTOCOLS.length)];
        long bytes = random.nextInt(100000) + 1;
        Instant timestamp = Instant.now();
        message = MESSAGES[random.nextInt(MESSAGES.length)];
        String rawLog = "RAW_LOG_ENTRY: " + message + " from " + srcIP + " to " + destIP + " on " + port;
        return new NetworkLog(srcIP, destIP, port, protocol, bytes, timestamp); // add raw_log and message here
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