package org.example.kafkaconsumer.service;

import org.example.kafkaconsumer.postgres.entity.Alert;
import org.example.kafkaconsumer.postgres.entity.StructuredNetworkLog;
import org.example.kafkaconsumer.postgres.repository.AlertRepository;
import org.example.kafkaconsumer.postgres.repository.StructuredLogRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID; // Import UUID if StructuredNetworkLog ID is UUID

@Service
public class AlertingService {

    private final StructuredLogRepository structuredLogRepository;
    private final AlertRepository alertRepository;

    private static final List<Long> SUSPICIOUS_PORTS = Arrays.asList(23L, 3389L, 22L, 445L);
    private static final List<String> BLACKLISTED_IPS = Arrays.asList(
            "1.2.3.4",
            "192.0.2.1",
            "10.0.0.1"
    );

    public AlertingService(StructuredLogRepository structuredLogRepository, AlertRepository alertRepository) {
        this.structuredLogRepository = structuredLogRepository;
        this.alertRepository = alertRepository;
    }

    public Mono<Void> analyzeAndAlert(Long logId) { // Adjust Long if your logId is UUID/String
        System.out.println("Alerting Service: Received trigger for Log ID: " + logId + ". Starting analysis.");

        return structuredLogRepository.findById(logId)
                .doOnSuccess(log -> {
                    if (log != null) {
                        System.out.println("Alerting Service: Fetched log " + log.getLogId() + " from PostgreSQL for analysis.");
                        performThreatAnalysis(log);
                    } else {
                        System.out.println("Alerting Service: Log with ID " + logId + " not found in PostgreSQL. Cannot perform analysis.");
                    }
                })
                .doOnError(e -> System.err.println("Alerting Service: Error fetching log " + logId + " for analysis: " + e.getMessage()))
                .then();
    }

    private void performThreatAnalysis(StructuredNetworkLog log) {
        Mono<Void> portAnalysis = checkSuspiciousPortActivity(log);
        Mono<Void> ipAnalysis = checkBlacklistedIpActivity(log);

        Mono.when(portAnalysis, ipAnalysis)
                .doOnSuccess((aVoid) -> System.out.println("Alerting Service: Completed all per-log analysis for Log ID: " + log.getLogId()))
                .doOnError(e -> System.err.println("Alerting Service: Error during combined analysis for Log ID " + log.getLogId() + ": " + e.getMessage()))
                .subscribe();
    }

    private Mono<Void> checkSuspiciousPortActivity(StructuredNetworkLog log) {
        if (SUSPICIOUS_PORTS.contains(log.getPort())) {
            System.out.println("DEBUG Alerting Service (Ports): Detected suspicious port: " + log.getPort() + " for Log ID: " + log.getLogId());
            String alertMessage = "Detected connection to suspicious port: " + log.getPort() +
                    " from " + log.getSrcIP() + " to " + log.getDestIP() +
                    ". Protocol: " + log.getProtocol();

            // Use the new Alert constructor: public Alert(Long networkLogId, String alertType, String severity, String alertMessage)
            Alert alert = new Alert(
                    log.getId(),
                    "Suspicious Port Activity",
                    "HIGH",
                    alertMessage
            );
            return createAndSaveAlertIfNotDuplicate(alert, log.getId()).then();
        }
        return Mono.empty();
    }

    private Mono<Void> checkBlacklistedIpActivity(StructuredNetworkLog log) {
        boolean isBlacklistedSrc = BLACKLISTED_IPS.contains(log.getSrcIP());
        boolean isBlacklistedDest = BLACKLISTED_IPS.contains(log.getDestIP());

        if (isBlacklistedSrc || isBlacklistedDest) {
            System.out.println("DEBUG Alerting Service (Blacklist): Detected blacklisted IP for Log ID: " + log.getLogId());
            String detectedIp = isBlacklistedSrc ? log.getSrcIP() : log.getDestIP();
            String alertMessage = "Detected connection involving blacklisted IP: " + detectedIp +
                    " (Source: " + log.getSrcIP() + ", Dest: " + log.getDestIP() + ")";

            // Use the new Alert constructor
            Alert alert = new Alert(
                    log.getId(), // Pass the StructuredNetworkLog's ID as networkLogId
                    "Blacklisted IP Activity",
                    "CRITICAL",
                    alertMessage
            );
            return createAndSaveAlertIfNotDuplicate(alert, log.getId()).then();
        }
        return Mono.empty();
    }

    private Mono<Alert> createAndSaveAlertIfNotDuplicate(Alert alert, Long networkLogId) { // Adjust Long if networkLogId is UUID
        return alertRepository.existsByNetworkLogId(networkLogId)
                .flatMap(exists -> {
                    if (!exists) {
                        System.out.println("DEBUG Alerting Service: Attempting to SAVE new alert: " + alert.getAlertType() + " for log ID: " + networkLogId);
                        return alertRepository.save(alert)
                                .doOnSuccess(savedAlert -> System.out.println("ALERT GENERATED: " + savedAlert.getAlertType() + " Severity: " + savedAlert.getSeverity() + " for log ID: " + savedAlert.getNetworkLogId() + " (Alert UUID: " + savedAlert.getAlertId() + ")"));
                    } else {
                        System.out.println("DEBUG Alerting Service: Duplicate alert found for log ID: " + networkLogId + ". Skipping save.");
                        return Mono.empty();
                    }
                })
                .doOnError(e -> System.err.println("ERROR: Could not save alert for log ID " + networkLogId + ": " + e.getMessage()));
    }
}