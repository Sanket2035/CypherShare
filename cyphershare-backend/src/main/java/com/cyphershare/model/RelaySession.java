package com.cyphershare.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.OutputStream;
import java.io.PipedInputStream;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelaySession {
    private String code;
    private OutputStream senderStream;
    private PipedInputStream receiverStream;
    private Instant createdAt;
    private String fileName;
    private long fileSize;
    private AtomicBoolean senderConnected;
    private AtomicBoolean receiverConnected;
    private CompletableFuture<Void> transferComplete;
    
    public RelaySession(String code) {
        this.code = code;
        this.createdAt = Instant.now();
        this.senderConnected = new AtomicBoolean(false);
        this.receiverConnected = new AtomicBoolean(false);
        this.transferComplete = new CompletableFuture<>();
    }
    
    public void closeStreams() {
        try {
            if (senderStream != null) {
                senderStream.close();
            }
        } catch (Exception e) {
            // Ignore
        }
        try {
            if (receiverStream != null) {
                receiverStream.close();
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}