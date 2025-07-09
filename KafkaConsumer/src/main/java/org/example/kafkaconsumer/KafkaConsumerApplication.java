package org.example.kafkaconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories; // NEW: Enable MongoDB repositories
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories; // Existing: Enable R2DBC repositories
import org.springframework.scheduling.annotation.EnableScheduling; // NEW: Enable scheduled tasks

@SpringBootApplication
@EnableReactiveMongoRepositories(basePackages = "org.example.kafkaconsumer.mongodb.repository") // Scan for MongoDB repositories
@EnableR2dbcRepositories(basePackages = "org.example.kafkaconsumer.postgres.repository") // Scan for PostgreSQL repositories
@EnableScheduling // Enable Spring's @Scheduled annotation
public class KafkaConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaConsumerApplication.class, args);
    }
}
