# Unified runtime across both architectures: same base image and JVM version
# as the microservices, so any measured difference is architectural, not runtime.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/monolith-1.0.0.jar app.jar

EXPOSE 8080

# Heap settings come from docker-compose via JAVA_OPTS (resource envelope).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
