FROM openjdk:17-slim

WORKDIR /app

# Copy the JAR file
COPY target/cyphershare-backend-1.0.0.jar app.jar

# Expose port
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8001/api/relay/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Xmx2g", "-Xms512m", "-jar", "app.jar"]