package org.example.kafkaconsumer.service;


import org.example.kafkaconsumer.postgres.entity.Alert;
import org.example.kafkaconsumer.postgres.repository.AlertRepository;
import org.example.kafkaconsumer.postgres.repository.StructuredLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Instant;
import java.time.Duration;

@Service
public class AlertingService {

    private final StructuredLogRepository structuredLogRepository;
    private final AlertRepository alertRepository;

    private static final List<Long> SUSPICIOUS_PORTS = Arrays.asList(23L, 3389L, 22L, 445L);
    private static final List<String> BLACKLISTED_IPS = Arrays.asList( // random IPs
            "1.2.3.4",
            "192.0.2.1",
            "10.0.0.1"     // internal IP that should NOT be external source/dest
    );

    // Can add Timestamp trackers for each rule, for independent tracking later
    private final AtomicReference<Instant> lastProcessedTimestampForAlerts = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<Instant> lastCheckedFrequentConnectionWindow = new AtomicReference<>(Instant.EPOCH);

    private static final Duration FREQUENCY_WINDOW = Duration.ofSeconds(60); // Check for connections in the last 60 seconds
    private static final long MIN_CONNECTIONS_FOR_ALERT = 5;

    public AlertingService(StructuredLogRepository structuredLogRepository, AlertRepository alertRepository) {
        this.structuredLogRepository = structuredLogRepository;
        this.alertRepository = alertRepository;
    }

    // Existing method for Suspicious Ports
    @Scheduled(fixedRate = 10000) // Check every 10 seconds
    @Async
    public void checkForSuspiciousPortActivity() {
        Instant currentRunTimestamp = Instant.now();
        Instant fetchFrom = lastProcessedTimestampForAlerts.get(); // Use the shared timestamp tracker

        System.out.println("Alerting Service: Checking for suspicious ports in logs since " + fetchFrom);

        structuredLogRepository.findByTimestampGreaterThan(fetchFrom)
                .doOnNext(log -> System.out.println("DEBUG Alerting Service (Ports): Fetched log " + log.getLogId() + " Port: " + log.getPort()))
                .filter(log -> SUSPICIOUS_PORTS.contains(log.getPort()))
                .doOnNext(log -> System.out.println("DEBUG Alerting Service (Ports): Log PASSED filter - Suspicious Port: " + log.getPort() + " Log ID: " + log.getLogId()))
                .flatMap(log -> {
                    Alert alert = new Alert(
                            log.getId(),
                            "Suspicious Port Activity",
                            "HIGH",
                            "Detected connection to suspicious port: " + log.getPort() +
                                    " from " + log.getSrcIP() + " to " + log.getDestIP() +
                                    ". Protocol: " + log.getProtocol()
                    );
                    return createAndSaveAlertIfNotDuplicate(alert, log.getId()); // Use helper method
                })
                .doOnComplete(() -> {
                    // add in a seperate time so there's no race condition
                })
                .doOnError(e -> System.err.println("CRITICAL ERROR in Suspicious Port Alerting stream: " + e.getMessage()))
                .then()
                .block();

    }

    @Scheduled(fixedRate = 10000) // Check every 10 seconds
    @Async
    public void checkForBlacklistedIpActivity() {
        Instant currentRunTimestamp = Instant.now();
        Instant fetchFrom = lastProcessedTimestampForAlerts.get(); // Use the shared timestamp tracker

        System.out.println("Alerting Service: Checking for blacklisted IPs in logs since " + fetchFrom);

        structuredLogRepository.findByTimestampGreaterThan(fetchFrom)
                .doOnNext(log -> System.out.println("DEBUG Alerting Service (Blacklist): Fetched log " + log.getLogId() + " SrcIP: " + log.getSrcIP() + " DestIP: " + log.getDestIP()))
                .filter(log -> BLACKLISTED_IPS.contains(log.getSrcIP()) || BLACKLISTED_IPS.contains(log.getDestIP()))
                .doOnNext(log -> System.out.println("DEBUG Alerting Service (Blacklist): Log PASSED filter - IP: " + log.getSrcIP() + " or " + log.getDestIP() + " Log ID: " + log.getLogId()))
                .flatMap(log -> {
                    String detectedIp = BLACKLISTED_IPS.contains(log.getSrcIP()) ? log.getSrcIP() : log.getDestIP();
                    String alertMessage = "Detected connection involving blacklisted IP: " + detectedIp +
                            " (Source: " + log.getSrcIP() + ", Dest: " + log.getDestIP() + ")";

                    Alert alert = new Alert(
                            log.getId(),
                            "Blacklisted IP Activity",
                            "CRITICAL",
                            alertMessage
                    );
                    return createAndSaveAlertIfNotDuplicate(alert, log.getId()); // Use helper method
                })
                .doOnComplete(() -> {
                    // maybe flawed? check later
                    lastProcessedTimestampForAlerts.set(currentRunTimestamp); // Update timestamp for this run
                    System.out.println("Alerting Service: Finished checking blacklisted IPs up to " + currentRunTimestamp);
                })
                .doOnError(e -> System.err.println("CRITICAL ERROR in Blacklisted IP Alerting stream: " + e.getMessage()))
                .then()
                .block();
    }


