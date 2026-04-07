package com.cyphershare.service;

import com.cyphershare.model.RelaySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RelaySessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RelaySessionManager.class);
    private static final String CODE_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();
    
    private final Map<String, RelaySession> sessions = new ConcurrentHashMap<>();
    
    @Value("${cyphershare.session.timeout-minutes:10}")
    private int sessionTimeoutMinutes;
    
    /**
     * Creates a new relay session with a unique 6-digit code
     */
    public RelaySession createSession(String fileName, long fileSize) {
        String code = generateUniqueCode();
        RelaySession session = new RelaySession(code);
        session.setFileName(fileName);
        session.setFileSize(fileSize);
        sessions.put(code, session);
        logger.info("Created session: {} for file: {} ({}MB)", code, fileName, fileSize / 1024 / 1024);
        return session;
    }
    
    /**
     * Retrieves a session by code
     */
    public RelaySession getSession(String code) {
        return sessions.get(code.toUpperCase());
    }
    
    /**
     * Removes a session and closes all streams
     */
    public void removeSession(String code) {
        RelaySession session = sessions.remove(code.toUpperCase());
        if (session != null) {
            session.closeStreams();
            session.getTransferComplete().complete(null);
            logger.info("Removed session: {}", code);
        }
    }
    
    /**
     * Generates a unique 6-character alphanumeric code
     */
    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (sessions.containsKey(code));
        return code;
    }
    
    /**
     * Scheduled task to clean up expired sessions (runs every 5 minutes)
     */
    @Scheduled(fixedDelayString = "${cyphershare.session.cleanup-interval-minutes:5}", initialDelay = 60000)
    public void cleanupExpiredSessions() {
        Instant now = Instant.now();
        int removedCount = 0;
        
        for (Map.Entry<String, RelaySession> entry : sessions.entrySet()) {
            RelaySession session = entry.getValue();
            Duration age = Duration.between(session.getCreatedAt(), now);
            
            if (age.toMinutes() >= sessionTimeoutMinutes) {
                removeSession(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("Cleaned up {} expired sessions", removedCount);
        }
    }
    
    /**
     * Gets current number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}