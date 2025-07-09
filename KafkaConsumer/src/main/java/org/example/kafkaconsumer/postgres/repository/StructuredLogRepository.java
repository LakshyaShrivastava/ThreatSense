package org.example.kafkaconsumer.postgres.repository;

import org.example.kafkaconsumer.postgres.entity.StructuredNetworkLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface StructuredLogRepository extends ReactiveCrudRepository<StructuredNetworkLog, Long> {
    // Spring Data R2DBC automatically provides methods like save(), findById(), findAll(), delete()
    // You can add custom reactive query methods here if needed
    Flux<StructuredNetworkLog> findBySrcIP(String srcIP);
}
