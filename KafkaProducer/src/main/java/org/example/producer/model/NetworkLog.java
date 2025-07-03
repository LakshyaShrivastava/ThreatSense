package org.example.producer.model;

import java.time.Instant; // For timestamp

public class NetworkLog {
    private String srcIP;
    private String destIP;
    private int port;
    private String protocol;
    private long bytes;
    private Instant timestamp;


    public NetworkLog() {} // default constructor required by Jackson for deserialization

    public NetworkLog(String srcIP, String destIP, int port, String protocol, long bytes, Instant timestamp) {
        this.srcIP = srcIP;
        this.destIP = destIP;
        this.port = port;
        this.protocol = protocol;
        this.bytes = bytes;
        this.timestamp = timestamp;
    }

    // --- Getters (REQUIRED by Jackson for serialization) ---
    public String getSrcIP() {return srcIP;}
    public String getDestIP() {return destIP;}
    public int getPort() {return port;}
    public String getProtocol() {return protocol;}
    public long  getBytes() {return bytes;}
    public Instant getTimestamp() {return timestamp;}

    // --- Setters (REQUIRED by Jackson for deserialization)
    public void setSrcIP(String srcIP) {this.srcIP = srcIP;}
    public void setDestIP(String destIP) {this.destIP = destIP;}
    public void setPort(int port) {this.port = port;}
    public void setProtocol(String protocol) {this.protocol = protocol;}
    public void setBytes(long bytes) {this.bytes = bytes;}
    public void setTimestamp(Instant timestamp) {this.timestamp = timestamp;}

    @Override
    public String toString() {
        return "NetworkLog{" +
                "srcIP='" + srcIP + '\'' +
                ", destIP='" + destIP + '\'' +
                ", port=" + port +
                ", protocol='" + protocol + '\'' +
                ", bytes=" + bytes +
                ", timestamp=" +timestamp +
                '}';
    }
}
