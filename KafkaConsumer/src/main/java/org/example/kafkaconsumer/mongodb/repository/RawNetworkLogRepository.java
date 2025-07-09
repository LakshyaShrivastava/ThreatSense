package org.example.kafkaconsumer.mongodb.repository;

import org.example.kafkaconsumer.model.RawNetworkLog;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.time.Instant;

@Repository // Marks this as a Spring Data Repository bean
public interface RawNetworkLogRepository extends ReactiveMongoRepository<RawNetworkLog, String> {
    // can add custom reactive query methods here.
    // Example: Find logs after a certain timestamp (for processing new ones)
    Flux<RawNetworkLog> findByTimestampGreaterThan(Instant timestamp);
}