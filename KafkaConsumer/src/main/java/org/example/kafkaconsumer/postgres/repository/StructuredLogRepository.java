package org.example.kafkaconsumer.postgres.repository;

import org.example.kafkaconsumer.postgres.entity.FrequentConnectionAlertDTO;
import org.example.kafkaconsumer.postgres.entity.StructuredNetworkLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Repository
public interface StructuredLogRepository extends ReactiveCrudRepository<StructuredNetworkLog, Long> {
    // Spring Data R2DBC automatically provides methods like save(), findById(), findAll(), delete()
    Flux<StructuredNetworkLog> findByTimestampGreaterThan(Instant timestamp);

    @Query("SELECT " +
            "s.src_ip AS srcIP, " +
            "s.dest_ip AS destIP, " +
            "COUNT(s.id) AS connectionCount, " +
            "MAX(s.timestamp) AS latestLogTimestamp, " +
            "MAX(s.id) AS latestLogId " + // Get the ID of the most recent log
            "FROM network_logs s " +
            "WHERE s.timestamp > :fromTime AND s.timestamp <= :toTime " + // Filter by time window
            "GROUP BY  s.src_ip, s.dest_ip " +
            "HAVING COUNT(s.id) > :minConnections") // Filter by minimum connection count
    Flux<FrequentConnectionAlertDTO> findFrequentConnectionsInWindow(Instant fromTime, Instant toTime, Long minConnections);
}
