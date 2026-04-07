package com.cyphershare.controller;

import com.cyphershare.exception.SessionNotFoundException;
import com.cyphershare.exception.UnauthorizedException;
import com.cyphershare.model.RelaySession;
import com.cyphershare.service.RelaySessionManager;
import com.cyphershare.service.StreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/relay")
public class RelayController {
    
    private static final Logger logger = LoggerFactory.getLogger(RelayController.class);
    
    private final RelaySessionManager sessionManager;
    private final StreamingService streamingService;
    
    @Value("${cyphershare.license.mock-key}")
    private String mockLicenseKey;
    
    public RelayController(RelaySessionManager sessionManager, StreamingService streamingService) {
        this.sessionManager = sessionManager;
        this.streamingService = streamingService;
    }
    
    /**
     * Endpoint 1: Upload/Send
     * POST /api/relay/send
     * Verifies license, generates code, creates session, streams file data
     */
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> sendFile(
            @RequestHeader(value = "X-Business-License", required = false) String licenseKey,
            @RequestPart("file") Mono<FilePart> filePartMono
    ) {
        // Verify business license
        if (licenseKey == null || !licenseKey.equals(mockLicenseKey)) {
            throw new UnauthorizedException("Invalid or missing business license");
        }
        
        return filePartMono.flatMap(filePart -> {
            String fileName = filePart.filename();
            logger.info("Received upload request for file: {}", fileName);
            
            // Create session and generate code
            RelaySession session = sessionManager.createSession(fileName, 0);
            
            // Create streaming pipeline
            try {
                RelaySession pipelineSession = streamingService.createStreamingPipeline(
                    session.getCode(), fileName, 0
                );
                sessionManager.getSession(session.getCode()).setSenderStream(pipelineSession.getSenderStream());
                sessionManager.getSession(session.getCode()).setReceiverStream(pipelineSession.getReceiverStream());
                session.getSenderConnected().set(true);
            } catch (Exception e) {
                logger.error("Error creating streaming pipeline", e);
                return Mono.error(new RuntimeException("Failed to create streaming pipeline"));
            }
            
            // Process file data asynchronously
            AtomicLong totalBytes = new AtomicLong(0);
            
            Flux<ByteBuffer> dataFlux = filePart.content()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    try {
                        streamingService.writeToSenderStream(session, bytes, 0, bytes.length);
                        totalBytes.addAndGet(bytes.length);
                    } catch (IOException e) {
                        logger.error("Error writing to stream", e);
                    }
                    return ByteBuffer.wrap(bytes);
                })
                .doOnComplete(() -> {
                    streamingService.closeSenderStream(session);
                    session.setFileSize(totalBytes.get());
                    session.getTransferComplete().complete(null);
                    logger.info("File upload complete for session: {} ({}MB)", 
                        session.getCode(), totalBytes.get() / 1024 / 1024);
                })
                .doOnError(error -> {
                    logger.error("Error during file upload: {}", error.getMessage());
                    sessionManager.removeSession(session.getCode());
                });
            
            // Subscribe to start processing
            dataFlux.subscribeOn(Schedulers.boundedElastic()).subscribe();
            
            // Return code immediately to sender
            Map<String, Object> response = new HashMap<>();
            response.put("code", session.getCode());
            response.put("fileName", fileName);
            response.put("message", "File is being processed. Share this code with the receiver.");
            response.put("expiresIn", "10 minutes");
            
            return Mono.just(ResponseEntity.ok(response));
        });
    }
    
    /**
     * Endpoint 2: Download/Receive
     * GET /api/relay/receive/{code}
     * Retrieves session and streams .udef file to receiver
     */
    @GetMapping("/receive/{code}")
    public Mono<ResponseEntity<InputStreamResource>> receiveFile(@PathVariable String code) {
        RelaySession session = sessionManager.getSession(code);
        
        if (session == null) {
            throw new SessionNotFoundException("Invalid or expired code: " + code);
        }
        
        if (!session.getSenderConnected().get()) {
            return Mono.error(new RuntimeException("Sender not yet connected. Please wait."));
        }
        
        session.getReceiverConnected().set(true);
        logger.info("Receiver connected for session: {}", code);
        
        // Stream .udef file from receiver input stream
        InputStreamResource resource = new InputStreamResource(session.getReceiverStream());
        
        String fileName = session.getFileName() + ".udef";
        
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
        headers.add("X-Original-Filename", session.getFileName());
        
        // Clean up session after transfer completes
        session.getTransferComplete().thenRun(() -> {
            logger.info("Transfer complete for session: {}", code);
            sessionManager.removeSession(code);
        });
        
        return Mono.just(ResponseEntity.ok()
            .headers(headers)
            .body(resource));
    }
    
    /**
     * Check session status (for frontend real-time updates)
     */
    @GetMapping("/status/{code}")
    public Mono<ResponseEntity<Map<String, Object>>> getStatus(@PathVariable String code) {
        RelaySession session = sessionManager.getSession(code);
        
        if (session == null) {
            throw new SessionNotFoundException("Invalid or expired code: " + code);
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("code", session.getCode());
        status.put("fileName", session.getFileName());
        status.put("senderConnected", session.getSenderConnected().get());
        status.put("receiverConnected", session.getReceiverConnected().get());
        status.put("createdAt", session.getCreatedAt().toString());
        
        return Mono.just(ResponseEntity.ok(status));
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("activeSessions", sessionManager.getActiveSessionCount());
        return Mono.just(ResponseEntity.ok(health));
    }
}
