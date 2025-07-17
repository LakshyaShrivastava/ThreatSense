package org.example.kafkaconsumer.postgres.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import org.example.kafkaconsumer.postgres.entity.Alert;
import reactor.core.publisher.Mono;

@Repository
public interface AlertRepository extends ReactiveCrudRepository<Alert, Long> {
    Mono<Boolean> existsByNetworkLogId(Long networkLogId);
}
