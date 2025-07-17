// object to hold the result of the sql query for this alert
// DTO == Data Transfer Object
package org.example.kafkaconsumer.postgres.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrequentConnectionAlertDTO {
    private String srcIP;
    private String destIP;
    private Long connectionCount;
    private Instant latestLogTimestamp;
    private Long latestLogId;
}
