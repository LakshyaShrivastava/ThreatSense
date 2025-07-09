package org.example.kafkaconsumer.service; // New package (or adjust to existing service package)

import org.example.kafkaconsumer.model.RawNetworkLog; // Raw log from MongoDB
import org.example.kafkaconsumer.mongodb.repository.RawNetworkLogRepository; // MongoDB Repository
import org.example.kafkaconsumer.postgres.entity.StructuredNetworkLog; // Structured log for PostgreSQL
import org.example.kafkaconsumer.postgres.repository.StructuredLogRepository; // PostgreSQL Repository

import org.springframework.scheduling.annotation.Scheduled; // For periodic processing
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class LogProcessorService {

    private final RawNetworkLogRepository rawLogRepository; // Repository for MongoDB
    private final StructuredLogRepository structuredLogRepository; // Repository for PostgreSQL

    // To keep track of the last processed timestamp (for fetching new logs)
    private final AtomicReference<Instant> lastProcessedTimestamp = new AtomicReference<>(Instant.EPOCH); // Start from beginning of time

    // Constructor Injection
    public LogProcessorService(RawNetworkLogRepository rawLogRepository, StructuredLogRepository structuredLogRepository) {
        this.rawLogRepository = rawLogRepository;
        this.structuredLogRepository = structuredLogRepository;
    }

    // This method will run periodically to fetch and process new logs from MongoDB
    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    public void processNewRawLogs() {
        Instant currentTimestamp = Instant.now();
        Instant fetchFrom = lastProcessedTimestamp.get(); // Get the last timestamp we processed

        System.out.println("Processing new raw logs from MongoDB (since " + fetchFrom + ")");

        rawLogRepository.findAll() // Fetch logs newer than last processed timestamp
                .doOnSubscribe(subscription -> System.out.println("DEBUG: Stream subscribed."))
                .doOnError(e -> System.err.println("DEBUG: An error occurred in the stream: " + e.getMessage()))
                .doOnNext(rawLog -> System.out.println("DEBUG: Fetched rawNetworkLog from MongoDB: " + rawLog))
                .flatMap(rawLog -> {
                    StructuredNetworkLog structuredLog = new StructuredNetworkLog(
                            rawLog.getSrcIP(),
                            rawLog.getDestIP(),
                            rawLog.getPort(),
                            rawLog.getProtocol(),
                            rawLog.getBytes(),
                            rawLog.getTimestamp(),
                            "Message placeholder (raw.message removed for debug)",
                            "Raw log placeholder (raw.rawLog removed for debug)"
                    );

                    // Save the structured log to PostgreSQL
                    return structuredLogRepository.save(structuredLog)
                            .doOnSuccess(savedStructuredLog -> System.out.println("Saved structured log to PostgreSQL: " + savedStructuredLog.getLogId()))
                            .doOnError(e -> System.err.println("Error saving structured log to PostgreSQL: " + e.getMessage()));
                })
                .doOnComplete(() -> {
                    // Update the last processed timestamp only if processing was complete
                    // lastProcessedTimestamp.set(currentTimestamp);
                    // System.out.println("Finished processing raw logs up to: " + currentTimestamp);
                    System.out.println("DEBUG: Stream completed.");
                })
                // .doOnError(e -> System.err.println("Error processing raw logs from MongoDB: " + e.getMessage()))
                .subscribe(); // Subscribe to trigger the reactive flow
    }
}