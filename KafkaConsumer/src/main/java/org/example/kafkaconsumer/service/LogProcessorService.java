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


@Service
public class LogProcessorService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final StructuredLogRepository structuredLogRepository;

    public LogProcessorService(ReactiveMongoTemplate mongoTemplate,
                               StructuredLogRepository structuredLogRepository) {
        this.mongoTemplate = mongoTemplate;
        this.structuredLogRepository = structuredLogRepository;
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
                            .doOnSuccess(saved -> System.out.println("Saved to PostgreSQL: " + saved.getLogId()));
                })
                .subscribe();
    }
}
