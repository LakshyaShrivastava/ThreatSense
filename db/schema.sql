-- Table: network_logs
CREATE TABLE network_logs (
    id BIGSERIAL PRIMARY KEY, -- Auto-incrementing primary key (BIGSERIAL for potentially many logs)
    log_id VARCHAR(36) UNIQUE NOT NULL, -- Your application's UUID for the log (from NetworkLog.java)
    timestamp TIMESTAMPTZ NOT NULL, -- When the log event occurred (with timezone)
    src_ip VARCHAR(45) NOT NULL, -- Source IP address (supports IPv4 and IPv6)
    dest_ip VARCHAR(45) NOT NULL, -- Destination IP address
    port INTEGER NOT NULL, -- The port (from your NetworkLog POJO)
    protocol VARCHAR(10) NOT NULL, -- e.g., TCP, UDP, ICMP, HTTP
    bytes BIGINT NOT NULL, -- Number of bytes transferred, use BIGINT for large values
    message TEXT, -- The descriptive log message
    raw_log TEXT -- The original raw log string
);

-- Indexes for network_logs
CREATE INDEX idx_network_logs_timestamp ON network_logs (timestamp DESC);
CREATE INDEX idx_network_logs_src_ip ON network_logs (src_ip);
CREATE INDEX idx_network_logs_dest_ip ON network_logs (dest_ip);
CREATE INDEX idx_network_logs_protocol ON network_logs (protocol);


-- Table: alerts
CREATE TABLE alerts (
    id SERIAL PRIMARY KEY, -- Auto-incrementing primary key for each alert
    alert_id VARCHAR(36) UNIQUE NOT NULL, -- A unique ID for the alert itself (can be new UUID)
    network_log_id BIGINT NOT NULL, -- Foreign key referencing the 'network_logs.id' that triggered this alert
    alert_type VARCHAR(50) NOT NULL, -- e.g., 'Port Scan', 'Failed Login', 'High Traffic'
    severity VARCHAR(20) NOT NULL, -- e.g., 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFORMATIONAL'
    status VARCHAR(20) DEFAULT 'NEW', -- e.g., 'NEW', 'IN_PROGRESS', 'ACKNOWLEDGED', 'CLOSED'
    alert_message TEXT, -- A specific message for this alert
    raised_at TIMESTAMPTZ NOT NULL, -- When this alert was generated/raised
    acknowledged_by VARCHAR(100), -- User who acknowledged the alert
    acknowledged_at TIMESTAMPTZ, -- When the alert was acknowledged

    CONSTRAINT fk_network_log
        FOREIGN KEY (network_log_id)
        REFERENCES network_logs (id) ON DELETE CASCADE
);

-- Indexes for alerts
CREATE INDEX idx_alerts_raised_at ON alerts (raised_at DESC);
CREATE INDEX idx_alerts_severity ON alerts (severity);
CREATE INDEX idx_alerts_status ON alerts (status);
