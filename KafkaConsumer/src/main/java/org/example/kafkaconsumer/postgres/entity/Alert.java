package org.example.kafkaconsumer.postgres.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("alerts")
public class Alert {
    @Id
    private long id;  // PostgreSQL auto-generated SERIAL primary key

    @Column("alert_id")
    private String alertId; // UUID that was assigned by pipeline

    @Column("network_log_id")
    private Long networkLogId; // Foreign key to network_logs table

    @Column("alert_type")
    private String alertType; // "Blacklisted IP, Suspicious port, etc"

    private String severity; // "CRITICAL", "HIGH", "MEDIUM", "LOW"
    private String status;   // "NEW", "ACKNOWLEDGED", "CLOSED"

    @Column("alert_message")
    private String alertMessage;

    @Column("raised_at")
    private Instant raisedAt;

    @Column("acknowledged_by")
    private String acknowledgedBy;
    @Column("acknowledged_at")
    private Instant acknowledgedAt;

    public Alert(Long networkLogId, String alertType, String severity, String alertMessage) {
        this.alertId = UUID.randomUUID().toString();
        this.networkLogId = networkLogId;
        this.alertType = alertType;
        this.severity = severity;
        this.alertMessage = alertMessage;
        this.status = "NEW";
        this.raisedAt = Instant.now();
    }
}
