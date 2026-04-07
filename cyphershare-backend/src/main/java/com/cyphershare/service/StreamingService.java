package com.cyphershare.service;

import com.cyphershare.model.RelaySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

@Service
public class StreamingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingService.class);
    private static final int PIPE_SIZE = 1024 * 1024; // 1MB pipe buffer
    
    private final CompressionService compressionService;
    private final EncryptionService encryptionService;
    
    public StreamingService(CompressionService compressionService, EncryptionService encryptionService) {
        this.compressionService = compressionService;
        this.encryptionService = encryptionService;
    }
    
    /**
     * Creates streaming pipeline: Sender -> Compression -> Encryption -> Receiver
     * Returns the complete session with connected pipes
     */
    public RelaySession createStreamingPipeline(String code, String fileName, long fileSize) throws Exception {
        RelaySession session = new RelaySession(code);
        session.setFileName(fileName);
        session.setFileSize(fileSize);
        
        // Create piped streams
        PipedInputStream pipedInput = new PipedInputStream(PIPE_SIZE);
        PipedOutputStream pipedOutput = new PipedOutputStream(pipedInput);
        
        // Generate encryption key and IV
        SecretKey key = encryptionService.generateKey();
        byte[] iv = encryptionService.generateIV();
        
        // Wrap output stream with compression and encryption layers
        OutputStream compressedStream = compressionService.wrapWithCompression(pipedOutput);
        OutputStream encryptedStream = encryptionService.wrapWithEncryption(compressedStream, key, iv);
        
        session.setSenderStream(encryptedStream);
        session.setReceiverStream(pipedInput);
        
        logger.info("Streaming pipeline created for session: {} with file: {}", code, fileName);
        return session;
    }
    
    /**
     * Writes data chunk to sender stream with error handling
     */
    public void writeToSenderStream(RelaySession session, byte[] data, int offset, int length) throws IOException {
        try {
            session.getSenderStream().write(data, offset, length);
            session.getSenderStream().flush();
        } catch (IOException e) {
            logger.error("Error writing to sender stream for session: {}", session.getCode(), e);
            session.closeStreams();
            throw e;
        }
    }
    
    /**
     * Closes sender stream when upload is complete
     */
    public void closeSenderStream(RelaySession session) {
        try {
            if (session.getSenderStream() != null) {
                session.getSenderStream().close();
                logger.info("Sender stream closed for session: {}", session.getCode());
            }
        } catch (IOException e) {
            logger.warn("Error closing sender stream: {}", e.getMessage());
        }
    }
}