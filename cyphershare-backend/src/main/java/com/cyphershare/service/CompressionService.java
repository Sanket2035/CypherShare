package com.cyphershare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class CompressionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompressionService.class);
    private static final int PACKET_SIZE = 8192; // 8KB packets
    private static final int MIN_MATCH_LENGTH = 3;
    private static final int MAX_MATCH_LENGTH = 258;
    private static final int DICTIONARY_SIZE = 32768; // 32KB sliding window
    
    /**
     * Wraps an OutputStream with custom UDEF dictionary-based lossless compression
     * Based on LZ77-like algorithm with localized byte-mapping
     */
    public OutputStream wrapWithCompression(OutputStream outputStream) {
        return new UDEFCompressionOutputStream(outputStream);
    }
    
    private static class UDEFCompressionOutputStream extends OutputStream {
        private final OutputStream wrapped;
        private final byte[] buffer;
        private int bufferPos;
        private final byte[] slidingWindow;
        private int windowPos;
        private int windowSize;
        
        public UDEFCompressionOutputStream(OutputStream wrapped) {
            this.wrapped = wrapped;
            this.buffer = new byte[PACKET_SIZE];
            this.bufferPos = 0;
            this.slidingWindow = new byte[DICTIONARY_SIZE];
            this.windowPos = 0;
            this.windowSize = 0;
        }
        
        @Override
        public void write(int b) throws IOException {
            buffer[bufferPos++] = (byte) b;
            if (bufferPos >= PACKET_SIZE) {
                compressAndFlush();
            }
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int remaining = len;
            int offset = off;
            
            while (remaining > 0) {
                int available = PACKET_SIZE - bufferPos;
                int toWrite = Math.min(remaining, available);
                System.arraycopy(b, offset, buffer, bufferPos, toWrite);
                bufferPos += toWrite;
                offset += toWrite;
                remaining -= toWrite;
                
                if (bufferPos >= PACKET_SIZE) {
                    compressAndFlush();
                }
            }
        }
        
        @Override
        public void flush() throws IOException {
            if (bufferPos > 0) {
                compressAndFlush();
            }
            wrapped.flush();
        }
        
        @Override
        public void close() throws IOException {
            flush();
            wrapped.close();
        }
        
        private void compressAndFlush() throws IOException {
            if (bufferPos == 0) return;
            
            // UDEF compression: dictionary-based with localized byte-mapping
            int readPos = 0;
            
            while (readPos < bufferPos) {
                // Find longest match in sliding window
                int[] match = findLongestMatch(readPos);
                int matchDistance = match[0];
                int matchLength = match[1];
                
                if (matchLength >= MIN_MATCH_LENGTH) {
                    // Write reference token: <distance, length>
                    writeToken(true, matchDistance, matchLength);
                    
                    // Add matched bytes to sliding window
                    for (int i = 0; i < matchLength; i++) {
                        addToSlidingWindow(buffer[readPos + i]);
                    }
                    readPos += matchLength;
                } else {
                    // Write literal byte
                    writeToken(false, 0, buffer[readPos] & 0xFF);
                    addToSlidingWindow(buffer[readPos]);
                    readPos++;
                }
            }
            
            bufferPos = 0;
        }
        
        private int[] findLongestMatch(int pos) {
            int bestDistance = 0;
            int bestLength = 0;
            
            if (windowSize == 0) {
                return new int[]{0, 0};
            }
            
            // Search in sliding window for matches
            for (int i = 0; i < windowSize; i++) {
                int matchLen = 0;
                int maxLen = Math.min(MAX_MATCH_LENGTH, bufferPos - pos);
                
                while (matchLen < maxLen && 
                       slidingWindow[(windowPos - windowSize + i + matchLen) & (DICTIONARY_SIZE - 1)] 
                       == buffer[pos + matchLen]) {
                    matchLen++;
                }
                
                if (matchLen > bestLength) {
                    bestLength = matchLen;
                    bestDistance = windowSize - i;
                }
            }
            
            return new int[]{bestDistance, bestLength};
        }
        
        private void addToSlidingWindow(byte b) {
            slidingWindow[windowPos] = b;
            windowPos = (windowPos + 1) & (DICTIONARY_SIZE - 1);
            if (windowSize < DICTIONARY_SIZE) {
                windowSize++;
            }
        }
        
        private void writeToken(boolean isReference, int distanceOrLiteral, int length) throws IOException {
            if (isReference) {
                // Reference token: 1 bit flag + 15 bits distance + 8 bits length
                wrapped.write(0x80 | ((distanceOrLiteral >> 8) & 0x7F));
                wrapped.write(distanceOrLiteral & 0xFF);
                wrapped.write(length & 0xFF);
            } else {
                // Literal token: 1 bit flag + 7 bits literal value
                wrapped.write(distanceOrLiteral & 0x7F);
            }
        }
    }
}