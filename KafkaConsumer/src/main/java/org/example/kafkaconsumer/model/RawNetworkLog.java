package org.example.kafkaconsumer.model;

import org.springframework.data.annotation.Id; // For MongoDB's _id
import org.springframework.data.mongodb.core.mapping.Document; // For collection mapping

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "network_logs") // Maps this class to the 'network_logs' collection in MongoDB
public class RawNetworkLog {
    @Id // Maps to MongoDB's default _id field
    private String id; // MongoDB's _id is typically a String (ObjectId converted to String by driver)

    // These fields directly match the JSON produced by PlainKafkaProducer (Can use annotation tags as well)
    private String srcIP;
    private String destIP;
    private long port;
    private String protocol;
    private long bytes;
    private Instant timestamp;

    private String message;
    private String rawLog;
}