    @Scheduled(fixedRate = 10000)
    @Async
    public void checkForFrequentConnections() {
        Instant currentRunEndTime = Instant.now();
        Instant windowStartTime = currentRunEndTime.minus(FREQUENCY_WINDOW); // Define the start of the current window to check
        Instant lastCheckedTime = lastCheckedFrequentConnectionWindow.get();

        Instant queryFromTime = lastCheckedTime.isAfter(windowStartTime) ? lastCheckedTime : windowStartTime;


        System.out.println("Alerting Service: Checking for frequent connections in window from " + queryFromTime + " to " + currentRunEndTime);

        structuredLogRepository.findFrequentConnectionsInWindow(
                        queryFromTime, // Start time for the query window
                        currentRunEndTime, // End time for the query window
                        MIN_CONNECTIONS_FOR_ALERT
                )
                .doOnSubscribe(subscription -> System.out.println("DEBUG Alerting service (Freq): Stream subscribed."))
                .doOnError(e -> System.err.println("CRITICAL ERROR in Frequent Connection Alerting stream: " + e.getMessage()))
                .doOnNext(dto -> System.out.println("DEBUG Alerting Service (Freq): Found frequent connection: " + dto.getSrcIP() + " -> " + dto.getDestIP() + " Count: " + dto.getConnectionCount()))
                .flatMap(dto -> {
                    String alertMessage = String.format("High frequency connections detected: %d connections from %s to %s within %d seconds. Latest log at %s.",
                            dto.getConnectionCount(), dto.getSrcIP(), dto.getDestIP(),
                            FREQUENCY_WINDOW.getSeconds(), dto.getLatestLogTimestamp());

                    Alert alert = new Alert(
                            dto.getLatestLogId(), // Link to the most recent log in this frequent connection group
                            "High Frequency Connection",
                            "CRITICAL",
                            alertMessage
                    );
                    return createAndSaveAlertIfNotDuplicate(alert, dto.getLatestLogId());
                })
                .doOnComplete(() -> {
                    lastCheckedFrequentConnectionWindow.set(currentRunEndTime); // Update to the end of the current window checked
                    System.out.println("Alerting Service: Finished checking frequent connections up to " + currentRunEndTime);
                })
                .then()
                .block();
    }

    // Helper method to save alert and prevent duplicates
    private Mono<Alert> createAndSaveAlertIfNotDuplicate(Alert alert, Long networkLogId) {
        return alertRepository.existsByNetworkLogId(networkLogId)
                .flatMap(exists -> {
                    if (!exists) {
                        System.out.println("DEBUG Alerting Service: Attempting to SAVE new alert for log ID: " + networkLogId);
                        return alertRepository.save(alert)
                                .doOnSuccess(savedAlert -> System.out.println("ALERT GENERATED: " + savedAlert.getAlertType() + " Severity: " + savedAlert.getSeverity() + " for log ID: " + savedAlert.getNetworkLogId()))
                                .doOnError(e -> System.err.println("ERROR: Could not save alert for log ID " + networkLogId + ": " + e.getMessage()));
                    } else {
                        System.out.println("DEBUG Alerting Service: Duplicate alert found for log ID: " + networkLogId + ". Skipping save.");
                        return Mono.empty();
                    }
                });
    }
}
