package org.example.kafkaconsumer.service;

import org.example.kafkaconsumer.model.RawNetworkLog;
import org.example.kafkaconsumer.postgres.entity.StructuredNetworkLog;
import org.example.kafkaconsumer.postgres.repository.StructuredLogRepository;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class LogProcessorService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final StructuredLogRepository structuredLogRepository;
    private final AlertingService alertingService;

    public LogProcessorService(ReactiveMongoTemplate mongoTemplate,
                               StructuredLogRepository structuredLogRepository,
                               AlertingService alertingService) {
        this.mongoTemplate = mongoTemplate;
        this.structuredLogRepository = structuredLogRepository;
        this.alertingService = alertingService;
    }

    @PostConstruct
    public void initChangeStreamListener() {
        System.out.println("Starting MongoDB Change Stream listener...");

        // Start a change stream on the collection
        Flux<ChangeStreamEvent<RawNetworkLog>> changeStream = mongoTemplate.changeStream(
                "network_logs_db",              // DB name
                "network_logs",              // Collection name (adjust to your actual name)
                ChangeStreamOptions.builder()
                        .returnFullDocumentOnUpdate()
                        .build(),
                RawNetworkLog.class
        );

        changeStream
                .doOnSubscribe(subscription -> System.out.println("Subscribed to change stream"))
                .doOnError(error -> System.err.println("Error in change stream: " + error))
                .flatMap(event -> {
                    RawNetworkLog rawLog = event.getBody();

                    StructuredNetworkLog structuredLog = new StructuredNetworkLog(
                            rawLog.getSrcIP(),
                            rawLog.getDestIP(),
                            rawLog.getPort(),
                            rawLog.getProtocol(),
                            rawLog.getBytes(),
                            rawLog.getTimestamp(),
                            rawLog.getMessage(),
                            rawLog.getRawLog()
                    );

                    return structuredLogRepository.save(structuredLog)
                            .flatMap(saved -> { // Use flatMap to chain the next reactive operation
                                System.out.println("LogProcessorService: Saved structured log to PostgreSQL: " + saved.getLogId());
                                // Directly call the AlertingService here, passing the logId
                                return alertingService.analyzeAndAlert(saved.getId())
                                        .then(Mono.just(saved)); // Pass the saved log through (or just Mono.empty() if no further chaining needed)
                            })
                            .doOnError(e -> System.err.println("LogProcessorService: Error saving structured log or triggering alert: " + e.getMessage()));
                })
                .subscribe();
    }
}
